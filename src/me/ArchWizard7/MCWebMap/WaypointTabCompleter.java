package me.ArchWizard7.MCWebMap;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class WaypointTabCompleter implements TabCompleter {
    /**
     * /waypoint コマンドの {@link TabCompleter} です <br>
     * /waypoint &lt;add&gt; &lt;dimension&gt; &lt;x&gt; &lt;y&gt; &lt;z&gt;
     * @param sender コマンドの入力者
     * @param command コマンド
     * @param label ラベル
     * @param args 引数
     * @return 引数の {@link List<String>}
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions = new ArrayList<>();

        if (sender instanceof Player player) {
            Location location = player.getLocation();

            // 次元 //
            if (args.length == 1) {
                switch (player.getWorld().getEnvironment()) {
                    case NORMAL -> completions.add("overworld");
                    case NETHER -> completions.add("nether");
                    case THE_END -> completions.add("the_end");
                    case CUSTOM -> completions.add("custom");
                }
            }

            // X 座標 //
            if (args.length == 2)
                completions.add(Integer.toString(location.getBlockX()));

            // Y 座標 //
            if (args.length == 3)
                completions.add(Integer.toString(location.getBlockY()));

            // Z 座標 //
            if (args.length == 4)
                completions.add(Integer.toString(location.getBlockZ()));
        }

        return completions;
    }
}
