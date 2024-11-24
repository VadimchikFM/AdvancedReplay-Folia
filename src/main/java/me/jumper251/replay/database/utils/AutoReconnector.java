package me.jumper251.replay.database.utils;


import me.jumper251.replay.database.DatabaseRegistry;
import me.jumper251.replay.database.MySQLDatabase;
import me.jumper251.replay.utils.Platform;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.TimeUnit;

public class AutoReconnector extends BukkitRunnable {

    protected Plugin plugin;

    public AutoReconnector(Plugin plugin) {
        this.plugin = plugin;
        if (Platform.isFolia())
            Bukkit.getAsyncScheduler().runAtFixedRate(plugin, (e) -> this.run(), 20 * 60 * 50, 20 * 60 * 50, TimeUnit.MILLISECONDS);
        else this.runTaskTimerAsynchronously(plugin, 20 * 60, 20 * 60);
    }

    @Override
    public void run() {
        MySQLDatabase database = (MySQLDatabase) DatabaseRegistry.getDatabase();
        database.update("USE `" + database.getDatabase() + "`");
    }

}
