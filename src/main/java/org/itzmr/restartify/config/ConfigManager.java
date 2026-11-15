package org.itzmr.restartify.config;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.itzmr.restartify.Restartify;
import org.itzmr.restartify.utils.MessageUtils;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
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
 * Loads and validates {@code config.yml} and the active localization file
 * ({@code messages.yml} or {@code messages_ru.yml}), caches frequently accessed
 * settings, and provides formatted Adventure {@link Component} messages.
 */
public final class ConfigManager {

    /** Action executed when the countdown reaches zero. */
    public enum RestartAction {
        /** Native core restart: {@code Bukkit.spigot().restart()}. */
        RESTART,
        /** Graceful server shutdown: {@code Bukkit.shutdown()}. */
        SHUTDOWN
    }

    /** Chat announcement scheduling mode during the countdown. */
    public enum AnnounceMode {
        /** Every N seconds. */
        INTERVAL,
        /** Only at the specified second marks. */
        SCHEDULE,
        /** Do not send countdown chat announcements. */
        OFF
    }

    private static final String CONFIG_FILE = "config.yml";
    private static final String MESSAGES_FILE = "messages.yml";
    private static final String MESSAGES_RU_FILE = "messages_ru.yml";
    private static final String LOCALE_EN = "en";
    private static final String LOCALE_RU = "ru";
    private static final DateTimeFormatter SCHEDULE_TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm", Locale.ROOT);

    // --- default fallback values (used if a key is missing) ---
    private static final String DEFAULT_STARTED = "<gray>Restart initiated by <white>%sender%<gray>, restarting in <white>%time%<gray>.";
    private static final String DEFAULT_ANNOUNCEMENT = "<red><bold>Attention!</bold></red> <gray>The server will restart in <white>%time%";
    private static final String DEFAULT_ALREADY_RUNNING = "<yellow>A restart is already scheduled. Time remaining: <white>%time%<gray>.";
    private static final String DEFAULT_RESTARTING = "<red><bold>Restarting...</bold></red> <gray>The server is restarting, please wait.";
    private static final String DEFAULT_NOT_RUNNING = "<yellow>No restart countdown is currently running.";
    private static final String DEFAULT_CANCELED = "<gray>Countdown cancelled by <white>%sender%<gray> with <white>%time%<gray> remaining.";
    private static final String DEFAULT_CANCELED_BROADCAST = "<yellow>Server restart has been <red>cancelled<yellow>.";
    private static final String DEFAULT_RELOAD_SUCCESS = "<green>Restartify configuration successfully reloaded.";
    private static final String DEFAULT_RELOAD_FAILED = "<red>Failed to reload configuration. Check console for details.";
    private static final String DEFAULT_RESTART_FALLBACK = "<red>Native restart is not supported by this server core, shutting down instead: <white>%error%";
    private static final String DEFAULT_NO_PERMISSION = "<red>You do not have permission to use this command.";
    private static final String DEFAULT_USAGE = "<gray>Usage: <white>/reboot <gray>[start|cancel|reload]";
    private static final String DEFAULT_BOSSBAR_TITLE = "<red><bold>Restarting in</bold> <white>%time%";
    private static final String DEFAULT_PVP_DENY_MESSAGE = "<red>PvP is disabled! The server is about to restart.</red>";

    private final Restartify plugin;

    private FileConfiguration config;
    private FileConfiguration messages;

    // --- cached configuration values ---
    private String locale = LOCALE_EN;
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
    private boolean pvpProtectionEnabled = true;
    private String pvpDenyMessage = DEFAULT_PVP_DENY_MESSAGE;
    private Component pvpDenyComponent = Component.empty();
    private boolean scheduledRestartEnabled = true;
    private ZoneId scheduledRestartZone = ZoneId.systemDefault();
    private List<LocalTime> scheduledRestartTimes = Collections.emptyList();

    // --- cached message settings ---
    private boolean prefixEnabled = true;
    private Component prefix = Component.empty();

    public ConfigManager(Restartify plugin) {
        this.plugin = plugin;
    }

    // ==================================================================
    //  Loading & Reloading
    // ==================================================================

