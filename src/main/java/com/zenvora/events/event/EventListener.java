package com.zenvora.events.event;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.entity.Player;

public final class EventListener implements Listener {
    private final EventManager manager;

    public EventListener(Object plugin, EventManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        manager.showBossbar(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onMine(BlockBreakEvent event) {
        if (!manager.isType(EventType.MINING)) return;
        String material = event.getBlock().getType().name();
        manager.addPoints(event.getPlayer(), manager.pointsFor(material));
    }

    @EventHandler(ignoreCancelled = true)
    public void onKill(EntityDeathEvent event) {
        if (!manager.isType(EventType.MOB_KILL)) return;
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        manager.addPoints(killer, manager.pointsFor(event.getEntityType().name()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (!manager.isType(EventType.FISHING) || event.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        Object caught = event.getCaught();
        String key = caught instanceof org.bukkit.entity.Item item ? item.getItemStack().getType().name() : "default";
        manager.addPoints(event.getPlayer(), manager.pointsFor(key));
    }

    @EventHandler(ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        // Reserved for future event modifiers and PvP events.
    }
}
