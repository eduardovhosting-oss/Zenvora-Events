package com.zenvora.events.data;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class PlayerStatsStore {
    private final JavaPlugin plugin;
    private final File file;
    private final YamlConfiguration data;

    public PlayerStatsStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stats.yml");
        if (!file.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("No se pudo crear stats.yml: " + e.getMessage());
            }
        }
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public void addPoints(UUID uuid, String playerName, int points) {
        if (points <= 0) return;
        String path = "players." + uuid;
        data.set(path + ".name", playerName);
        data.set(path + ".points", getPoints(uuid) + points);
        data.set(path + ".events", getEvents(uuid) + 1);
    }

    public int getPoints(UUID uuid) {
        return data.getInt("players." + uuid + ".points", 0);
    }

    public int getEvents(UUID uuid) {
        return data.getInt("players." + uuid + ".events", 0);
    }

    public List<Map.Entry<UUID, Integer>> top(int limit) {
        List<Map.Entry<UUID, Integer>> result = new ArrayList<>();
        if (!data.isConfigurationSection("players")) return result;

        for (String key : Objects.requireNonNull(data.getConfigurationSection("players")).getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                result.add(Map.entry(uuid, data.getInt("players." + key + ".points", 0)));
            } catch (IllegalArgumentException ignored) {}
        }

        result.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        return result.subList(0, Math.min(limit, result.size()));
    }

    public String getStoredName(UUID uuid) {
        return data.getString("players." + uuid + ".name", uuid.toString());
    }

    public void save() {
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("No se pudo guardar stats.yml: " + e.getMessage());
        }
    }
}
