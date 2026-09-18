package com.zenvora.events.event;

import com.zenvora.events.data.PlayerStatsStore;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.*;

public final class EventManager {
    private final JavaPlugin plugin;
    private final PlayerStatsStore statsStore;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Map<String, EventDefinition> definitions = new LinkedHashMap<>();
    private final Map<String, Long> cooldowns = new HashMap<>();
    private ActiveEvent active;
    private BukkitTask ticker;

    public EventManager(JavaPlugin plugin, PlayerStatsStore statsStore) {
        this.plugin = plugin;
        this.statsStore = statsStore;
        reload();
    }

    public void reload() {
        definitions.clear();
        File file = new File(plugin.getDataFolder(), "events.yml");
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = cfg.getConfigurationSection("events");
        if (section == null) return;

        for (String id : section.getKeys(false)) {
            String p = "events." + id;
            EventType type;
            try {
                type = EventType.valueOf(cfg.getString(p + ".type", "MINING").toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Tipo inválido en evento " + id);
                continue;
            }

            Map<String, Integer> valuable = new HashMap<>();
            ConfigurationSection vp = cfg.getConfigurationSection(p + ".points.valuable");
            if (vp != null) {
                for (String key : vp.getKeys(false)) valuable.put(key.toUpperCase(), vp.getInt(key));
            }

            Map<String, List<String>> rewards = new HashMap<>();
            rewards.put("participation", cfg.getStringList(p + ".rewards.participation"));
            ConfigurationSection top = cfg.getConfigurationSection(p + ".rewards.top");
            if (top != null) {
                for (String pos : top.getKeys(false)) rewards.put("top." + pos, cfg.getStringList(p + ".rewards.top." + pos));
            }

            long duration = Math.min(
                    cfg.getLong(p + ".duration-seconds", 600),
                    plugin.getConfig().getLong("settings.max-event-duration-seconds", 3600)
            );

            definitions.put(id.toLowerCase(), new EventDefinition(
                    id.toLowerCase(),
                    cfg.getString(p + ".display-name", id),
                    cfg.getString(p + ".description", ""),
                    cfg.getBoolean(p + ".enabled", true),
                    duration,
                    cfg.getLong(p + ".cooldown-seconds", 0),
                    type,
                    cfg.getInt(p + ".points.default", 1),
                    valuable,
                    rewards
            ));
        }
    }

    public Collection<EventDefinition> definitions() {
        return Collections.unmodifiableCollection(definitions.values());
    }

    public EventDefinition get(String id) {
        return definitions.get(id.toLowerCase());
    }

    public ActiveEvent active() {
        return active;
    }

    public boolean isActive() {
        return active != null;
    }

    public String display(String raw) {
        return raw == null ? "" : raw.replace("<", "<").replace(">", ">");
    }

    public boolean start(String id) {
        EventDefinition def = get(id);
        if (def == null || !def.enabled() || active != null || isCooldown(def)) return false;

        active = new ActiveEvent(def);
        broadcast(def.displayName() + " <gray>ha comenzado!</gray>");
        titleAll(def.displayName(), "<gray>¡Consigue puntos y gana recompensas!</gray>");
        startBossbar();

        if (ticker != null) ticker.cancel();
        ticker = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (active == null) return;
            if (active.expired()) stopActiveEvent(true);
            else updateBossbar();
        }, 20L, 20L);

