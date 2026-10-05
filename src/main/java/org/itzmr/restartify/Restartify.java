package org.itzmr.restartify;

import org.bukkit.plugin.java.JavaPlugin;
import org.itzmr.restartify.commands.RestartCommand;
import org.itzmr.restartify.config.ConfigManager;
import org.itzmr.restartify.listeners.PlayerListener;
import org.itzmr.restartify.manager.RestartManager;

import java.util.logging.Level;

/**
 * Restartify — предупреждение игроков о рестарте сервера.
 *
 * <p>Команды: {@code /reboot} (алиас {@code /restart}) и
 * {@code /reboot cancel}. Право: {@link #ADMIN_PERMISSION}.</p>
 */
public final class Restartify extends JavaPlugin {

    /** Право на запуск/отмену рестарта (по умолчанию только OP). */
    public static final String ADMIN_PERMISSION = "restartify.admin";

    private ConfigManager configManager;
    private RestartManager restartManager;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);

        if (!configManager.reload()) {
            getLogger().severe("Restartify: не удалось загрузить конфигурацию — плагин отключён.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        restartManager = new RestartManager(this, configManager);

        new RestartCommand(this).register();
        getServer().getPluginManager().registerEvents(new PlayerListener(restartManager), this);

        getLogger().info("Restartify " + getDescription().getVersion() + " включён.");
    }

    @Override
    public void onDisable() {
        if (restartManager != null) {
            restartManager.shutdown();
        }
        getLogger().info("Restartify выключен.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public RestartManager getRestartManager() {
        return restartManager;
    }

    /**
     * Перечитывает {@code config.yml} и {@code messages.yml}.
     * Активный отсчёт при этом не прерывается — новые тексты применяются сразу.
     *
     * @return {@code true}, если перезагрузка удалась
     */
    public boolean reloadPluginConfiguration() {
        if (configManager == null) {
            return false;
        }
        try {
            boolean success = configManager.reload();
            if (!success) {
                getLogger().log(Level.SEVERE, "Restartify: перезагрузка конфигурации не удалась.");
            }
            return success;
        } catch (RuntimeException exception) {
            getLogger().log(Level.SEVERE, "Restartify: исключение при перезагрузке конфигурации", exception);
            return false;
        }
    }
}