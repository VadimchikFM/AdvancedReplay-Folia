package me.jumper251.replay.utils;

public class Platform {
    private static boolean isFolia = false;

    static {
        try {
            Class.forName("io.papermc.paper.threadedregions.commands.CommandServerHealth");
            isFolia = true;
        } catch (ClassNotFoundException ignored) {}
    }

    public static boolean isFolia() {
        return isFolia;
    }
}
