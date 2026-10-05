package org.itzmr.restartify.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.itzmr.restartify.Restartify;
import org.itzmr.restartify.config.ConfigManager;
import org.itzmr.restartify.manager.RestartManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Обработчик команды {@code /reboot} и её подкоманд
 * {@code cancel}, {@code reload}.
 *
 * <p>Команда {@code /restart} намеренно НЕ регистрируется: она встроена в ядро
 * ({@code org.spigotmc.RestartCommand}), и её перехват приводил к зацикливанию
 * {@code dispatchCommand("restart") → Restartify → dispatchCommand("restart") → ...}.
 * Теперь перезапуск выполняется напрямую через {@code Bukkit.spigot().restart()}.</p>
 */
public final class RestartCommand implements CommandExecutor, TabCompleter {

    private static final String COMMAND_NAME = "reboot";

    private static final String SUB_START = "start";
    private static final String SUB_CANCEL = "cancel";
    private static final String SUB_RELOAD = "reload";

    private static final List<String> SUBCOMMANDS =
            Collections.unmodifiableList(Arrays.asList(SUB_START, SUB_CANCEL, SUB_RELOAD));

    private final Restartify plugin;

    public RestartCommand(Restartify plugin) {
        this.plugin = plugin;
    }

    /**
     * Регистрирует исполнителя и автодополнение для {@code /reboot}.
     */
    public void register() {
        PluginCommand command = plugin.getCommand(COMMAND_NAME);
        if (command == null) {
            plugin.getLogger().severe("Restartify: команда /" + COMMAND_NAME
                    + " отсутствует в plugin.yml — команда не будет работать!");
            return;
        }
        command.setExecutor(this);
        command.setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        ConfigManager config = plugin.getConfigManager();

        if (!sender.hasPermission(Restartify.ADMIN_PERMISSION)) {
            config.sendNoPermission(sender);
            return true;
        }

        String subCommand = args.length == 0 ? SUB_START : args[0].toLowerCase(Locale.ROOT);

        switch (subCommand) {
            case SUB_START:
                plugin.getRestartManager().start(sender);
                break;

            case SUB_CANCEL:
                plugin.getRestartManager().cancel(sender);
                break;

            case SUB_RELOAD:
                reloadConfiguration(sender);
                break;

            default:
                config.sendUsage(sender);
                break;
        }

        return true;
    }

    private void reloadConfiguration(CommandSender sender) {
        ConfigManager config = plugin.getConfigManager();
        if (plugin.reloadPluginConfiguration()) {
            config.sendReloadSuccess(sender);
        } else {
            config.sendReloadFailed(sender);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(Restartify.ADMIN_PERMISSION)) {
            return Collections.emptyList();
        }
        if (args.length != 1) {
            return Collections.emptyList();
        }

        RestartManager manager = plugin.getRestartManager();
        String prefix = args[0].toLowerCase(Locale.ROOT);

        List<String> result = new ArrayList<>(3);
        for (String subCommand : SUBCOMMANDS) {
            if (manager.isRunning() && SUB_START.equals(subCommand)) {
                // Повторный запуск бессмысленен — не подсказываем.
                continue;
            }
            if (subCommand.startsWith(prefix)) {
                result.add(subCommand);
            }
        }
        return result;
    }
}