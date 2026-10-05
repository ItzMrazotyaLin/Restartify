package org.itzmr.restartify.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
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
import java.util.Map;

/**
 * Обработчик команд {@code /reboot} и {@code /restart}
 * (а также подкоманд {@code cancel} и {@code reload}).
 */
public final class RestartCommand implements CommandExecutor, TabCompleter {

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
     * Регистрирует исполнителя во всех командах плагина.
     */
    public void register() {
        registerExecutor("reboot");

        if (plugin.getConfigManager().isOverrideBuiltinRestart()) {
            takeOverBuiltinRestart();
        } else {
            plugin.getLogger().info("Restartify: /restart остаётся встроенной командой сервера "
                    + "(commands.override-builtin-restart: false).");
        }
    }

    private void registerExecutor(String name) {
        PluginCommand command = plugin.getCommand(name);
        if (command == null) {
            plugin.getLogger().warning("Restartify: команда /" + name + " отсутствует в plugin.yml.");
            return;
        }
        command.setExecutor(this);
        command.setTabCompleter(this);
    }

/**
     * Bukkit/Paper регистрирует встроенную команду {@code /restart}
     * ({@code org.spigotmc.RestartCommand}) ПОСЛЕ команд плагинов, поэтому одного
     * объявления в {@code plugin.yml} недостаточно.
     *
     * <p>Кроме того {@code SimpleCommandMap#register} отказывается перезаписывать ярлык,
     * если в {@code knownCommands} уже лежит запись с таким же ярлыком
     * (проверка {@code existing.getLabel().equals(label)}), поэтому старую запись нужно
     * предварительно вычистить из карты.</p>
     */
    private void takeOverBuiltinRestart() {
        PluginCommand ours = plugin.getCommand("restart");
        if (ours == null) {
            plugin.getLogger().warning("Restartify: команда /restart отсутствует в plugin.yml.");
            return;
        }
        ours.setExecutor(this);
        ours.setTabCompleter(this);

        CommandMap commandMap = Bukkit.getCommandMap();
        Command current = commandMap.getCommand("restart");
        if (current == ours) {
            return;
        }
        String previous = current == null ? "—" : current.getClass().getName();

        try {
            Map<String, Command> known = commandMap.getKnownCommands();
            if (current != null) {
                known.values().removeIf(command -> command == current);
            }
            known.remove("restart");
        } catch (UnsupportedOperationException exception) {
            plugin.getLogger().warning("Restartify: карта команд сервера недоступна для изменения, "
                    + "/restart остаётся встроенной командой. Используйте /reboot.");
            return;
        }

        commandMap.register("restart", ours);

        if (commandMap.getCommand("restart") != ours) {
            plugin.getLogger().warning("Restartify: не удалось перехватить /restart "
                    + "(занята командой " + previous + "). Используйте /reboot.");
        } else {
            plugin.getLogger().info("Restartify: команда /restart перехвачена (была: " + previous + ").");
        }
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