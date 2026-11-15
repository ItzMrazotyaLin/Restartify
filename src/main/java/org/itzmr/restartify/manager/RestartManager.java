package org.itzmr.restartify.manager;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.itzmr.restartify.Restartify;
import org.itzmr.restartify.config.ConfigManager;
import org.itzmr.restartify.utils.MessageUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages the restart countdown: timer, BossBar, announcements, PvP lock, and execution.
 *
 * <p>All operations run on the main server thread. Countdown accuracy is decoupled from
 * server TPS by tracking the wall-clock end timestamp, so server lag does not stretch
 * the countdown duration.</p>
 */
public final class RestartManager {

    private final Restartify plugin;
    private final ConfigManager config;

    private volatile boolean running;
    private volatile boolean isRestartPvpDisabled;
    private volatile int totalSeconds;
    private volatile long endTimeMillis;
    private volatile String initiatorName = "Console";
    private volatile BossBar bossBar;

    private final Set<UUID> bossBarViewers = ConcurrentHashMap.newKeySet();

    private BukkitTask task;
    /**
     * Value of {@code remaining} processed on the previous tick.
     * Initialized in {@link #start(CommandSender)} before starting the timer.
     */
    private int lastTickSecond;
    private boolean disabling;

    public RestartManager(Restartify plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    // ==================================================================
    //  Public API
    // ==================================================================

    public boolean isRunning() {
        return running;
    }

    /**
     * Returns {@code true} while PvP between players is disabled during an active countdown.
     */
    public boolean isRestartPvpDisabled() {
        return isRestartPvpDisabled;
    }

    /**
     * Returns remaining countdown seconds (for external display).
     */
    public int getRemainingSeconds() {
        return running ? remainingFrom(System.currentTimeMillis()) : 0;
    }

    /**
     * Starts the restart countdown.
     *
     * @return {@code true} if the countdown was started
     */
    public boolean start(CommandSender initiator) {
        if (running) {
            config.sendAlreadyRunning(initiator, placeholders(initiatorName, getRemainingSeconds()));
            return false;
        }

        totalSeconds = Math.max(1, config.getCountdownSeconds());
        endTimeMillis = System.currentTimeMillis() + totalSeconds * 1000L;
        initiatorName = resolveName(initiator);

        // The initial announcement is sent here, so mark the starting second as already
        // processed — otherwise the first timer tick would broadcast it a second time.
        lastTickSecond = totalSeconds;

        running = true;
        isRestartPvpDisabled = config.isPvpProtectionEnabled();

        createBossBar();
        showBossBarToEveryone();

        Map<String, String> placeholders = placeholders(initiatorName, totalSeconds);
        config.sendStarted(placeholders);
        config.sendAnnouncement(placeholders);

        startTask();

        plugin.getLogger().info("Restart countdown started (" + initiatorName + "), " + totalSeconds + "s.");
        return true;
    }

    /**
     * Starts a scheduled restart as Console (must be called on the main thread).
     */
    public void startScheduled() {
        if (running || disabling) {
            return;
        }
        start(Bukkit.getConsoleSender());
    }

    /**
     * Synchronizes PvP protection state after configuration reload.
     */
    public void onConfigReloaded() {
        isRestartPvpDisabled = running && config.isPvpProtectionEnabled();
    }

    /**
     * Cancels an active restart countdown.
     *
     * @return {@code true} if an active countdown was cancelled
     */
    public boolean cancel(CommandSender initiator) {
        if (!running) {
            config.sendNotRunning(initiator);
            return false;
        }

        int remaining = getRemainingSeconds();
        running = false;
        isRestartPvpDisabled = false;

        stopTask();
        removeBossBar();

        String canceller = resolveName(initiator);
        Map<String, String> placeholders = placeholders(canceller, remaining);
        config.sendCanceled(placeholders);
        config.sendCanceledBroadcast(placeholders);

        plugin.getLogger().info("Restart countdown cancelled (" + canceller + ").");
        return true;
    }

    /**
     * Displays the active BossBar to a newly joined player.
     */
    public void showBossBarTo(Player player) {
        BossBar bar = bossBar;
        if (player == null || bar == null || !running || !config.isBossBarEnabled()) {
            return;
        }
        player.showBossBar(bar);
        bossBarViewers.add(player.getUniqueId());
    }

    /**
     * Removes the BossBar from a disconnecting player.
     */
    public void hideBossBarFrom(Player player) {
        if (player == null) {
            return;
        }
        BossBar bar = bossBar;
        if (bar != null) {
            player.hideBossBar(bar);
        }
        bossBarViewers.remove(player.getUniqueId());
    }

    /**
     * Cleans up all tasks and BossBars during plugin/server shutdown (without chat broadcasts).
     */
    public void shutdown() {
        disabling = true;
        running = false;
        isRestartPvpDisabled = false;
        stopTask();
        removeBossBar();
        bossBar = null;
    }

    // ==================================================================
    //  Timer
    // ==================================================================

    private void startTask() {
        stopTask();
        // BukkitRunnable must be scheduled via its own runTaskTimer method rather than BukkitScheduler.
        task = new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void stopTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        if (!running) {
            stopTask();
            return;
        }

        int remaining = remainingFrom(System.currentTimeMillis());

        // Act only when the integer second changes to avoid duplicate broadcasts and redundant BossBar updates.
        if (remaining == lastTickSecond) {
            return;
        }
        lastTickSecond = remaining;

        updateBossBar(remaining);

        if (remaining <= 0) {
            finish();
            return;
        }

        if (config.shouldAnnounce(remaining, totalSeconds)) {
            config.sendAnnouncement(placeholders(initiatorName, remaining));
        }
    }

    private int remainingFrom(long now) {
        long delta = endTimeMillis - now;
        if (delta <= 0L) {
            return 0;
        }
        return (int) ((delta + 999L) / 1000L);
    }

    private void finish() {
        running = false;
        stopTask();
        removeBossBar();

        if (disabling) {
            return;
        }

        config.sendRestarting(placeholders(initiatorName, 0));
        plugin.getLogger().info("Countdown finished, executing restart (" + config.getRestartAction() + ").");

        long delay = Math.max(0L, config.getRestartDelayTicks());
        Bukkit.getScheduler().runTaskLater(plugin, this::executeRestart, delay);
    }

    /**
     * Executes the server restart or shutdown after the countdown ends.
     *
     * <p>Native restart invokes {@code Bukkit.spigot().restart()} directly rather than
     * dispatching {@code /restart}, avoiding command recursion. Falls back to
     * {@link Bukkit#shutdown()} if unsupported by the server core.</p>
     */
    @SuppressWarnings("deprecation")
    private void executeRestart() {
        if (config.getRestartAction() == ConfigManager.RestartAction.SHUTDOWN) {
            Bukkit.shutdown();
            return;
        }

        try {
            Bukkit.spigot().restart();
        } catch (Throwable throwable) {
            plugin.getLogger().log(Level.WARNING,
                    "Server core does not support Bukkit.spigot().restart() — shutting down via Bukkit.shutdown()",
                    throwable);
            config.sendRestartFallback(describe(throwable));
            Bukkit.shutdown();
        }
    }

    private static String describe(Throwable throwable) {
        String message = throwable.getMessage();
        return message == null || message.trim().isEmpty()
                ? throwable.getClass().getSimpleName()
                : message;
    }

    // ==================================================================
    //  BossBar
    // ==================================================================

    private void createBossBar() {
        if (!config.isBossBarEnabled()) {
            bossBar = null;
            return;
        }

        Map<String, String> placeholders = placeholders(initiatorName, totalSeconds);
        Component title = config.getBossBarTitle(placeholders);

        BossBar bar = BossBar.bossBar(title, 1.0f, config.getBossBarColor(), config.getBossBarOverlay());
        // Adventure 4.x controls darken-screen and boss music via flags.
        if (config.isBossBarDarkenScreen()) {
            bar.addFlag(BossBar.Flag.DARKEN_SCREEN);
        } else {
            bar.removeFlag(BossBar.Flag.DARKEN_SCREEN);
        }
        bar.removeFlag(BossBar.Flag.PLAY_BOSS_MUSIC);
        bar.name(title);

        bossBar = bar;
        bossBarViewers.clear();
    }

    private void showBossBarToEveryone() {
        BossBar bar = bossBar;
        if (bar == null) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showBossBar(bar);
            bossBarViewers.add(player.getUniqueId());
        }
    }

