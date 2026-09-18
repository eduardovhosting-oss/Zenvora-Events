package com.zenvora.events.scheduler;

import com.zenvora.events.event.EventDefinition;
import com.zenvora.events.event.EventManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class EventScheduler {
    private final JavaPlugin plugin;
    private final EventManager manager;
    private BukkitTask task;

    public EventScheduler(JavaPlugin plugin, EventManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void start() {
        long minutes = Math.max(1, plugin.getConfig().getLong("settings.scheduler-minutes", 60));
        long ticks = minutes * 60L * 20L;
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (manager.isActive()) return;

            List<String> ids = plugin.getConfig().getStringList("scheduler.random-events");
            if (ids.isEmpty()) return;

            String id = ids.get(ThreadLocalRandom.current().nextInt(ids.size()));
            EventDefinition def = manager.get(id);
            if (def != null && def.enabled() && manager.cooldownRemaining(id) <= 0) {
                manager.start(id);
            }
        }, ticks, ticks);
    }

    public void stop() {
        if (task != null) task.cancel();
    }
}
