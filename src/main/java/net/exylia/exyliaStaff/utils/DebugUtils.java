package net.exylia.exyliaStaff.utils;


import org.bukkit.Bukkit;

import static net.exylia.exyliaStaff.utils.AnsiComponentLogger.convertHexColors;


public class DebugUtils {

    public static final String PURPLE = "\u001B[35m";
    public static final String RESET = "\u001B[0m";

    private static final String prefix = "<#a89ab5>[<gradient:#aa76de:#8a51c4:#aa76de>ExyliaEvents</gradient><#a89ab5>] ";


    public static void logDebug(Boolean enabled, String message){
        if (!enabled) return;
        Bukkit.getConsoleSender().sendMessage(convertHexColors(ColorUtils.translateColors(prefix + "<#e7cfff>[DEBUG] " + message)));
    }
    public static void logError(String message){
        Bukkit.getConsoleSender().sendMessage(convertHexColors(ColorUtils.translateColors(prefix + "<#a33b53>[ERROR] " + message)));
    }
    public static void logWarn(String message){
        Bukkit.getConsoleSender().sendMessage(convertHexColors(ColorUtils.translateColors(prefix + "<#ffc58f>[WARN] " + message)));
    }
    public static void logInfo(String message){
        Bukkit.getConsoleSender().sendMessage(convertHexColors(ColorUtils.translateColors(prefix + "<#59a4ff>[INFO] " + message)));
    }

    public static void log(String message){
        Bukkit.getConsoleSender().sendMessage(convertHexColors(ColorUtils.translateColors(prefix + message)));
    }

    public static void sendMOTD() {
        log("<#8a51c4> ______            _ _       ______               _    __      _____  <reset>");
        log("<#8a51c4> |  ____|          | (_)     |  ____|             | |   \\ \\    / /__ \\ <reset>");
        log("<#8a51c4> | |__  __  ___   _| |_  __ _| |____   _____ _ __ | |_ __\\ \\  / /   ) |<reset>");
        log("<#8a51c4> |  __| \\ \\/ / | | | | |/ _` |  __\\ \\ / / _ \\ '_ \\| __/ __\\ \\/ /   / / <reset>");
        log("<#8a51c4> | |____ >  <| |_| | | | (_| | |___\\ V /  __/ | | | |_\\__ \\\\  /   / /_ <reset>");
        log("<#8a51c4> |______/_/\\_\\\\__, |_|_|\\__,_|______\\_/ \\___|_| |_|\\__|___/ \\/   |____|<reset>");
        log("<#8a51c4>               __/ |                                                   <reset>");
        log("<#8a51c4>              |___/                                                    <reset>");
    }
}



