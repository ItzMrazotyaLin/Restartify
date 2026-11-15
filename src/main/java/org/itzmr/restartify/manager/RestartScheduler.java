package org.itzmr.restartify.manager;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.itzmr.restartify.Restartify;
import org.itzmr.restartify.config.ConfigManager;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Asynchronous periodic scheduler for automatic restarts based on configured times and timezone.
 */
public final class RestartScheduler {

    private static final long CHECK_INTERVAL_TICKS = 40L; // 2 seconds
    private static final long TRIGGER_WINDOW_SECONDS = 15L;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);

    private final Restartify plugin;
    private final ConfigManager config;
    private final RestartManager restartManager;

    private BukkitTask task;
    private volatile Instant startedAt = Instant.now();
    private volatile ZonedDateTime lastTriggeredMinute;

    public RestartScheduler(Restartify plugin, ConfigManager config, RestartManager restartManager) {
        this.plugin = plugin;
        this.config = config;
        this.restartManager = restartManager;
    }

    /**
     * Starts or restarts the asynchronous scheduler task using the current configuration.
     */
    public synchronized void startOrUpdate() {
        stop();

        if (!config.isScheduledRestartEnabled()) {
            return;
        }

        List<LocalTime> times = config.getScheduledRestartTimes();
        if (times.isEmpty()) {
            plugin.getLogger().warning("Restartify: scheduled-restart.enabled=true, but scheduled-restart.times is empty.");
            return;
        }

        startedAt = Instant.now();
        ZoneId zone = config.getScheduledRestartZone();
        String formattedTimes = times.stream()
                .map(TIME_FORMATTER::format)
                .collect(Collectors.joining(", "));
        plugin.getLogger().info("Scheduled restart enabled (timezone: " + zone.getId()
                + ", times: " + formattedTimes + ").");

        task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkSchedule, 20L, CHECK_INTERVAL_TICKS);
    }

    /**
     * Stops the scheduler task if running.
     */
    public synchronized void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void checkSchedule() {
        if (!config.isScheduledRestartEnabled()) {
            return;
        }

        List<LocalTime> times = config.getScheduledRestartTimes();
        if (times.isEmpty()) {
            return;
        }

        ZoneId zone = config.getScheduledRestartZone();
        int countdownSeconds = Math.max(1, config.getCountdownSeconds());
        ZonedDateTime now = ZonedDateTime.now(zone);
        LocalDate today = now.toLocalDate();

        // Account for midnight wrap-around when subtracting countdownSeconds shifts the trigger to the prior day.
        int maxDaysAhead = Math.max(1, (countdownSeconds / 86400) + 1);

        for (int dayOffset = 0; dayOffset <= maxDaysAhead; dayOffset++) {
            LocalDate targetDate = today.plusDays(dayOffset);
            for (LocalTime targetTime : times) {
                ZonedDateTime targetDateTime = ZonedDateTime.of(targetDate, targetTime, zone);
                ZonedDateTime triggerDateTime = targetDateTime.minusSeconds(countdownSeconds);

                if (now.isBefore(triggerDateTime) || !now.isBefore(triggerDateTime.plusSeconds(TRIGGER_WINDOW_SECONDS))) {
                    continue;
                }
                if (triggerDateTime.toInstant().isBefore(startedAt.minusSeconds(1))) {
                    continue;
                }

                ZonedDateTime triggerMinute = triggerDateTime.truncatedTo(ChronoUnit.MINUTES);
                if (triggerMinute.equals(lastTriggeredMinute)) {
                    continue;
                }

                lastTriggeredMinute = triggerMinute;

                if (!restartManager.isRunning()) {
                    plugin.getLogger().info("Scheduled restart for " + TIME_FORMATTER.format(targetTime)
                            + " (" + zone.getId() + ") — starting countdown (" + countdownSeconds + "s).");
                    Bukkit.getScheduler().runTask(plugin, restartManager::startScheduled);
                }
                return;
            }
        }
    }
}
