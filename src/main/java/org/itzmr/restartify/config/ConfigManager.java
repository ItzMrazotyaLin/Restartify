package org.itzmr.restartify.config;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.itzmr.restartify.Restartify;
import org.itzmr.restartify.utils.MessageUtils;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;

/**
 * Загружает и валидирует {@code config.yml} и {@code messages.yml},
 * кэширует часто используемые значения и отдаёт готовые {@link Component}-сообщения.
 */
public final class ConfigManager {

    /** Что делать по окончании отсчёта. */
    public enum RestartAction {
        /** Нативный рестарт ядра: {@code Bukkit.spigot().restart()}. */
        RESTART,
        /** Корректное выключение сервера: {@code Bukkit.shutdown()}. */
        SHUTDOWN
    }

    /** Режим отправки сообщений обратного отсчёта. */
    public enum AnnounceMode {
        /** Каждые N секунд. */
        INTERVAL,
        /** Только на указанных секундах. */
        SCHEDULE,
        /** Не отправлять вовсе. */
        OFF
    }

    private static final String CONFIG_FILE = "config.yml";
    private static final String MESSAGES_FILE = "messages.yml";

    // --- значения по умолчанию (используются, если ключ отсутствует) ---
    private static final String DEFAULT_STARTED = "<gray>Рестарт запущен: <white>%sender%<gray>, перезапуск через <white>%time%<gray>.";
    private static final String DEFAULT_ANNOUNCEMENT = "<red><bold>Внимание!</bold></red> <gray>Сервер будет перезапущен через <white>%time%";
    private static final String DEFAULT_ALREADY_RUNNING = "<yellow>Рестарт уже запланирован. Осталось <white>%time%<gray>.";
    private static final String DEFAULT_RESTARTING = "<red><bold>Перезапуск...</bold></red> <gray>Сервер перезагружается, ожидайте.";
    private static final String DEFAULT_NOT_RUNNING = "<yellow>Обратный отсчёт не запущен.";
    private static final String DEFAULT_CANCELED = "<gray>Отсчёт отменён: <white>%sender%<gray>, оставалось <white>%time%<gray>.";
    private static final String DEFAULT_CANCELED_BROADCAST = "<yellow>Перезапуск сервера <red>отменён<yellow>.";
    private static final String DEFAULT_RELOAD_SUCCESS = "<green>Конфигурация Restartify успешно перезагружена.";
    private static final String DEFAULT_RELOAD_FAILED = "<red>Не удалось перезагрузить конфигурацию. Подробности в консоли.";
    private static final String DEFAULT_RESTART_FALLBACK = "<red>Нативный рестарт не поддерживается ядром, сервер будет выключен: <white>%error%";
    private static final String DEFAULT_NO_PERMISSION = "<red>У вас нет прав на использование этой команды.";
    private static final String DEFAULT_USAGE = "<gray>Использование: <white>/reboot <gray>[start|cancel|reload]";
    private static final String DEFAULT_BOSSBAR_TITLE = "<red><bold>Рестарт через</bold> <white>%time%";

    private final Restartify plugin;

    private FileConfiguration config;
    private FileConfiguration messages;

    // --- закэшированные значения ---
    private int countdownSeconds = 60;
    private boolean chatEnabled = true;
    private AnnounceMode announceMode = AnnounceMode.INTERVAL;
    private int announceInterval = 15;
    private Set<Integer> announceSeconds = Collections.emptySet();
    private boolean bossBarEnabled = true;
    private BossBar.Color bossBarColor = BossBar.Color.RED;
    private BossBar.Overlay bossBarOverlay = BossBar.Overlay.PROGRESS;
    private boolean bossBarDarkenScreen = true;
    private RestartAction restartAction = RestartAction.RESTART;
    private int restartDelayTicks = 20;

    // --- сообщения ---
    private boolean prefixEnabled = true;
    private Component prefix = Component.empty();

    public ConfigManager(Restartify plugin) {
        this.plugin = plugin;
    }

    // ==================================================================
    //  Загрузка
    // ==================================================================

