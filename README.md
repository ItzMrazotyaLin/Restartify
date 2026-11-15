# Restartify

**Language:** English | [Русский](README_RU.md)

[![Paper](https://img.shields.io/badge/server-Paper%20%2F%20Purpur-4A4A4A?style=flat-square&logo=papermc)](https://papermc.io)
[![Java](https://img.shields.io/badge/java-17%2B-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://adoptium.net)
[![API](https://img.shields.io/badge/api-1.20%20--%2026.x%2B-4A4A4A?style=flat-square)](https://papermc.io)
[![Version](https://img.shields.io/badge/version-1.1.0-2ea44f?style=flat-square)](gradle.properties)

A plugin for **graceful server restarts** on Paper/Purpur (1.21+ and 1.20+): it runs a wall-clock countdown with configurable chat announcements, an on-screen BossBar, automatic **PvP protection** during the countdown, and **scheduled restarts** with IANA timezone support, then restarts the server via the core's native restart method.

> [!CAUTION]
> This plugin is still being tested, so you may run into issues while using it. If you find a bug,
> please report it in [Issues](https://github.com/ItzMrazotyaLin/Restartify/issues)!

## Supported Versions

| Component | Versions |
|---|---|
| Server software | Paper / Purpur **1.20 – 26.x+** (recommended **1.21+**) |
| Java (JDK) | **17+** |
| `api-version` | `1.20` |

The plugin is compiled for Java 17 and relies only on the Adventure API shipped with Paper — no external libraries are required.

## Description

An abrupt server restart cuts players off without warning and can cause item loss during combat. Restartify solves this: an administrator runs `/reboot` (or configures automatic times in `scheduled-restart`), everyone online sees a live countdown in chat and via BossBar, and PvP can be automatically disabled until the restart completes or is cancelled. When the timer reaches zero, the plugin restarts the server natively.

## Features (v1.1.0)

- **BossBar with a smooth countdown timer.** Progress moves smoothly from `1.0` down to `0.0` using wall-clock time (server lag does not stretch the countdown). Color (`PINK`, `BLUE`, `RED`, `GREEN`, `YELLOW`, `PURPLE`, `WHITE`), style (`SOLID`, `NOTCHED_6/10/12/20`), and `darken-screen` are configurable. Players joining mid-countdown see the BossBar automatically.
- **PvP Protection during countdown (`pvp-protection`).** When a manual or scheduled restart countdown starts, player-vs-player damage is blocked at `EventPriority.HIGHEST` — overriding third-party PvP managers and CombatLog/anti-relog plugins so players do not lose items right before a restart. Covers melee hits, arrows, tridents, splash potions, TNT, and End Crystals (while preserving self-damage such as Ender Pearls), with a 2-second anti-spam cooldown on the deny message. Cancelled restarts immediately restore normal PvP.
- **Scheduled Restarts with timezone support (`scheduled-restart`).** An asynchronous scheduler powered by `java.time.ZoneId` and `ZonedDateTime` triggers restarts at configured `HH:mm` times in your chosen IANA timezone, accounting for `countdown.seconds` (e.g., a `04:00` restart with a `60`-second countdown starts at `03:59:00`). Invalid timezones log a warning and safely fall back to `ZoneId.systemDefault()`. See [TIMEZONES.md](TIMEZONES.md) for valid timezone IDs.
- **Configurable chat announcements.** Three modes: `INTERVAL` (every N seconds), `SCHEDULE` (exact seconds list), and `OFF` (BossBar only). Setting `chat.enabled: false` silences countdown chat messages while keeping command replies active.
- **MiniMessage and legacy color codes together.** Mix `<red>`, `<bold>`, `<gradient:#ff0000:#0000ff>`, `<#RRGGBB>` with `&c`, `&l`, `&#RRGGBB` in any text — closing tags are inserted automatically.
- **Built-in localization (`en` / `ru`).** Defaults to English (`messages.yml`), with a built-in Russian translation (`messages_ru.yml`) activated via `locale: ru` in `config.yml`.
- **Admin notifications, cancel & hot-reload.** Admins and console see who started or cancelled the restart; `/reboot cancel` stops the countdown and re-enables PvP; `/reboot reload` updates texts, locale, PvP settings, and restart schedules on the fly without interrupting an active countdown.

## Commands & Permissions

| Command | Description | Permission |
|---|---|---|
| `/reboot` | Start the restart countdown | `restartify.admin` |
| `/reboot start` | Explicit subcommand to start the countdown | `restartify.admin` |
| `/reboot cancel` | Cancel a running countdown and restore PvP | `restartify.admin` |
| `/reboot reload` | Reload `config.yml` and `messages.yml` | `restartify.admin` |
| `/rb` | Alias for `/reboot` | `restartify.admin` |

The single permission `restartify.admin` defaults to operators (`default: op`). Example with LuckPerms: `/lp user <player> permission set restartify.admin true`.

> **Note:** `/restart` belongs to the server core (`org.spigotmc.RestartCommand`) and is deliberately **not** intercepted by Restartify to prevent restart command loops. Use `/reboot` instead — it invokes `Bukkit.spigot().restart()` directly.

## Localization

Restartify supports multiple built-in languages configured via `locale` in `config.yml`:

| Locale | Language | Messages File |
|---|---|---|
| `en` | English (default) | `plugins/Restartify/messages.yml` |
| `ru` | Russian | `plugins/Restartify/messages_ru.yml` |

```yaml
# Plugin localization language. Available options: en, ru
locale: en
```

When `locale: ru` is selected (at startup or on `/reboot reload`), Restartify automatically generates `plugins/Restartify/messages_ru.yml` if it does not exist yet and loads all messages from it. Any unknown `locale` value falls back to `en` (`messages.yml`), and missing keys fall back to the embedded defaults.

## Configuration

On first startup, the plugin creates `plugins/Restartify/config.yml` and `plugins/Restartify/messages.yml`.

### `config.yml` example

```yaml
# Plugin localization language. Available options: en, ru
locale: en

countdown:
  seconds: 60

chat:
  enabled: true

announce:
  mode: INTERVAL
  interval-seconds: 15
  seconds:
    - 60
    - 30
    - 15
    - 10
    - 5
    - 4
    - 3
    - 2
    - 1

bossbar:
  enabled: true
  color: RED
  overlay: SOLID
  darken-screen: true

restart:
  action: RESTART
  delay-ticks: 20

pvp-protection:
  enabled: true
  deny-message: "<red>PvP is disabled! The server is about to restart.</red>"

scheduled-restart:
  enabled: true
  # IANA timezone format (see TIMEZONES.md for the full list)
  timezone: "Europe/Moscow"
  # 24-hour HH:mm format
  times:
    - "04:00"
    - "16:00"
```

### `config.yml` keys

| Key | Description |
|---|---|
| `locale` | Plugin language (`en` or `ru`). |
| `countdown.seconds` | Countdown duration in seconds (minimum `1`). |
| `chat.enabled` | `false` silences countdown chat broadcasts (BossBar and command replies still work). |
| `announce.mode` | `INTERVAL`, `SCHEDULE`, or `OFF`. |
| `announce.interval-seconds` | Interval in seconds for `INTERVAL` mode. |
| `announce.seconds` | List of seconds for `SCHEDULE` mode. |
| `bossbar.enabled` | Whether to display the countdown BossBar. |
| `bossbar.color` | Bar color (`PINK`, `BLUE`, `RED`, `GREEN`, `YELLOW`, `PURPLE`, `WHITE`). |
| `bossbar.overlay` | Bar style (`SOLID`, `NOTCHED_6`, `NOTCHED_10`, `NOTCHED_12`, `NOTCHED_20`). |
| `bossbar.darken-screen` | Darken the screen while the BossBar is active. |
| `restart.action` | `RESTART` — native `Bukkit.spigot().restart()` (auto-falls back to `SHUTDOWN` if unsupported); `SHUTDOWN` — `Bukkit.shutdown()`. |
| `restart.delay-ticks` | Delay in ticks before executing restart/shutdown (`20` = 1 second). |
| `pvp-protection.enabled` | Disable PvP between players while a restart countdown is running. |
| `pvp-protection.deny-message` | Message sent to an attacker when PvP is blocked (2s anti-spam cooldown). |
| `scheduled-restart.enabled` | Enable automatic restarts according to `times`. |
| `scheduled-restart.timezone` | IANA `ZoneId` name — see [TIMEZONES.md](TIMEZONES.md). |
| `scheduled-restart.times` | List of target restart times in `HH:mm` (24-hour) format. |

### `messages.yml`

All chat messages and the BossBar title are configured in `messages.yml`. Available placeholders:

| Placeholder | Meaning |
|---|---|
| `%sender%` | Who started or cancelled the restart (player name or `Console`). |
| `%time%` | Remaining time formatted as `MM:SS` (or `H:MM:SS`). |
| `%seconds%` | Remaining time in seconds. |
| `%total%` | Total countdown duration in seconds. |
| `%progress%` | Percentage of countdown remaining. |
| `%error%` | Error message (used only in `restart-fallback`). |

## Building the Project

Requires **JDK 17** or newer:

```bash
# Linux / macOS
./gradlew build

# Windows
gradlew.bat build
```

Built JARs are placed in `build/libs/`:

- `Restartify-<version>.jar` — main plugin JAR;
- `Restartify-<version>-shadow.jar` — shadow JAR (all dependencies are `compileOnly` and provided by Paper).