    /**
     * Extracts missing default files from the JAR and reloads configuration and messages.
     *
     * @return {@code true} if loading succeeded
     */
    public boolean reload() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create plugin data folder: {0}", dataFolder);
            return false;
        }

        try {
            saveResourceIfAbsent(CONFIG_FILE);
            saveResourceIfAbsent(MESSAGES_FILE);

            config = YamlConfiguration.loadConfiguration(new File(dataFolder, CONFIG_FILE));
            locale = resolveLocale(config.getString("locale", LOCALE_EN));

            String activeMessagesFile = MESSAGES_FILE;
            if (LOCALE_RU.equals(locale)) {
                saveResourceIfAbsent(MESSAGES_RU_FILE);
                activeMessagesFile = MESSAGES_RU_FILE;
            }

            messages = loadMessagesWithDefaults(new File(dataFolder, activeMessagesFile), activeMessagesFile);

            cacheMessages();
            cacheConfig();

            plugin.getLogger().info("Restartify: configuration loaded "
                    + "(locale " + locale + ", countdown " + countdownSeconds + "s, mode " + announceMode + ").");
            return true;
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Error while loading Restartify configuration", exception);
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

    private String resolveLocale(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return LOCALE_EN;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (LOCALE_EN.equals(normalized) || LOCALE_RU.equals(normalized)) {
            return normalized;
        }
        plugin.getLogger().warning("Restartify: unknown locale '" + raw + "' in config.yml — falling back to 'en'.");
        return LOCALE_EN;
    }

    private FileConfiguration loadMessagesWithDefaults(File file, String resourceName) {
        YamlConfiguration loaded = YamlConfiguration.loadConfiguration(file);
        try (InputStream stream = plugin.getResource(resourceName)) {
            if (stream != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(stream, StandardCharsets.UTF_8));
                loaded.setDefaults(defaults);
            }
        } catch (Exception exception) {
            plugin.getLogger().log(Level.WARNING,
                    "Failed to load embedded default messages resource: " + resourceName, exception);
        }
        return loaded;
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
            plugin.getLogger().warning("Restartify: announce.mode=SCHEDULE, but announce.seconds is empty — "
                    + "falling back to INTERVAL.");
            announceMode = AnnounceMode.INTERVAL;
        }
        announceSeconds = Collections.unmodifiableSet(new LinkedHashSet<>(seconds));

        bossBarEnabled = config.getBoolean("bossbar.enabled", true);
        bossBarColor = parseColor(config.getString("bossbar.color", "RED"));
        bossBarOverlay = parseOverlay(config.getString("bossbar.overlay", "SOLID"));
        bossBarDarkenScreen = config.getBoolean("bossbar.darken-screen", true);

        restartAction = parseAction(config.getString("restart.action", "RESTART"));
        restartDelayTicks = Math.max(0, config.getInt("restart.delay-ticks", 20));

        pvpProtectionEnabled = config.getBoolean("pvp-protection.enabled", true);
        String rawPvpDeny = config.getString("pvp-protection.deny-message");
        if (rawPvpDeny == null || rawPvpDeny.trim().isEmpty()) {
            rawPvpDeny = DEFAULT_PVP_DENY_MESSAGE;
        }
        pvpDenyMessage = rawPvpDeny;
        Component parsedPvpDeny = MessageUtils.parse(pvpDenyMessage);
        pvpDenyComponent = prefixEnabled ? MessageUtils.join(prefix, parsedPvpDeny) : parsedPvpDeny;

        scheduledRestartEnabled = config.getBoolean("scheduled-restart.enabled", true);
        scheduledRestartZone = parseZoneId(config.getString("scheduled-restart.timezone", "Europe/Moscow"));
        scheduledRestartTimes = parseScheduledTimes(config.getStringList("scheduled-restart.times"));
    }

    private ZoneId parseZoneId(String raw) {
        ZoneId fallback = ZoneId.systemDefault();
        if (raw == null || raw.trim().isEmpty()) {
            plugin.getLogger().warning("Restartify: scheduled-restart.timezone is not set — "
                    + "using system default timezone (" + fallback.getId() + ").");
            return fallback;
        }

        String trimmed = raw.trim();
        try {
            return ZoneId.of(trimmed);
        } catch (DateTimeException exception) {
            plugin.getLogger().warning("Restartify: unknown timezone '" + trimmed
                    + "' in scheduled-restart.timezone — falling back to system default timezone ("
                    + fallback.getId() + "). See TIMEZONES.md for valid identifiers.");
            return fallback;
        }
    }

    private List<LocalTime> parseScheduledTimes(List<String> rawTimes) {
        if (rawTimes == null || rawTimes.isEmpty()) {
            return Collections.emptyList();
        }
        Set<LocalTime> uniqueTimes = new TreeSet<>();
        for (String entry : rawTimes) {
            if (entry == null || entry.trim().isEmpty()) {
                continue;
            }
            String trimmed = entry.trim();
            try {
                uniqueTimes.add(LocalTime.parse(trimmed, SCHEDULE_TIME_FORMATTER));
            } catch (DateTimeParseException exception) {
                plugin.getLogger().warning("Restartify: invalid time '" + trimmed
                        + "' in scheduled-restart.times (expected 24-hour HH:mm format, e.g. 04:00).");
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(uniqueTimes));
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
                plugin.getLogger().warning("Restartify: unknown announce.mode '" + raw
                        + "' — using INTERVAL.");
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
            plugin.getLogger().warning("Restartify: unknown bossbar.color '" + raw + "' — using RED.");
            return BossBar.Color.RED;
        }
    }

    private BossBar.Overlay parseOverlay(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return BossBar.Overlay.PROGRESS;
        }

        String value = raw.trim().toUpperCase(Locale.ROOT);
        switch (value) {
            // Adventure names the continuous bar PROGRESS; SOLID is the Bukkit API name.
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
                plugin.getLogger().warning("Restartify: unknown bossbar.overlay '" + raw
                        + "' — using SOLID. Allowed: SOLID, NOTCHED_6, NOTCHED_10, NOTCHED_12, NOTCHED_20.");
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
            // Legacy mode dispatched the "restart" command via dispatchCommand,
            // which caused loops when plugins intercepted /restart; map to native RESTART.
            plugin.getLogger().warning("Restartify: restart.action=COMMAND is no longer supported "
                    + "(command-based restarts caused loops) — using native RESTART.");
            return RestartAction.RESTART;
        }

        plugin.getLogger().warning("Restartify: unknown restart.action '" + raw
                + "' — using RESTART. Allowed: RESTART, SHUTDOWN.");
        return RestartAction.RESTART;
    }

    // ==================================================================
    //  Configuration Getters
    // ==================================================================

    public String getLocale() {
        return locale;
    }

    public int getCountdownSeconds() {
        return countdownSeconds;
    }

    public boolean isChatEnabled() {
        return chatEnabled;
    }

    /**
     * Checks whether a countdown chat announcement should be sent at the given second.
     *
     * @param remaining remaining seconds
     * @param total     total countdown duration in seconds
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

    public boolean isPvpProtectionEnabled() {
        return pvpProtectionEnabled;
    }

    public String getPvpDenyMessage() {
        return pvpDenyMessage;
    }

    public Component getPvpDenyComponent() {
        return pvpDenyComponent;
    }

    public boolean isScheduledRestartEnabled() {
        return scheduledRestartEnabled;
    }

    public ZoneId getScheduledRestartZone() {
        return scheduledRestartZone;
    }

    public List<LocalTime> getScheduledRestartTimes() {
        return scheduledRestartTimes;
    }

    // ==================================================================
    //  Messages
    // ==================================================================

    /**
     * Returns a raw string from the active messages file with placeholders applied.
     */
    public String getRawMessage(String path, String fallback, Map<String, String> placeholders) {
        String value = (messages == null) ? null : messages.getString(path);
        if (value == null) {
            value = fallback;
        }
        return MessageUtils.applyPlaceholders(value, placeholders);
    }

    /**
     * Parses a message component without the chat prefix (e.g. BossBar title).
     */
    public Component getComponent(String path, String fallback, Map<String, String> placeholders) {
        return MessageUtils.parse(getRawMessage(path, fallback, placeholders));
    }

    /**
     * Parses a message component with the chat prefix prepended (when enabled).
     */
    public Component getPrefixedComponent(String path, String fallback, Map<String, String> placeholders) {
        Component main = getComponent(path, fallback, placeholders);
        return prefixEnabled ? MessageUtils.join(prefix, main) : main;
    }

    /**
     * Returns the formatted BossBar title (without prefix).
     */
    public Component getBossBarTitle(Map<String, String> placeholders) {
        return getComponent("bossbar.title", DEFAULT_BOSSBAR_TITLE, placeholders);
    }

    // ==================================================================
    //  Pre-built Plugin Messages
    // ==================================================================

    /** {@code started} — sent to admins and console when a restart starts. */
    public void sendStarted(Map<String, String> placeholders) {
        if (!chatEnabled) {
            return;
        }
        MessageUtils.sendToAdmins(getPrefixedComponent("started", DEFAULT_STARTED, placeholders));
    }

    /** {@code announcement} — broadcast to all online players. */
    public void sendAnnouncement(Map<String, String> placeholders) {
        if (!chatEnabled) {
            return;
        }
        MessageUtils.broadcast(getPrefixedComponent("announcement", DEFAULT_ANNOUNCEMENT, placeholders));
    }

    /** {@code already-running} — sent only to the command initiator. */
    public void sendAlreadyRunning(CommandSender sender, Map<String, String> placeholders) {
        MessageUtils.send(sender, getPrefixedComponent("already-running", DEFAULT_ALREADY_RUNNING, placeholders));
    }

    /** {@code restarting} — broadcast to all online players. */
    public void sendRestarting(Map<String, String> placeholders) {
        if (!chatEnabled) {
            return;
        }
        MessageUtils.broadcast(getPrefixedComponent("restarting", DEFAULT_RESTARTING, placeholders));
    }

    /** {@code canceled} — sent to admins and console when cancelled. */
    public void sendCanceled(Map<String, String> placeholders) {
        if (!chatEnabled) {
            return;
        }
        MessageUtils.sendToAdmins(getPrefixedComponent("canceled", DEFAULT_CANCELED, placeholders));
    }

    /** {@code canceled-broadcast} — broadcast to all online players. */
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
     * {@code restart-fallback} — native restart is unsupported by the core;
     * the server will shut down via {@code Bukkit.shutdown()}.
     */
    public void sendRestartFallback(String error) {
        Map<String, String> placeholders = new HashMap<>(2);
        placeholders.put("%error%", error == null ? "unknown" : error);
        MessageUtils.broadcast(getPrefixedComponent("restart-fallback", DEFAULT_RESTART_FALLBACK, placeholders));
    }
}