    private void updateBossBar(int remaining) {
        BossBar bar = bossBar;
        if (bar == null || !config.isBossBarEnabled()) {
            return;
        }

        double progress = totalSeconds <= 0
                ? 0.0D
                : Math.max(0.0D, Math.min(1.0D, remaining / (double) totalSeconds));
        bar.progress((float) progress);
        bar.name(config.getBossBarTitle(placeholders(initiatorName, remaining)));
    }

    private void removeBossBar() {
        BossBar bar = bossBar;
        if (bar == null) {
            bossBarViewers.clear();
            return;
        }

        for (UUID uuid : bossBarViewers) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                player.hideBossBar(bar);
            }
        }
        bossBarViewers.clear();

        // Safety pass: hide the bar from every online player.
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.hideBossBar(bar);
        }
    }

    // ==================================================================
    //  Utilities
    // ==================================================================

    /**
     * Builds the placeholder map for message templates: {@code %sender%}, {@code %time%},
     * {@code %seconds%}, {@code %total%}, {@code %progress%}.
     */
    public Map<String, String> placeholders(String sender, int remaining) {
        Map<String, String> placeholders = new HashMap<>(8);
        int safeTotal = Math.max(1, totalSeconds);
        int safeRemaining = Math.max(0, remaining);

        placeholders.put("%sender%", sender == null ? "Console" : sender);
        placeholders.put("%time%", MessageUtils.formatDuration(safeRemaining));
        placeholders.put("%seconds%", String.valueOf(safeRemaining));
        placeholders.put("%total%", String.valueOf(safeTotal));
        placeholders.put("%progress%", String.valueOf((safeRemaining * 100) / safeTotal));
        return placeholders;
    }

    private static String resolveName(CommandSender sender) {
        if (sender instanceof Player) {
            return ((Player) sender).getName();
        }
        if (sender == null || sender instanceof ConsoleCommandSender
                || sender.equals(Bukkit.getConsoleSender())) {
            return "Console";
        }
        String name = sender.getName();
        return name == null || name.trim().isEmpty() ? "Console" : name;
    }
}
