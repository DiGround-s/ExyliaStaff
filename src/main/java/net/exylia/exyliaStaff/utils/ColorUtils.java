package net.exylia.exyliaStaff.utils;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

import static net.exylia.exyliaStaff.utils.DebugUtils.logWarn;


public class ColorUtils {

    private static final Map<Character, String> COLOR_MAP;

    static {
        Map<Character, String> map = new HashMap<>();
        map.put('0', "<black>");
        map.put('1', "<dark_blue>");
        map.put('2', "<dark_green>");
        map.put('3', "<dark_aqua>");
        map.put('4', "<dark_red>");
        map.put('5', "<dark_purple>");
        map.put('6', "<gold>");
        map.put('7', "<gray>");
        map.put('8', "<dark_gray>");
        map.put('9', "<blue>");
        map.put('a', "<green>");
        map.put('b', "<aqua>");
        map.put('c', "<red>");
        map.put('d', "<light_purple>");
        map.put('e', "<yellow>");
        map.put('f', "<white>");
        map.put('k', "<obfuscated>");
        map.put('l', "<bold>");
        map.put('m', "<strikethrough>");
        map.put('n', "<underline>");
        map.put('o', "<italic>");
        map.put('r', "<reset>");
        COLOR_MAP = Map.copyOf(map);
    }

    public static Component translateColors(String message) {
        if (message == null || !message.contains("&")) {
            assert message != null;
            return MiniMessage.miniMessage().deserialize(message).decoration(TextDecoration.ITALIC, false);
        }

        StringBuilder builder = new StringBuilder(message.length() + 16);
        int length = message.length();

        for (int i = 0; i < length; i++) {
            char c = message.charAt(i);
            if (c == '&' && i + 1 < length) {
                char next = message.charAt(i + 1);
                String replacement = COLOR_MAP.get(next);
                if (replacement != null) {
                    builder.append(replacement);
                    i++;
                } else {
                    builder.append(c);
                }
            } else {
                builder.append(c);
            }
        }

        return MiniMessage.miniMessage().deserialize(builder.toString())
                .decoration(TextDecoration.ITALIC, false);
    }

    public static String normalizeColor(String input) {
        if (input == null || input.isBlank()) {
            return "<#ffffff>";
        }

        input = input.trim().toLowerCase();

        // Si ya está en formato MiniMessage <#ffffff>
        if (input.matches("<#[0-9a-f]{6}>")) {
            return input;
        }

        // Si es un código hexadecimal en varios formatos (ffffff, #ffffff, &#ffffff)
        if (input.matches("#?[0-9a-f]{6}") || input.matches("&#[0-9a-f]{6}")) {
            return "<#" + input.replace("#", "").replace("&", "") + ">";
        }

        // Si es un código de color con "&" (ej: &f)
        if (input.startsWith("&") && input.length() == 2) {
            String replacement = COLOR_MAP.get(input.charAt(1));
            if (replacement != null) {
                return replacement;
            }
        }

        logWarn("No se pudo normalizar el color: " + input + ". Usando color blanco por defecto.");
        return "<#ffffff>";
    }


    public static String oldTranslateColors(String message) {
        return ChatColor.translateAlternateColorCodes('&', GradientUtils.applyGradientsAndHex(message));
    }

    public static void sendPlayerMessage(Player player, String message) {
        Component component = ColorUtils.translateColors(message);
        player.sendMessage(component);
    }

    public static void sendSenderMessage(CommandSender sender, String message) {
        Component component = ColorUtils.translateColors(message);
        sender.sendMessage(component);
    }

    public static void showPlayerBossBar(Player player, BossBar bossBar) {
        player.showBossBar(bossBar);
    }

    public static void showPlayersBossBar(BossBar bossBar) {
        for (Player players : Bukkit.getOnlinePlayers()) {
            players.showBossBar(bossBar);
        }
    }

    public static void hidePlayerBossBar(Player player, BossBar bossBar) {
        player.hideBossBar(bossBar);
    }

    public static void hidePlayersBossBar(BossBar bossBar) {
        for (Player players : Bukkit.getOnlinePlayers()) {
            players.hideBossBar(bossBar);
        }
    }

    public static void sendPlayerTitle(Player player, Title title) {
        player.showTitle(title);
    }

    public static void sendBroadcastMessage(String message) {
        Component component = ColorUtils.translateColors(message);
        Bukkit.broadcast(component);
    }
}
