# Restartify

[![Paper](https://img.shields.io/badge/server-Paper%20%2F%20Purpur-4A4A4A?style=flat-square&logo=papermc)](https://papermc.io)
[![Java](https://img.shields.io/badge/java-17%2B-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://adoptium.net)
[![API](https://img.shields.io/badge/api-1.20%20--%2026.x%2B-4A4A4A?style=flat-square)](https://papermc.io)

[Read in Russian](README.md)

A plugin for **graceful server restarts**: it runs a countdown before the restart with configurable
chat announcements and an on-screen BossBar, then performs the restart through the server core's
own native restart method.

> [!CAUTION]
> This plugin is still being tested, so you may run into issues while using it. If you find a bug,
> please report it in [Issues](https://github.com/ItzMrazotyaLin/Restartify/issues)!

## Supported versions

| Component | Versions |
|---|---|
| Server software | Paper / Purpur **1.20 – 26.x+** |
| Java (JDK) | **17+** (for building the project) |
| `api-version` | `1.20` |

The plugin is compiled for Java 17 and relies only on the Adventure API, which already ships with
Paper — so there are no extra libraries to drop into your server.

## Description

A regular server restart cuts everyone's game short without any warning. Restartify fixes that: an
administrator runs `/reboot`, and everyone online sees a countdown — in chat (on a schedule or at a
fixed interval) and as a BossBar. When the timer reaches zero, the plugin restarts the server. You
can cancel the countdown at any moment, and any player who joins while it is already running will
automatically be shown the active BossBar.

## Features

- **BossBar with a live countdown timer.** Progress smoothly moves from `1.0` down to `0.0`, and both
  the color (`PINK`, `BLUE`, `RED`, `GREEN`, `YELLOW`, `PURPLE`, `WHITE`) and the style (`SOLID`,
  `NOTCHED_6/10/12/20`) are configurable. Players who join mid-countdown get the bar automatically,
  and it is properly removed when a player leaves or the plugin shuts down.
- **Chat alerts with configurable intervals.** Three modes: `INTERVAL` — every N seconds
  (60, 45, 30, 15 …), `SCHEDULE` — only at the exact seconds you list (60, 30, 15, 10, 5, 4, 3, 2, 1),
  and `OFF` — completely silent. On top of that, `chat.enabled: false` leaves players with nothing but
  the BossBar.
- **MiniMessage and legacy color codes at the same time.** You are free to mix `<red>`, `<bold>`,
  `<gradient:#ff0000:#0000ff>`, `<#RRGGBB>` and legacy codes such as `&c`, `&l`, `&#RRGGBB` in any
  message — the plugin inserts the closing tags for you, and `&r` resets the formatting.
- **Admin notifications.** Everyone with the permission or the OP status receives a message telling
  them **who** started the restart (the player's name or `Console`). The console sees it too.
- **Cancel at any time.** `/reboot cancel` stops the countdown, removes the BossBar from all players,
  and broadcasts a cancellation notice.
- **Hot-reloadable configuration.** `/reboot reload` re-reads both files without interrupting an
  already running countdown — new texts apply immediately.

## Commands & permissions

| Command | Description | Permission |
|---|---|---|
| `/reboot` | Start the restart countdown | `restartify.admin` |
| `/reboot start` | Same as `/reboot` (explicit subcommand) | `restartify.admin` |
| `/reboot cancel` | Cancel a running countdown | `restartify.admin` |
| `/reboot reload` | Re-read `config.yml` and `messages.yml` | `restartify.admin` |
| `/rb` | Alias for `/reboot` | `restartify.admin` |

There is a single permission, `restartify.admin`, granted to operators by default
(`default: op`). For example, with LuckPerms:
`/lp user <player> permission set restartify.admin true`.

> **Note:** the `/restart` command belongs to the server core (`org.spigotmc.RestartCommand`) and is
> deliberately **not** hijacked by this plugin. Hijacking it caused the restart to loop back into the
> plugin. Use `/reboot` instead — it calls the native `Bukkit.spigot().restart()` by itself.

## Configuration

On first launch the plugin creates a `plugins/Restartify/` folder containing two files.

### `config.yml` — behaviour

| Key | Description |
|---|---|
| `countdown.seconds` | Countdown length in seconds (minimum `1`). |
| `chat.enabled` | `false` silences countdown messages and leaves only the BossBar. Replies to commands keep working. |
| `announce.mode` | `INTERVAL`, `SCHEDULE` or `OFF`. |
| `announce.interval-seconds` | Period used by the `INTERVAL` mode. |
| `announce.seconds` | List of seconds used by the `SCHEDULE` mode. |
| `bossbar.enabled` | Whether to show the BossBar at all. |
| `bossbar.color` | Bar color. |
| `bossbar.overlay` | Bar style (`SOLID` = a solid fill). |
| `bossbar.darken-screen` | Darken the screen while the bar is visible. |
| `restart.action` | `RESTART` — native `Bukkit.spigot().restart()`; `SHUTDOWN` — `Bukkit.shutdown()`. If the core does not support restarting, the plugin automatically falls back to `SHUTDOWN`. |
| `restart.delay-ticks` | Delay in ticks before the restart (`20` = 1 second). |

### `messages.yml` — all texts

This file holds every chat message and the BossBar title. The following placeholders are available:

| Placeholder | Meaning |
|---|---|
| `%sender%` | Who started or cancelled the restart (player's name or `Console`). |
| `%time%` | Remaining time formatted as `MM:SS` (or `H:MM:SS`). |
| `%seconds%` | Remaining time in seconds. |
| `%total%` | Total countdown length in seconds. |
| `%progress%` | Percentage of the countdown still remaining. |
| `%error%` | Error text (used only in `restart-fallback`). |

Top-level keys: `prefix`, `no-permission`, `usage`, `started`, `announcement`, `already-running`,
`restarting`, `not-running`, `canceled`, `canceled-broadcast`, `reload-success`, `reload-failed`,
`restart-fallback`, plus the `bossbar.title` section. The prefix is only added to chat messages and
does not affect the BossBar (toggle it with `settings.prefix-enabled`).

## Building the project

You need JDK 17 or newer installed.

```bash
# Linux / macOS
./gradlew build

# Windows
gradlew.bat build
```

The built plugins end up in `build/libs/`:

- `Restartify-<version>.jar` — the main file to upload to your server;
- `Restartify-<version>-shadow.jar` — the same plugin (all dependencies are declared as `compileOnly`
  and are provided by the server, so package relocation is not applied).