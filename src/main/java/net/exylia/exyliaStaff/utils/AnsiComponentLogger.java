package net.exylia.exyliaStaff.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.function.Function;
import java.util.regex.Pattern;

public class AnsiComponentLogger {
    private static final Pattern HEX_PATTERN = Pattern.compile(
            "§#([0-9a-fA-F]{6})"
    );

    private static final String RGB_ANSI = "\u001B[38;2;%d;%d;%dm";
    private static final String RESET = "\u001B[0m";

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.builder()
            .hexColors()
            .character('§')
            .hexCharacter('#')
            .build();

    private static final Function<Component, String> converter =
            supportsAnsi() ? AnsiComponentLogger::convertHexColors : AnsiComponentLogger::stripHexColors;

    /**
     * Converts a Component to an ANSI-colored string
     * @param component The input Component
     * @return Converted string
     */
    public static String convert(final Component component) {
        return converter.apply(component);
    }

    /**
     * Converts hex colors to ANSI RGB colors
     * @param input The input Component
     * @return String with ANSI RGB colors
     */
    static String convertHexColors(final Component input) {
        String serialized = SERIALIZER.serialize(input);
        return HEX_PATTERN.matcher(serialized).replaceAll(result -> {
            final int hex = Integer.decode("0x" + result.group().substring(2));
            final int red = hex >> 16 & 0xFF;
            final int green = hex >> 8 & 0xFF;
            final int blue = hex & 0xFF;
            return String.format(RGB_ANSI, red, green, blue);
        }) + RESET;
    }

    /**
     * Strips hex colors if ANSI is not supported
     * @param input The input Component
     * @return String with hex colors removed
     */
    private static String stripHexColors(final Component input) {
        String serialized = SERIALIZER.serialize(input);
        return HEX_PATTERN.matcher(serialized).replaceAll("");
    }

    /**
     * Checks if ANSI is supported in the current terminal
     * @return true if ANSI is supported, false otherwise
     */
    private static boolean supportsAnsi() {
        String osName = System.getProperty("os.name").toLowerCase();
        String term = System.getenv("TERM");

        // Basic ANSI support check
        return !osName.contains("win") ||
                (term != null && !term.equals("dumb"));
    }

    /**
     * Converts legacy color codes to ANSI colors
     * @param input The input Component
     * @return String with ANSI colors
     */
    public static String convertLegacyColors(final Component input) {
        String serialized = SERIALIZER.serialize(input);
        return convertLegacyColorCodes(serialized);
    }

    /**
     * Internal method to convert legacy color codes
     * @param input The input string with legacy color codes
     * @return String with ANSI colors
     */
    private static String convertLegacyColorCodes(String input) {
        // Map of legacy color codes to ANSI colors
        return input
                .replace("§0", "\u001B[30m")   // Black
                .replace("§1", "\u001B[34m")   // Dark Blue
                .replace("§2", "\u001B[32m")   // Dark Green
                .replace("§3", "\u001B[36m")   // Dark Aqua
                .replace("§4", "\u001B[31m")   // Dark Red
                .replace("§5", "\u001B[35m")   // Dark Purple
                .replace("§6", "\u001B[33m")   // Gold
                .replace("§7", "\u001B[37m")   // Gray
                .replace("§8", "\u001B[90m")   // Dark Gray
                .replace("§9", "\u001B[94m")   // Blue
                .replace("§a", "\u001B[92m")   // Green
                .replace("§b", "\u001B[96m")   // Aqua
                .replace("§c", "\u001B[91m")   // Red
                .replace("§d", "\u001B[95m")   // Light Purple
                .replace("§e", "\u001B[93m")   // Yellow
                .replace("§f", "\u001B[97m")   // White
                + RESET;
    }
}