    /**
     * Копирует отсутствующие файлы из jar и перечитывает их.
     *
     * @return {@code true}, если конфигурация успешно загружена
     */
    public boolean reload() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().log(Level.SEVERE, "Не удалось создать папку плагина: {0}", dataFolder);
            return false;
        }

        try {
            saveResourceIfAbsent(CONFIG_FILE);
            saveResourceIfAbsent(MESSAGES_FILE);

            config = YamlConfiguration.loadConfiguration(new File(dataFolder, CONFIG_FILE));
            messages = YamlConfiguration.loadConfiguration(new File(dataFolder, MESSAGES_FILE));

            cacheMessages();
            cacheConfig();

            plugin.getLogger().info("Restartify: конфигурация загружена "
                    + "(отсчёт " + countdownSeconds + " c, режим " + announceMode + ").");
            return true;
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка при загрузке конфигурации Restartify", exception);
            return false;
        }
    }

    private void saveResourceIfAbsent(String name) {
        File file = new File(plugin.getDataFolder(), name);
        if (file.exists()) {
            return;
        }
        plugin.saveResource(name, false);
    }

    private void cacheMessages() {
        prefixEnabled = messages.getBoolean("settings.prefix-enabled", true);
        String rawPrefix = messages.getString("prefix", "");
        prefix = prefixEnabled ? MessageUtils.parse(rawPrefix) : Component.empty();
    }

    private void cacheConfig() {
        countdownSeconds = Math.max(1, config.getInt("countdown.seconds", 60));

        chatEnabled = config.getBoolean("chat.enabled", true);

        announceMode = parseMode(config.getString("announce.mode", "INTERVAL"));
        announceInterval = Math.max(1, config.getInt("announce.interval-seconds", 15));

        Set<Integer> seconds = new TreeSet<>();
        List<Integer> rawSeconds = config.getIntegerList("announce.seconds");
        if (rawSeconds != null) {
            for (Integer value : rawSeconds) {
                if (value != null && value.intValue() > 0) {
                    seconds.add(value);
                }
            }
        }
        if (announceMode == AnnounceMode.SCHEDULE && seconds.isEmpty()) {
            plugin.getLogger().warning("Restartify: announce.mode=SCHEDULE, но announce.seconds пуст — "
                    + "переключаюсь на INTERVAL.");
            announceMode = AnnounceMode.INTERVAL;
        }
        announceSeconds = Collections.unmodifiableSet(new LinkedHashSet<>(seconds));

        bossBarEnabled = config.getBoolean("bossbar.enabled", true);
        bossBarColor = parseColor(config.getString("bossbar.color", "RED"));
        bossBarOverlay = parseOverlay(config.getString("bossbar.overlay", "SOLID"));
        bossBarDarkenScreen = config.getBoolean("bossbar.darken-screen", true);

        restartAction = parseAction(config.getString("restart.action", "RESTART"));
        restartDelayTicks = Math.max(0, config.getInt("restart.delay-ticks", 20));
    }

    private AnnounceMode parseMode(String raw) {
        if (raw == null) {
            return AnnounceMode.INTERVAL;
        }
        String value = raw.trim().toUpperCase(Locale.ROOT);
        switch (value) {
            case "INTERVAL":
                return AnnounceMode.INTERVAL;
            case "SCHEDULE":
            case "LIST":
            case "SECONDS":
                return AnnounceMode.SCHEDULE;
            case "OFF":
            case "NONE":
            case "DISABLED":
                return AnnounceMode.OFF;
            default:
                plugin.getLogger().warning("Restartify: неизвестный announce.mode '" + raw
                        + "' — используется INTERVAL.");
                return AnnounceMode.INTERVAL;
        }
    }

    private BossBar.Color parseColor(String raw) {
        if (raw == null) {
            return BossBar.Color.RED;
        }
        try {
            return BossBar.Color.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Restartify: неизвестный bossbar.color '" + raw + "' — используется RED.");
            return BossBar.Color.RED;
        }
    }

    private BossBar.Overlay parseOverlay(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return BossBar.Overlay.PROGRESS;
        }

        String value = raw.trim().toUpperCase(Locale.ROOT);
        switch (value) {
            // Adventure называет «сплошной» бар PROGRESS, SOLID — это имя из Bukkit API.
            case "SOLID":
            case "PROGRESS":
                return BossBar.Overlay.PROGRESS;
            case "NOTCHED_6":
                return BossBar.Overlay.NOTCHED_6;
            case "NOTCHED_10":
                return BossBar.Overlay.NOTCHED_10;
            case "NOTCHED_12":
                return BossBar.Overlay.NOTCHED_12;
            case "NOTCHED_20":
                return BossBar.Overlay.NOTCHED_20;
            default:
                plugin.getLogger().warning("Restartify: неизвестный bossbar.overlay '" + raw
                        + "' — используется SOLID. Допустимо: SOLID, NOTCHED_6, NOTCHED_10, NOTCHED_12, NOTCHED_20.");
                return BossBar.Overlay.PROGRESS;
        }
    }

    private RestartAction parseAction(String raw) {
        if (raw == null) {
            return RestartAction.RESTART;
        }
        String value = raw.trim().toUpperCase(Locale.ROOT);

        if ("RESTART".equals(value)) {
            return RestartAction.RESTART;
        }
        if ("SHUTDOWN".equals(value) || "STOP".equals(value) || "KICK".equals(value)) {
            return RestartAction.SHUTDOWN;
        }
        if ("COMMAND".equals(value) || "CONSOLE".equals(value)) {
            // Старый режим отправлял команду "restart" через dispatchCommand.
            // Теперь это привело бы к зацикливанию (плагин перехватывал /restart),
            // поэтому приводим к нативному рестарту ядра.
            plugin.getLogger().warning("Restartify: restart.action=COMMAND больше не поддерживается "
                    + "(командный рестарт зацикливался) — используется нативный RESTART.");
            return RestartAction.RESTART;
        }

        plugin.getLogger().warning("Restartify: неизвестный restart.action '" + raw
                + "' — используется RESTART. Допустимо: RESTART, SHUTDOWN.");
        return RestartAction.RESTART;
    }

    // ==================================================================
    //  Значения конфигурации
    // ==================================================================

    public int getCountdownSeconds() {
        return countdownSeconds;
    }

    public boolean isChatEnabled() {
        return chatEnabled;
    }

    /**
     * Нужно ли отправлять сообщение обратного отсчёта на указанной секунде.
     *
     * @param remaining оставшиеся секунды
     * @param total     общая длительность отсчёта
     */
    public boolean shouldAnnounce(int remaining, int total) {
        if (remaining <= 0) {
            return false;
        }
        switch (announceMode) {
            case OFF:
                return false;
            case SCHEDULE:
                return announceSeconds.contains(remaining);
            case INTERVAL:
            default: {
                int elapsed = Math.max(0, total - remaining);
                return elapsed % announceInterval == 0;
            }
        }
    }

    public boolean isBossBarEnabled() {
        return bossBarEnabled;
    }

    public BossBar.Color getBossBarColor() {
        return bossBarColor;
    }

    public BossBar.Overlay getBossBarOverlay() {
        return bossBarOverlay;
    }

    public boolean isBossBarDarkenScreen() {
        return bossBarDarkenScreen;
    }

    public RestartAction getRestartAction() {
        return restartAction;
    }

    public int getRestartDelayTicks() {
        return restartDelayTicks;
    }

    // ==================================================================
    //  Сообщения
    // ==================================================================

    /**
     * Сырая строка из {@code messages.yml} с подстановкой плейсхолдеров.
     */
    public String getRawMessage(String path, String fallback, Map<String, String> placeholders) {
        String value = (messages == null) ? null : messages.getString(path);
        if (value == null) {
            value = fallback;
        }
        return MessageUtils.applyPlaceholders(value, placeholders);
    }

    /**
     * Компонент без префикса (например, заголовок BossBar).
     */
    public Component getComponent(String path, String fallback, Map<String, String> placeholders) {
        return MessageUtils.parse(getRawMessage(path, fallback, placeholders));
    }

    /**
     * Компонент с префиксом — для сообщений в чат.
     */
    public Component getPrefixedComponent(String path, String fallback, Map<String, String> placeholders) {
        Component main = getComponent(path, fallback, placeholders);
        return prefixEnabled ? MessageUtils.join(prefix, main) : main;
    }

    /**
     * Заголовок BossBar (без префикса).
     */
    public Component getBossBarTitle(Map<String, String> placeholders) {
        return getComponent("bossbar.title", DEFAULT_BOSSBAR_TITLE, placeholders);
    }

    // ==================================================================
    //  Готовые сообщения плагина
    // ==================================================================

    /** {@code started} — админам, кто запустил рестарт. */
    public void sendStarted(Map<String, String> placeholders) {
        if (!chatEnabled) {
            return;
        }
        MessageUtils.sendToAdmins(getPrefixedComponent("started", DEFAULT_STARTED, placeholders));
    }

    /** {@code announcement} — всем игрокам. */
    public void sendAnnouncement(Map<String, String> placeholders) {
        if (!chatEnabled) {
            return;
        }
        MessageUtils.broadcast(getPrefixedComponent("announcement", DEFAULT_ANNOUNCEMENT, placeholders));
    }

    /** {@code already-running} — только инициатору команды. */
    public void sendAlreadyRunning(CommandSender sender, Map<String, String> placeholders) {
        MessageUtils.send(sender, getPrefixedComponent("already-running", DEFAULT_ALREADY_RUNNING, placeholders));
    }

    /** {@code restarting} — всем игрокам. */
    public void sendRestarting(Map<String, String> placeholders) {
        if (!chatEnabled) {
            return;
        }
        MessageUtils.broadcast(getPrefixedComponent("restarting", DEFAULT_RESTARTING, placeholders));
    }

    /** {@code canceled} — админам. */
    public void sendCanceled(Map<String, String> placeholders) {
        if (!chatEnabled) {
            return;
        }
        MessageUtils.sendToAdmins(getPrefixedComponent("canceled", DEFAULT_CANCELED, placeholders));
    }

    /** {@code canceled-broadcast} — всем игрокам. */
    public void sendCanceledBroadcast(Map<String, String> placeholders) {
        if (!chatEnabled) {
            return;
        }
        MessageUtils.broadcast(getPrefixedComponent("canceled-broadcast", DEFAULT_CANCELED_BROADCAST, placeholders));
    }

    public void sendNoPermission(CommandSender sender) {
        MessageUtils.send(sender, getPrefixedComponent("no-permission", DEFAULT_NO_PERMISSION, Collections.emptyMap()));
    }

    public void sendUsage(CommandSender sender) {
        MessageUtils.send(sender, getPrefixedComponent("usage", DEFAULT_USAGE, Collections.emptyMap()));
    }

    public void sendNotRunning(CommandSender sender) {
        MessageUtils.send(sender, getPrefixedComponent("not-running", DEFAULT_NOT_RUNNING, Collections.emptyMap()));
    }

    public void sendReloadSuccess(CommandSender sender) {
        MessageUtils.send(sender, getPrefixedComponent("reload-success", DEFAULT_RELOAD_SUCCESS, Collections.emptyMap()));
    }

    public void sendReloadFailed(CommandSender sender) {
        MessageUtils.send(sender, getPrefixedComponent("reload-failed", DEFAULT_RELOAD_FAILED, Collections.emptyMap()));
    }

    /**
     * {@code restart-fallback} — нативный рестарт не поддерживается ядром,
     * сервер будет выключен через {@code Bukkit.shutdown()}.
     */
    public void sendRestartFallback(String error) {
        Map<String, String> placeholders = new HashMap<>(2);
        placeholders.put("%error%", error == null ? "unknown" : error);
        MessageUtils.broadcast(getPrefixedComponent("restart-fallback", DEFAULT_RESTART_FALLBACK, placeholders));
    }
}