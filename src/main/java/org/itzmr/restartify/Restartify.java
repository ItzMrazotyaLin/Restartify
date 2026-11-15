package org.itzmr.restartify;

import org.bukkit.plugin.java.JavaPlugin;
import org.itzmr.restartify.commands.RestartCommand;
import org.itzmr.restartify.config.ConfigManager;
import org.itzmr.restartify.listeners.PlayerListener;
import org.itzmr.restartify.listeners.PvpListener;
import org.itzmr.restartify.manager.RestartManager;
import org.itzmr.restartify.manager.RestartScheduler;

import java.util.logging.Level;

/**
 * Restartify — graceful server restart countdown plugin.
 *
 * <p>Commands: {@code /reboot} (with {@code start}, {@code cancel}, {@code reload}
 * subcommands). Permission: {@link #ADMIN_PERMISSION}.</p>
 */
public final class Restartify extends JavaPlugin {

    /** Permission required to start, cancel, or reload restarts (OP by default). */
    public static final String ADMIN_PERMISSION = "restartify.admin";

    private ConfigManager configManager;
    private RestartManager restartManager;
    private RestartScheduler restartScheduler;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);

        if (!configManager.reload()) {
            getLogger().severe("Restartify: failed to load configuration — disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        restartManager = new RestartManager(this, configManager);
        restartScheduler = new RestartScheduler(this, configManager, restartManager);

        new RestartCommand(this).register();
        getServer().getPluginManager().registerEvents(new PlayerListener(restartManager), this);
        getServer().getPluginManager().registerEvents(new PvpListener(configManager, restartManager), this);

        restartScheduler.startOrUpdate();

        getLogger().info("Restartify " + getDescription().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        if (restartScheduler != null) {
            restartScheduler.stop();
        }
        if (restartManager != null) {
            restartManager.shutdown();
        }
        getLogger().info("Restartify disabled.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public RestartManager getRestartManager() {
        return restartManager;
    }

    public RestartScheduler getRestartScheduler() {
        return restartScheduler;
    }

    /**
     * Reloads {@code config.yml} and the active messages file.
     * An active countdown is not interrupted — updated messages apply immediately.
     *
     * @return {@code true} if the reload succeeded
     */
    public boolean reloadPluginConfiguration() {
        if (configManager == null) {
            return false;
        }
        try {
            boolean success = configManager.reload();
            if (!success) {
                getLogger().log(Level.SEVERE, "Restartify: configuration reload failed.");
                return false;
            }
            if (restartManager != null) {
                restartManager.onConfigReloaded();
            }
            if (restartScheduler != null) {
                restartScheduler.startOrUpdate();
            }
            return true;
        } catch (RuntimeException exception) {
            getLogger().log(Level.SEVERE, "Restartify: exception while reloading configuration", exception);
            return false;
        }
    }
}
