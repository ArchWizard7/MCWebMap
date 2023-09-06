package me.ArchWizard7.MCWebMap;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;

public class Config implements Listener {
    public void createConfig() throws IOException {
        Plugin MCWebMap = Bukkit.getServer().getPluginManager().getPlugin("MCWebMap");
        File folder = MCWebMap != null ? MCWebMap.getDataFolder() : null;
        assert folder != null;

        if (!folder.exists()) {
            if (!folder.mkdirs()) {
                System.out.println("'MCWebMap' folder mkdirs failed.");
                return;
            }
        }

        File configFile = new File(folder.getPath() + "/config.yml");

        if (!configFile.exists()) {
            if (configFile.createNewFile()) {
                FileConfiguration config = new YamlConfiguration();

                config.set("mysql.host", "localhost");
                config.set("mysql.port", 3306);
                config.set("mysql.db", "mcwebmap");
                config.set("mysql.user", "root");
                config.set("mysql.password", "");

                config.save(configFile);
            } else {
                System.out.println("File 'config.yml' creation failed.");
            }
        }
    }

    public FileConfiguration getPluginConfig() {
        Plugin MCWebMap = Bukkit.getServer().getPluginManager().getPlugin("MCWebMap");
        return MCWebMap != null ? MCWebMap.getConfig() : null;
    }
}
