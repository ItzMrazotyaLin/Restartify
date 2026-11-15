# Restartify

**Язык:** [English](README.md) | Русский

[![Paper](https://img.shields.io/badge/server-Paper%20%2F%20Purpur-4A4A4A?style=flat-square&logo=papermc)](https://papermc.io)
[![Java](https://img.shields.io/badge/java-17%2B-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://adoptium.net)
[![API](https://img.shields.io/badge/api-1.20%20--%2026.x%2B-4A4A4A?style=flat-square)](https://papermc.io)
[![Version](https://img.shields.io/badge/version-1.1.0-2ea44f?style=flat-square)](gradle.properties)

Плагин для **плавного перезапуска сервера** на Paper/Purpur (1.21+ и 1.20+): перед рестартом запускается точный обратный отсчёт с настраиваемыми оповещениями в чат, BossBar на экране, автоматической **блокировкой PvP** и **планировщиком авторестарта** по часовым поясам, а сам перезапуск выполняется нативным методом ядра.

> [!CAUTION]
> Этот плагин всё ещё находится на стадии тестирования, поэтому при работе могут возникать ошибки. Если вы столкнулись с багом, пожалуйста, сообщите о нём в [Issues](https://github.com/ItzMrazotyaLin/Restartify/issues)!

## Поддерживаемые версии

| Компонент | Версии |
|---|---|
| Серверные ядра | Paper / Purpur **1.20 – 26.x+** (рекомендуется **1.21+**) |
| Java (JDK) | **17+** (для сборки проекта) |
| `api-version` | `1.20` |

Плагин собран под Java 17 и использует только Adventure API, который уже входит в Paper, поэтому дополнительных библиотек на сервер класть не нужно.

## Описание

Обычный рестарт обрывает игру без предупреждения и может привести к потере ресурсов во время боя. Restartify решает эту проблему: администратор запускает `/reboot` (или настраивает расписание в `scheduled-restart`), все игроки онлайн видят обратный отсчёт в чате и в виде BossBar, а PvP автоматически блокируется до перезагрузки или отмены рестарта. По окончании таймера плагин перезапускает сервер нативным методом.

## Основные возможности (v1.1.0)

- **BossBar с плавным обратным отсчётом.** Прогресс плавно уменьшается от `1.0` до `0.0` по реальному времени (лаги сервера не растягивают таймер). Настраиваются цвет (`PINK`, `BLUE`, `RED`, `GREEN`, `YELLOW`, `PURPLE`, `WHITE`), стиль (`SOLID`, `NOTCHED_6/10/12/20`) и затемнение экрана (`darken-screen`). Игроки, зашедшие во время отсчёта, получают BossBar автоматически.
- **Защита от PvP во время отсчёта (`pvp-protection`).** При запуске ручного или запланированного отсчёта урон между игроками блокируется на приоритете `EventPriority.HIGHEST` поверх сторонних PvP-менеджеров и CombatLog/анти-релог плагинов, защищая игроков от потери вещей перед рестартом. Блокируются удары в ближнем бою, стрелы, трезубцы, зелья, TNT и кристаллы Энда (при этом собственный урон от жемчуга Края сохраняется), а сообщение о запрете защищено анти-спам кулдауном в 2 секунды. При `/reboot cancel` PvP сразу возвращается в обычный режим.
- **Авторестарт по расписанию и часовому поясу (`scheduled-restart`).** Асинхронный планировщик на базе `java.time.ZoneId` и `ZonedDateTime` запускает рестарт по заданному времени `HH:mm` в выбранном поясе IANA с учётом длительности отсчёта `countdown.seconds` (например, рестарт на `04:00` при отсчёте `60` с запустится ровно в `03:59:00`). При опечатке в названии зоны плагин выведет предупреждение в консоль и откатится на системный часовой пояс. Список зон см. в [TIMEZONES_RU.md](TIMEZONES_RU.md).
- **Оповещения в чат с настраиваемыми интервалами.** Три режима: `INTERVAL` (каждые N секунд), `SCHEDULE` (только на заданных секундах) и `OFF` (только BossBar). Флаг `chat.enabled: false` отключает чат-рассылку отсчёта, оставляя ответы на команды.
- **MiniMessage и legacy-коды одновременно.** Можно свободно смешивать `<red>`, `<bold>`, `<gradient:#ff0000:#0000ff>`, `<#RRGGBB>` и `&c`, `&l`, `&#RRGGBB` — закрывающие теги расставляются автоматически.
- **Встроенная локализация (`en` / `ru`).** По умолчанию используется английский язык (`messages.yml`); при выборе `locale: ru` в `config.yml` плагин автоматически выгружает и подключает русский файл сообщений (`messages_ru.yml`).
- **Оповещение админов, отмена и горячая перезагрузка.** Операторы и консоль видят инициатора запуска/отмены; `/reboot cancel` останавливает отсчёт и возвращает PvP; `/reboot reload` обновляет тексты, язык, настройки PvP и расписание без прерывания активного таймера.

## Команды и права

| Команда | Описание | Право |
|---|---|---|
| `/reboot` | Запустить таймер перезапуска | `restartify.admin` |
| `/reboot start` | То же, что `/reboot` (явная подкоманда) | `restartify.admin` |
| `/reboot cancel` | Отменить запущенный отсчёт и вернуть PvP | `restartify.admin` |
| `/reboot reload` | Перечитать `config.yml` и `messages.yml` | `restartify.admin` |
| `/rb` | Алиас `/reboot` | `restartify.admin` |

Единственное право — `restartify.admin`, по умолчанию выдаётся операторам (`default: op`). Например, через LuckPerms: `/lp user <ник> permission set restartify.admin true`.

> **Важно:** команда `/restart` принадлежит ядру (`org.spigotmc.RestartCommand`) и намеренно **не перехватывается** плагином, чтобы избежать зацикливания. Используйте `/reboot` — он сам вызывает нативный `Bukkit.spigot().restart()`.

## Локализация

Restartify поддерживает встроенную локализацию через параметр `locale` в файле `config.yml`:

| Код (`locale`) | Язык | Файл сообщений |
|---|---|---|
| `en` | Английский (по умолчанию) | `plugins/Restartify/messages.yml` |
| `ru` | Русский | `plugins/Restartify/messages_ru.yml` |

```yaml
# Plugin localization language. Available options: en, ru
locale: ru
```

При выборе `locale: ru` (при запуске сервера или после `/reboot reload`) плагин автоматически сгенерирует файл `plugins/Restartify/messages_ru.yml`, если его ещё нет, и загрузит сообщения из него. При указании неизвестного значения `locale` используется английский язык (`messages.yml`), а отсутствующие ключи подтягиваются из встроенных ресурсов JAR.

## Конфигурация

При первом запуске плагин создаёт `plugins/Restartify/config.yml` и `plugins/Restartify/messages.yml`.

### Пример `config.yml`

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
  # Часовой пояс в формате IANA (полный список см. в TIMEZONES_RU.md)
  timezone: "Europe/Moscow"
  # 24-часовой формат HH:mm
  times:
    - "04:00"
    - "16:00"
```

### Параметры `config.yml`

| Ключ | Описание |
|---|---|
| `locale` | Язык локализации плагина (`en` или `ru`). |
| `countdown.seconds` | Длительность отсчёта в секундах (минимум `1`). |
| `chat.enabled` | `false` — глушит сообщения отсчёта в чате, оставляя BossBar и ответы на команды. |
| `announce.mode` | `INTERVAL`, `SCHEDULE` или `OFF`. |
| `announce.interval-seconds` | Период для режима `INTERVAL`. |
| `announce.seconds` | Список секунд для режима `SCHEDULE`. |
| `bossbar.enabled` | Показывать ли BossBar. |
| `bossbar.color` | Цвет бара (`PINK`, `BLUE`, `RED`, `GREEN`, `YELLOW`, `PURPLE`, `WHITE`). |
| `bossbar.overlay` | Стиль бара (`SOLID`, `NOTCHED_6`, `NOTCHED_10`, `NOTCHED_12`, `NOTCHED_20`). |
| `bossbar.darken-screen` | Затемнять экран, пока виден бар. |
| `restart.action` | `RESTART` — нативный `Bukkit.spigot().restart()` (при отсутствии поддержки откат на `SHUTDOWN`); `SHUTDOWN` — `Bukkit.shutdown()`. |
| `restart.delay-ticks` | Задержка в тиках перед перезапуском (`20` = 1 секунда). |
| `pvp-protection.enabled` | Блокировать ли PvP между игроками во время обратного отсчёта. |
| `pvp-protection.deny-message` | Сообщение атакующему при блокировке PvP (кулдаун 2 секунды). |
| `scheduled-restart.enabled` | Включить ли автоматический рестарт по расписанию. |
| `scheduled-restart.timezone` | Часовой пояс IANA (`ZoneId`) — см. [TIMEZONES_RU.md](TIMEZONES_RU.md). |
| `scheduled-restart.times` | Список времени рестарта в 24-часовом формате `HH:mm`. |

### `messages.yml`

Здесь задаются все сообщения в чат и заголовок BossBar. Доступные плейсхолдеры:

| Плейсхолдер | Значение |
|---|---|
| `%sender%` | Кто запустил или отменил рестарт (ник игрока или `Console`). |
| `%time%` | Оставшееся время в формате `MM:SS` (или `H:MM:SS`). |
| `%seconds%` | Оставшееся время в секундах. |
| `%total%` | Длительность отсчёта в секундах. |
| `%progress%` | Сколько процентов отсчёта осталось. |
| `%error%` | Текст ошибки (только в `restart-fallback`). |

## Сборка проекта

Требуется установленный **JDK 17** или новее:

```bash
# Linux / macOS
./gradlew build

# Windows
gradlew.bat build
```

Готовые JAR-файлы появятся в `build/libs/`:

- `Restartify-<версия>.jar` — основной файл для загрузки на сервер;
- `Restartify-<версия>-shadow.jar` — shadow-сборка (все зависимости объявлены как `compileOnly` и предоставляются ядром).
