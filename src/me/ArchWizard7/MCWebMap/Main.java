package me.ArchWizard7.MCWebMap;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public class Main extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        // イベントリスナーの登録 //
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new Operation(), this);

        // コマンドのTab補完 //
        PluginCommand waypoint = getCommand("waypoint");
        assert waypoint != null;
        waypoint.setTabCompleter(new WaypointTabCompleter());

        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    @Override
    public void onLoad() {
        Config c = new Config();

        try {
            c.createConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }

        super.onLoad();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            if (command.getName().equalsIgnoreCase("mcwebmap")) {
                if (args.length == 0)
                    return sendHelpMessage(player);

                if (args[0].equalsIgnoreCase("check")) {
                    return true;
                }

                if (args[0].equalsIgnoreCase("config")) {
                    if (args.length == 3) {
                        FileConfiguration config = getConfig();
                        config.set(args[1], args[2]);
                        saveConfig();
                        reloadConfig();

                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                        player.sendMessage("§a✔ config.yml をセットしました！");
                        player.sendMessage("§aKEY§r §2§l'" + args[1] + "'§r ➤ §aVALUE §6§l'" + args[2] + "'");
                    } else {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                        player.sendMessage("§c✗ 引数が間違っています");
                        player.sendMessage("§cUsage: /mcwebmap config [KEY] [VALUE]");
                        return false;
                    }
                }

                if (args[0].equalsIgnoreCase("update")) {
                    Operation operation = new Operation();
                    try {
                        return operation.updateTile(player);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            if (command.getName().equalsIgnoreCase("waypoint")) {
                if (args.length < 4) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                    player.sendMessage("§c✗ 引数が正しくありません");
                    return false;
                } else {
                    try {
                        Operation o = new Operation();

                        String environment = args[0];
                        int x = Integer.parseInt(args[1]);
                        int y = Integer.parseInt(args[2]);
                        int z = Integer.parseInt(args[3]);

                        String icon = "default";
                        if (args.length >= 5) icon = args[4];

                        StringBuilder sb = new StringBuilder();

                        for (int i = 5; i < args.length; i++) {
                            if (i >= 6) sb.append(" ");
                            sb.append(args[i]);
                        }

                        String name = (args.length >= 6) ? sb.toString() : "NO NAME";

                        return o.addWaypoint(name, player, environment, x, y, z, icon);
                    } catch (NumberFormatException e) {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
                        player.sendMessage("§c✗ 座標は整数である必要があります");
                        return false;
                    }
                }
            }

            if (command.getName().equalsIgnoreCase("test-command")) {
                if (args.length == 0)
                    return testCommand(player);
            }
        }

        return super.onCommand(sender, command, label, args);
    }

    public static boolean testCommand(Player player) {
        for (int y = 0; y <= 383; y++) {
            int x = -62 + (2 * (y % 16));
            int z = -62 + (2 * (y / 16));

            Block block = player.getWorld().getBlockAt(x, (y - 63), z);
            block.setType(Material.GRASS_BLOCK);
        }

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.5f);
        player.sendMessage("§a✔ §lTEST COMMAND EXECUTION SUCCESSFUL!");

        return true;
    }

    /**
     * プレイヤーに対してヘルプメッセージを送信します
     * @param player ヘルプメッセージを送るプレイヤー
     * @return {@code player} が {@code null} でない限り、{@code true}
     */
    public static boolean sendHelpMessage(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);

        player.sendMessage("§d==========§r §f[§r §6§lMCWebMap§r §f]§r §d==========");
        player.sendMessage("§0");
        player.sendMessage("§b§oAuthor: §r§6§l§nArchWizard7");
        player.sendMessage("§b§oVersion: §r§6§l§n1.0");
        player.sendMessage("§0");
        player.sendMessage("§d==============================");

        return true;
    }
}
