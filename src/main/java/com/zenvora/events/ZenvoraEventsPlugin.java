package com.zenvora.events;

import com.zenvora.events.command.EventsCommand;
import com.zenvora.events.data.PlayerStatsStore;
import com.zenvora.events.event.EventManager;
import com.zenvora.events.event.EventListener;
import com.zenvora.events.scheduler.EventScheduler;
import org.bukkit.plugin.java.JavaPlugin;

public final class ZenvoraEventsPlugin extends JavaPlugin {

    private EventManager eventManager;
    private PlayerStatsStore statsStore;
    private EventScheduler scheduler;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("events.yml", false);
        saveResource("messages.yml", false);

        this.statsStore = new PlayerStatsStore(this);
        this.eventManager = new EventManager(this, statsStore);

        getServer().getPluginManager().registerEvents(new EventListener(this, eventManager), this);
        getCommand("zevents").setExecutor(new EventsCommand(this, eventManager, statsStore));
        getCommand("zevents").setTabCompleter(new EventsCommand(this, eventManager, statsStore));

        if (getConfig().getBoolean("settings.scheduler-enabled", true)) {
            this.scheduler = new EventScheduler(this, eventManager);
            scheduler.start();
        }

        getLogger().info("Zenvora Events v" + getPluginMeta().getVersion() + " habilitado.");
        getLogger().info("Modular event engine listo.");
    }

    @Override
    public void onDisable() {
        if (scheduler != null) scheduler.stop();
        if (eventManager != null) eventManager.stopActiveEvent(false);
        if (statsStore != null) statsStore.save();
        getLogger().info("Zenvora Events deshabilitado.");
    }

    public void reloadPlugin() {
        reloadConfig();
        eventManager.reload();
    }

    public EventManager getEventManager() {
        return eventManager;
    }
}
