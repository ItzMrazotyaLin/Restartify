package org.itzmr.restartify.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.itzmr.restartify.Restartify;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Универсальный парсер текста.
 *
 * <p>Поддерживает ОДНОВРЕМЕННО два синтаксиса:</p>
 * <ul>
 *     <li>MiniMessage: {@code <red>}, {@code <bold>}, {@code <gradient:#ff0000:#0000ff>}, {@code <#RRGGBB>}</li>
 *     <li>Legacy: {@code &c}, {@code &l}, {@code &#RRGGBB}, {@code &x&f&f&0&0&0&0}</li>
 * </ul>
 *
 * <p>Идея: legacy-коды переводятся в открывающие MiniMessage-теги,
 * а закрывающие теги проставляются автоматически в конце строки (или при {@code &r}).
 * Благодаря этому итоговая строка всегда является корректным MiniMessage-документом
 * с правильной вложенностью тегов.</p>
 *
 * <p>Символы {@code <}, {@code >} и {@code &} в MiniMessage-синтаксисе не конфликтуют
 * между собой, поэтому смешивание в одной строке безопасно.</p>
 */
public final class MessageUtils {

    /**
     * Порядок альтернатив важен: hex ({@code &#RRGGBB}) проверяется раньше
     * одиночного символа, {@code &x&R&R&G&G&B&B} — раньше {@code &x}.
     */
    private static final Pattern LEGACY_PATTERN = Pattern.compile(
            "&(#[0-9a-fA-F]{6}|[xX](?:&[0-9a-fA-F]){6}|[0-9a-fA-FkK-lL-oOrR])"
    );

    /** Символ, который в конвертируемой строке означает «сбросить всё». */
    private static final String RESET_TAG = "reset";

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private MessageUtils() {
    }

    // ------------------------------------------------------------------
    //  Таблицы legacy-кодов
    // ------------------------------------------------------------------

    private static final Map<Character, String> LEGACY_TAGS = new HashMap<>(26);

    static {
        LEGACY_TAGS.put('0', "black");
        LEGACY_TAGS.put('1', "dark_blue");
        LEGACY_TAGS.put('2', "dark_green");
        LEGACY_TAGS.put('3', "dark_aqua");
        LEGACY_TAGS.put('4', "dark_red");
        LEGACY_TAGS.put('5', "dark_purple");
        LEGACY_TAGS.put('6', "gold");
        LEGACY_TAGS.put('7', "gray");
        LEGACY_TAGS.put('8', "dark_gray");
        LEGACY_TAGS.put('9', "blue");
        LEGACY_TAGS.put('a', "green");
        LEGACY_TAGS.put('b', "aqua");
        LEGACY_TAGS.put('c', "red");
        LEGACY_TAGS.put('d', "light_purple");
        LEGACY_TAGS.put('e', "yellow");
        LEGACY_TAGS.put('f', "white");

        LEGACY_TAGS.put('k', "obfuscated");
        LEGACY_TAGS.put('l', "bold");
        LEGACY_TAGS.put('m', "strikethrough");
        LEGACY_TAGS.put('n', "underlined");
        LEGACY_TAGS.put('o', "italic");

        LEGACY_TAGS.put('r', RESET_TAG);
    }

    // ------------------------------------------------------------------
    //  Основной API
    // ------------------------------------------------------------------

    /**
     * Преобразует строку в Adventure {@link Component},
     * поддерживая MiniMessage-теги и legacy-коды одновременно.
     *
     * @param input исходная строка (может быть {@code null})
     * @return готовый компонент
     */
    public static Component parse(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        return MINI_MESSAGE.deserialize(convertLegacyCodes(input));
    }

    /**
     * {@link #parse(String)} с подстановкой плейсхолдеров вида {@code %time%}.
     *
     * @param placeholders карта плейсхолдеров (может быть {@code null})
     */
    public static Component parse(String input, Map<String, String> placeholders) {
        return parse(applyPlaceholders(input, placeholders));
    }

    /**
     * {@link #parse(String)} с одним плейсхолдером.
     */
    public static Component parse(String input, String key, String value) {
        if (key == null) {
            return parse(input);
        }
        Map<String, String> map = new HashMap<>(2);
        map.put(key, value == null ? "" : value);
        return parse(input, map);
    }

    /**
     * {@link #parse(String)} с двумя плейсхолдерами.
     */
    public static Component parse(String input, String key1, String value1, String key2, String value2) {
        Map<String, String> map = new HashMap<>(4);
        if (key1 != null) {
            map.put(key1, value1 == null ? "" : value1);
        }
        if (key2 != null) {
            map.put(key2, value2 == null ? "" : value2);
        }
        return parse(input, map);
    }

    /**
     * Склеивает компоненты в один.
     */
    public static Component join(Component prefix, Component main) {
        if (prefix == null) {
            return main == null ? Component.empty() : main;
        }
        if (main == null) {
            return prefix;
        }
        return prefix.append(main);
    }

    /**
     * Заменяет плейсхолдеры в строке БЕЗ парсинга MiniMessage.
     * Используется, если нужно получить «сырую» строку (например, для лога).
     */
    public static String applyPlaceholders(String input, Map<String, String> placeholders) {
        if (input == null || input.isEmpty() || placeholders == null || placeholders.isEmpty()) {
            return input;
        }
        String result = input;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isEmpty()) {
                continue;
            }
            String value = entry.getValue() == null ? "" : entry.getValue();
            result = result.replace(key, value);
        }
        return result;
    }

    // ------------------------------------------------------------------
    //  Отправка сообщений
    // ------------------------------------------------------------------

    public static void send(CommandSender sender, Component component) {
        if (sender == null || component == null) {
            return;
        }
        sender.sendMessage(component);
    }

    /**
     * Отправляет сообщение всем игрокам онлайн.
     */
    public static void broadcast(Component component) {
        if (component == null) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(component);
        }
    }

    /**
     * Отправляет сообщение всем операторам и игрокам с правом
     * {@link Restartify#ADMIN_PERMISSION}, а также в консоль.
     */
    public static void sendToAdmins(Component component) {
        if (component == null) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isOp() || player.hasPermission(Restartify.ADMIN_PERMISSION)) {
                player.sendMessage(component);
            }
        }
        CommandSender console = Bukkit.getConsoleSender();
        if (console != null) {
            console.sendMessage(component);
        }
    }

    // ------------------------------------------------------------------
    //  Форматирование времени
    // ------------------------------------------------------------------

    /**
     * {@code 95 -> "01:35"}, {@code 3725 -> "1:02:05"}.
     */
    public static String formatDuration(int totalSeconds) {
        long seconds = Math.max(0L, totalSeconds);
        long hours = seconds / 3600L;
        long minutes = (seconds % 3600L) / 60L;
        long secs = seconds % 60L;

        if (hours > 0L) {
            return String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, secs);
        }
        return String.format(Locale.ROOT, "%02d:%02d", minutes, secs);
    }

    // ------------------------------------------------------------------
    //  Конвертация legacy-кодов
    // ------------------------------------------------------------------

    /**
     * Заменяет {@code &c}-коды на MiniMessage-теги, расставляя закрывающие теги.
     */
    private static String convertLegacyCodes(String input) {
        Matcher matcher = LEGACY_PATTERN.matcher(input);
        if (!matcher.find()) {
            return input;
        }

        StringBuilder result = new StringBuilder(input.length() + 32);
        List<String> openTags = new ArrayList<>(4);

        int cursor = 0;
        do {
            result.append(input, cursor, matcher.start());

            String tag = resolveTag(matcher.group(1));
            if (tag == null) {
                // Неизвестный код — оставляем как есть.
                result.append(matcher.group());
            } else if (RESET_TAG.equals(tag)) {
                // &r закрывает всё, что было открыто ранее.
                closeAll(result, openTags);
            } else {
                openTags.add(tag);
                result.append('<').append(tag).append('>');
            }

            cursor = matcher.end();
        } while (matcher.find());

        result.append(input, cursor, input.length());
        closeAll(result, openTags);
        return result.toString();
    }

    /**
     * Закрывает открытые теги в обратном порядке (соблюдая вложенность).
     */
    private static void closeAll(StringBuilder result, List<String> openTags) {
        for (int i = openTags.size() - 1; i >= 0; i--) {
            result.append("</").append(openTags.get(i)).append('>');
        }
        openTags.clear();
    }

    /**
     * @param code содержимое группы 1 (то, что после {@code &})
     * @return имя MiniMessage-тега или {@code null}, если код неизвестен
     */
    private static String resolveTag(String code) {
        if (code.length() == 7 && code.charAt(0) == '#') {
            return "#" + code.substring(1).toUpperCase(Locale.ROOT);
        }
        if (code.length() == 13) {
            // Формат &x&R&R&G&G&B&B: шестёрки лежат на чётных индексах (2, 4, 6, 8, 10, 12).
            StringBuilder hex = new StringBuilder(6);
            for (int i = 2; i < 13; i += 2) {
                hex.append(code.charAt(i));
            }
            return "#" + hex.toString().toUpperCase(Locale.ROOT);
        }
        return LEGACY_TAGS.get(Character.toLowerCase(code.charAt(0)));
    }
}