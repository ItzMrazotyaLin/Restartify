package org.itzmr.restartify.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.itzmr.restartify.manager.RestartManager;

/**
 * Показывает активный BossBar игроку, который вошёл во время отсчёта,
 * и корректно убирает его при выходе.
 */
public final class PlayerListener implements Listener {

    private final RestartManager restartManager;

    public PlayerListener(RestartManager restartManager) {
        this.restartManager = restartManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        restartManager.showBossBarTo(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        restartManager.hideBossBarFrom(event.getPlayer());
    }
}