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
 * Управляет обратным отсчётом: таймер, BossBar, сообщения и сам рестарт.
 *
 * <p>Всё выполняется в основном потоке сервера. Точность отсчёта не зависит
 * от тиков сервера — используется абсолютное время окончания, поэтому лаги
 * не растягивают отсчёт.</p>
 */
public final class RestartManager {

    private final Restartify plugin;
    private final ConfigManager config;

    private volatile boolean running;
    private volatile int totalSeconds;
    private volatile long endTimeMillis;
    private volatile String initiatorName = "Console";
    private volatile BossBar bossBar;

    private final Set<UUID> bossBarViewers = ConcurrentHashMap.newKeySet();

    private BukkitTask task;
    private int lastTickSecond = Integer.MIN_VALUE;
    private boolean disabling;

    public RestartManager(Restartify plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    // ==================================================================
    //  Публичное API
    // ==================================================================

    public boolean isRunning() {
        return running;
    }

    /**
     * Оставшиеся секунды (для внешнего отображения).
     */
    public int getRemainingSeconds() {
        return running ? remainingFrom(System.currentTimeMillis()) : 0;
    }

    /**
     * @return {@code true}, если отсчёт был запущен
     */
    public boolean start(CommandSender initiator) {
        if (running) {
            config.sendAlreadyRunning(initiator, placeholders(initiatorName, getRemainingSeconds()));
            return false;
        }

        totalSeconds = Math.max(1, config.getCountdownSeconds());
        endTimeMillis = System.currentTimeMillis() + totalSeconds * 1000L;
        initiatorName = resolveName(initiator);
        lastTickSecond = Integer.MIN_VALUE;
        running = true;

        createBossBar();
        showBossBarToEveryone();

        Map<String, String> placeholders = placeholders(initiatorName, totalSeconds);
        config.sendStarted(placeholders);
        config.sendAnnouncement(placeholders);

        startTask();

        plugin.getLogger().info("Рестарт запущен (" + initiatorName + "), " + totalSeconds + " c.");
        return true;
    }

    /**
     * @return {@code true}, если отсчёт был активен и отменён
     */
    public boolean cancel(CommandSender initiator) {
        if (!running) {
            config.sendNotRunning(initiator);
            return false;
        }

        int remaining = getRemainingSeconds();
        running = false;

        stopTask();
        removeBossBar();

        String canceller = resolveName(initiator);
        Map<String, String> placeholders = placeholders(canceller, remaining);
        config.sendCanceled(placeholders);
        config.sendCanceledBroadcast(placeholders);

        plugin.getLogger().info("Рестарт отменён (" + canceller + ").");
        return true;
    }

    /**
     * Показывает активный BossBar вошедшему игроку.
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
     * Убирает BossBar у вышедшего игрока.
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
     * Полная очистка при выключении плагина/сервера (без сообщений).
     */
    public void shutdown() {
        disabling = true;
        running = false;
        stopTask();
        removeBossBar();
        bossBar = null;
    }

    // ==================================================================
    //  Таймер
    // ==================================================================

    private void startTask() {
        stopTask();
        // Важно: BukkitRunnable нельзя передавать в BukkitScheduler#runTaskTimer —
        // нужно вызывать собственный метод BukkitRunnable#runTaskTimer.
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
        updateBossBar(remaining);

        if (remaining == lastTickSecond) {
            return;
        }
        lastTickSecond = remaining;

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
        plugin.getLogger().info("Отсчёт завершён, выполняю рестарт (" + config.getRestartAction() + ").");

        long delay = Math.max(0L, config.getRestartDelayTicks());
        Bukkit.getScheduler().runTaskLater(plugin, this::executeRestart, delay);
    }

    private void executeRestart() {
        ConfigManager.RestartAction action = config.getRestartAction();

        if (action == ConfigManager.RestartAction.SHUTDOWN) {
            Bukkit.shutdown();
            return;
        }

        String command = config.getRestartCommand();
        if (command == null) {
            command = "";
        }
        command = command.trim();
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        if (command.isEmpty()) {
            plugin.getLogger().warning("restart.command пуст — выполняю Bukkit.shutdown().");
            Bukkit.shutdown();
            return;
        }

        try {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } catch (Throwable throwable) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка при выполнении команды '" + command + "'", throwable);
            config.sendCommandError(String.valueOf(throwable.getMessage()));
            Bukkit.shutdown();
            return;
        }

        if (config.isShutdownAfterCommand()) {
            Bukkit.shutdown();
        }
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
        // В Adventure 4.x затемнение экрана и музыка включаются флагами, а не сеттерами.
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

        // Страховка: убираем бар у всех, кто онлайн.
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.hideBossBar(bar);
        }
    }

    // ==================================================================
    //  Утилиты
    // ==================================================================

    /**
     * Плейсхолдеры для messages.yml: {@code %sender%}, {@code %time%},
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