        return true;
    }

    public void stopActiveEvent(boolean reward) {
        if (active == null) return;

        ActiveEvent finished = active;
        active = null;
        cooldowns.put(finished.definition().id(), System.currentTimeMillis() + finished.definition().cooldownSeconds() * 1000L);

        if (ticker != null) {
            ticker.cancel();
            ticker = null;
        }

        if (reward) {
            List<Map.Entry<UUID, Integer>> ranking = new ArrayList<>(finished.scores().entrySet());
            ranking.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

            int position = 1;
            for (Map.Entry<UUID, Integer> entry : ranking) {
                reward(entry.getKey(), finished.definition(), position);
                position++;
                if (position > 3) break;
            }

            for (UUID uuid : finished.scores().keySet()) {
                rewardParticipation(uuid, finished.definition());
                statsStore.addPoints(uuid,
                        Optional.ofNullable(Bukkit.getOfflinePlayer(uuid).getName()).orElse(uuid.toString()),
                        finished.score(uuid));
            }
            statsStore.save();
        }

        clearBossbar();
        broadcast(finished.definition().displayName() + " <gray>ha terminado.</gray>");
        titleAll(finished.definition().displayName(), "<yellow>¡Evento finalizado!</yellow>");
    }

    public void addPoints(Player player, int points) {
        if (active != null) active.addScore(player.getUniqueId(), points);
    }

    public int pointsFor(String key) {
        return active == null ? 0 : active.definition().pointsFor(key);
    }

    public boolean isType(EventType type) {
        return active != null && active.definition().type() == type;
    }

    private boolean isCooldown(EventDefinition def) {
        return cooldowns.getOrDefault(def.id(), 0L) > System.currentTimeMillis();
    }

    public long cooldownRemaining(String id) {
        return Math.max(0, (cooldowns.getOrDefault(id.toLowerCase(), 0L) - System.currentTimeMillis()) / 1000);
    }

    private void reward(UUID uuid, EventDefinition def, int position) {
        Player player = Bukkit.getPlayer(uuid);
        String name = player != null ? player.getName() : Optional.ofNullable(Bukkit.getOfflinePlayer(uuid).getName()).orElse(uuid.toString());
        List<String> commands = def.rewards().getOrDefault("top." + position, List.of());
        for (String command : commands) executeReward(command, name, def.id(), position, activeScoreFallback(uuid));
    }

    private void rewardParticipation(UUID uuid, EventDefinition def) {
        Player player = Bukkit.getPlayer(uuid);
        String name = player != null ? player.getName() : Optional.ofNullable(Bukkit.getOfflinePlayer(uuid).getName()).orElse(uuid.toString());
        for (String command : def.rewards().getOrDefault("participation", List.of()))
            executeReward(command, name, def.id(), 0, 0);
    }

    private int activeScoreFallback(UUID uuid) {
        return 0;
    }

    private void executeReward(String command, String player, String event, int position, int points) {
        command = command.replace("%player%", player)
                .replace("%event%", event)
                .replace("%position%", String.valueOf(position))
                .replace("%points%", String.valueOf(points));
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    private void broadcast(String message) {
        Component component = mm.deserialize(plugin.getConfig().getString("display.prefix", "") + message);
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(component));
    }

    private void titleAll(String title, String subtitle) {
        if (!plugin.getConfig().getBoolean("display.title.enabled", true)) return;
        Component t = mm.deserialize(title);
        Component s = mm.deserialize(subtitle);
        Bukkit.getOnlinePlayers().forEach(p -> p.showTitle(net.kyori.adventure.title.Title.title(t, s)));
    }

    private BossBar bossbar;

    private void startBossbar() {
        if (!plugin.getConfig().getBoolean("display.bossbar.enabled", true) || active == null) return;
        bossbar = BossBar.bossBar(mm.deserialize(active.definition().displayName()), 1f,
                BossBar.Color.PURPLE, BossBar.Overlay.PROGRESS);
        Bukkit.getOnlinePlayers().forEach(p -> p.showBossBar(bossbar));
        updateBossbar();
    }

    private void updateBossbar() {
        if (bossbar == null || active == null) return;
        long remaining = Math.max(0, active.endAt() - System.currentTimeMillis());
        float progress = Math.max(0f, Math.min(1f, remaining / (active.definition().durationSeconds() * 1000f)));
        bossbar.progress(progress);
        bossbar.name(mm.deserialize(active.definition().displayName() + " <gray>• <white>" + (remaining / 1000) + "s</white></gray>"));
    }

    private void clearBossbar() {
        if (bossbar != null) {
            Bukkit.getOnlinePlayers().forEach(p -> p.hideBossBar(bossbar));
            bossbar = null;
        }
    }

    public void showBossbar(Player player) {
        if (bossbar != null) player.showBossBar(bossbar);
    }

    public void stopTicker() {
        if (ticker != null) ticker.cancel();
    }

    public void startTickerIfNeeded() {
        if (active == null || ticker != null) return;
        ticker = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (active != null && active.expired()) stopActiveEvent(true);
            else updateBossbar();
        }, 20L, 20L);
    }
}
