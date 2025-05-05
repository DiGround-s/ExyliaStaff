package net.exylia.exyliaStaff.utils;

import net.md_5.bungee.api.ChatColor;

import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GradientUtils {

    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<#([A-Fa-f0-9]{6})>(.*?)</#([A-Fa-f0-9]{6})>");
    private static final Pattern HEX_PATTERN = Pattern.compile("<#([A-Fa-f0-9]{6})>");
    private static final Pattern FORMAT_PATTERN = Pattern.compile("(&[klmnor])");

    public static String applyGradientsAndHex(String message) {
        message = applyHexColors(message);
        return applyGradients(message);
    }

    private static String applyHexColors(String message) {
        Matcher matcher = HEX_PATTERN.matcher(message);
        while (matcher.find()) {
            String hexCode = matcher.group(1);
            String hexColor = toChatColor("#" + hexCode);
            message = message.replace("<#" + hexCode + ">", hexColor);
        }
        return message;
    }

    private static String applyGradients(String message) {
        Matcher matcher = GRADIENT_PATTERN.matcher(message);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String startColor = matcher.group(1);
            String content = matcher.group(2);
            String endColor = matcher.group(3);

            String gradient = createGradient(content, Color.decode("#" + startColor), Color.decode("#" + endColor));
            matcher.appendReplacement(sb, gradient);
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private static String createGradient(String text, Color startColor, Color endColor) {
        StringBuilder builder = new StringBuilder();
        Matcher formatMatcher = FORMAT_PATTERN.matcher(text);

        int length = text.length();
        int formatEnd = 0;
        String currentFormat = "";

        for (int i = 0; i < length; i++) {
            while (formatMatcher.find() && formatMatcher.start() == i) {
                currentFormat += formatMatcher.group();
                formatEnd = formatMatcher.end();
            }

            double ratio = (double) i / (length - 1);
            int red = (int) (startColor.getRed() * (1 - ratio) + endColor.getRed() * ratio);
            int green = (int) (startColor.getGreen() * (1 - ratio) + endColor.getGreen() * ratio);
            int blue = (int) (startColor.getBlue() * (1 - ratio) + endColor.getBlue() * ratio);

            builder.append(toChatColor(String.format("#%02x%02x%02x", red, green, blue)));
            builder.append(currentFormat);

            if (i >= formatEnd) {
                builder.append(text.charAt(i));
            }
        }

        return builder.toString();
    }

    public static String toChatColor(String hex) {
        return ChatColor.of(hex).toString();
    }
}
