package com.zenvora.events.command;

import com.zenvora.events.ZenvoraEventsPlugin;
import com.zenvora.events.data.PlayerStatsStore;
import com.zenvora.events.event.*;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public final class EventsCommand implements CommandExecutor, TabCompleter {
    private final ZenvoraEventsPlugin plugin;
    private final EventManager manager;
    private final PlayerStatsStore stats;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public EventsCommand(ZenvoraEventsPlugin plugin, EventManager manager, PlayerStatsStore stats) {
        this.plugin = plugin;
        this.manager = manager;
        this.stats = stats;
    }

    private void msg(CommandSender sender, String text) {
        sender.sendMessage(mm.deserialize(text));
    }

    private boolean perm(CommandSender s, String p) {
        if (s.hasPermission("zenvoraevents.admin")) return true;
        if (s.hasPermission(p)) return true;
        msg(s, plugin.getConfig().getString("messages.no-permission", "<red>Sin permiso.</red>"));
        return false;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "help" -> {
                for (String line : YamlHelp.lines()) msg(sender, line);
            }
            case "list" -> {
                msg(sender, "<gradient:#8A2BE2:#FF4FD8><bold>✦ ZENVORA EVENTS ✦</bold></gradient>");
                for (EventDefinition e : manager.definitions()) {
                    msg(sender, "<gray>• <white>" + e.id() + "</white> <dark_gray>—</dark_gray> " +
                            e.displayName() + " <dark_gray>(" + e.type() + ")</dark_gray></gray>");
                }
            }
            case "info" -> {
                if (args.length < 2) { msg(sender, "<red>Uso: /zevents info <id></red>"); return true; }
                EventDefinition e = manager.get(args[1]);
                if (e == null) { msg(sender, "<red>Evento no encontrado.</red>"); return true; }
                msg(sender, "<gradient:#8A2BE2:#FF4FD8><bold>EVENTO</bold></gradient>");
                msg(sender, "<gray>" + e.displayName() + "</gray>");
                msg(sender, "<gray>Tipo: <white>" + e.type() + "</white> | Duración: <white>" + e.durationSeconds() + "s</white></gray>");
                msg(sender, "<gray>" + e.description() + "</gray>");
            }
            case "start" -> {
                if (!perm(sender, "zenvoraevents.start")) return true;
                if (args.length < 2) { msg(sender, "<red>Uso: /zevents start <id></red>"); return true; }
                EventDefinition e = manager.get(args[1]);
                if (e == null) { msg(sender, "<red>Evento no encontrado.</red>"); return true; }
                if (manager.isActive()) { msg(sender, "<red>Ya hay un evento activo.</red>"); return true; }
                if (manager.cooldownRemaining(e.id()) > 0) {
                    msg(sender, "<red>Cooldown restante: " + manager.cooldownRemaining(e.id()) + "s</red>");
                    return true;
                }
                if (!manager.start(e.id())) msg(sender, "<red>No se pudo iniciar el evento.</red>");
            }
            case "stop" -> {
                if (!perm(sender, "zenvoraevents.stop")) return true;
                if (!manager.isActive()) { msg(sender, "<red>No hay evento activo.</red>"); return true; }
                manager.stopActiveEvent(true);
            }
            case "top" -> {
                if (!perm(sender, "zenvoraevents.top")) return true;
                msg(sender, "<gradient:#FFD700:#FF8C00><bold>🏆 TOP ZENVORA EVENTS</bold></gradient>");
                List<Map.Entry<UUID,Integer>> top = stats.top(10);
                if (top.isEmpty()) msg(sender, "<gray>Aún no hay participantes.</gray>");
                int i = 1;
                for (Map.Entry<UUID,Integer> entry : top) {
                    msg(sender, "<white>#" + i + "</white> <gray>" + stats.getStoredName(entry.getKey()) +
                            "</gray> <dark_gray>—</dark_gray> <yellow>" + entry.getValue() + " puntos</yellow>");
                    i++;
                }
            }
            case "stats" -> {
                if (!perm(sender, "zenvoraevents.stats")) return true;
                UUID uuid;
                String name;
                if (args.length >= 2) {
                    Player p = Bukkit.getPlayerExact(args[1]);
                    if (p == null) {
                        msg(sender, "<red>Ese jugador debe estar conectado.</red>");
                        return true;
                    }
                    uuid = p.getUniqueId(); name = p.getName();
                } else if (sender instanceof Player p) {
                    uuid = p.getUniqueId(); name = p.getName();
                } else {
                    msg(sender, "<red>Desde consola usa /zevents stats <jugador>.</red>");
                    return true;
                }
                msg(sender, "<gradient:#8A2BE2:#FF4FD8><bold>ESTADÍSTICAS</bold></gradient>");
                msg(sender, "<gray>" + name + " <dark_gray>—</dark_gray> <white>" +
                        stats.getPoints(uuid) + " puntos</white> <dark_gray>|</dark_gray> <white>" +
                        stats.getEvents(uuid) + " acciones</white></gray>");
            }
            case "reload" -> {
                if (!perm(sender, "zenvoraevents.admin")) return true;
                plugin.reloadPlugin();
                msg(sender, plugin.getConfig().getString("messages.reloaded", "<green>Recargado.</green>"));
            }
            default -> {
                for (String line : YamlHelp.lines()) msg(sender, line);
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return partial(args[0], List.of("help", "list", "info", "start", "stop", "top", "stats", "reload"));
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("start") || args[0].equalsIgnoreCase("info"))) {
            return partial(args[1], manager.definitions().stream().map(EventDefinition::id).toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("stats")) {
            return partial(args[1], Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        }
        return List.of();
    }

    private List<String> partial(String input, List<String> values) {
        return values.stream().filter(x -> x.toLowerCase().startsWith(input.toLowerCase())).toList();
    }

    private static final class YamlHelp {
        static List<String> lines() {
            return List.of(
                    "<gradient:#8A2BE2:#FF4FD8><bold>✦ ZENVORA EVENTS ✦</bold></gradient>",
                    "<gray>/zevents list <dark_gray>— Ver eventos</dark_gray>",
                    "<gray>/zevents info <id> <dark_gray>— Información</dark_gray>",
                    "<gray>/zevents start <id> <dark_gray>— Iniciar evento</dark_gray>",
                    "<gray>/zevents stop <dark_gray>— Detener evento</dark_gray>",
                    "<gray>/zevents top <dark_gray>— Clasificación</dark_gray>",
                    "<gray>/zevents stats [jugador] <dark_gray>— Estadísticas</dark_gray>",
                    "<gray>/zevents reload <dark_gray>— Recargar configuración</dark_gray>"
            );
        }
    }
}
