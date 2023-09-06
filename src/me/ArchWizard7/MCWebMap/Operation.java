package me.ArchWizard7.MCWebMap;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Operation implements Listener {
    /**
     * 128x128 ブロックをバイナリデータに変換してデータベースに登録します
     * @param player プレイヤー
     * @return データの登録に成功したら {@code true}、失敗したら {@code false} を返します
     */
    public boolean updateDatabase(Player player) {
        Location location = player.getLocation();

        int X = location.getBlockX();
        int Z = location.getBlockZ();

        int offsetX = X / 128; // 何番目の区間かを計算
        int offsetZ = Z / 128; // 何番目の区間かを計算

        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

        String environment = player.getWorld().getEnvironment().toString();
        String id = environment + "," + offsetX + "," + offsetZ;
        String data = get128x128ToString(player, offsetX, offsetZ); // 128x128 を文字列に変換
        String registered = player.getUniqueId().toString();
        String dateString = sdf.format(date);

        // REGISTER AREA //

        Config c = new Config();
        FileConfiguration config = c.getPluginConfig();

        String host     = config.getString("mysql.host", "localhost");
        int    port     = config.getInt("mysql.port", 3306);
        String db       = config.getString("mysql.db", "mcwebmap");
        String user     = config.getString("mysql.user", "root");
        String password = config.getString("mysql.password", "");

        /*
         * [ DEFAULT ]
         * jdbc:mysql://localhost:3306/mcwebmap
         */
        String url = "jdbc:mysql://" + host + ":" + port + "/" + db;

        try {
            Connection conn = DriverManager.getConnection(url, user, password);

            // 無ければ INSERT、既に id キーが存在するなら UPDATE
            String query = "INSERT INTO mcwebmap.chunks (id, data, registered, date) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE data = ?, registered = ?, date = ?";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, id);
            ps.setString(2, data);
            ps.setString(3, registered);
            ps.setString(4, dateString);
            ps.setString(5, data);
            ps.setString(6, registered);
            ps.setString(7, dateString);
            int rows = ps.executeUpdate();

            // メッセージ送信
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
            player.sendMessage("§a✔ 更新に成功しました！");
            player.sendMessage("§aAffected: §c§l" + rows);

            // 接続を閉じる
            ps.close();
            conn.close();
        } catch (SQLException e) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            player.sendMessage("§c✗ §lMySQL サーバーの接続に失敗しました");
            return false;
        }

        return true; // 成功したら true
    }

    /**
     * Web サーバー上の static/tiles を登録/更新します
     * @param player プレイヤー
     * @return HTTP 通信の GET リクエストの送信に成功したかどうか
     */
    public boolean updateTile(Player player) throws IOException {
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item.getType() != Material.FILLED_MAP) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            player.sendMessage("§c✗ メインハンドに §lFILLED_MAP§r§c をセットする必要があります");
            return false;
        }

        MapMeta meta = (MapMeta) item.getItemMeta();

        if (meta == null) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            player.sendMessage("§c✗ 地図情報の取得中にエラーが発生しました");
            return false;
        }

        MapView view = meta.getMapView();

        if (view == null) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            player.sendMessage("§c✗ An error has occurred in getView()");
            return false;
        }

        int id = view.getId();

        String path = "http://localhost:5000/register-tile?id=" + id + "&registered=" + player.getUniqueId();

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 2.0f);
        player.sendMessage("§e§oConnecting to §r§9§l§n" + path);

        URL url = new URL(path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        connection.connect(); // 接続
        connection.getResponseCode(); // 重要
        connection.disconnect(); // 切断

        player.playSound(player.getLocation(), Sound.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, 1.0f, 1.0f);
        player.sendMessage("§a地図の投稿に成功しました！");

        return true;
    }

    /**
     * データベース上にウェイポイントを追加します
     * @param name ウェイポイントの名前
     * @param player ウェイポイントの登録者
     * @param environment ウェイポイントの次元
     * @param x ウェイポイントの x 座標
     * @param y ウェイポイントの y 座標
     * @param z ウェイポイントの z 座標
     * @param icon ウェイポイントのアイコン
     * @return ウェイポイントの登録に成功したか
     */
    public boolean addWaypoint(String name, Player player, String environment, int x, int y, int z, String icon) {
        Date dateObject = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

        String registered = player.getUniqueId().toString();
        String date = sdf.format(dateObject);

        // REGISTER AREA //

        Config c = new Config();
        FileConfiguration config = c.getPluginConfig();

        String host     = config.getString("mysql.host", "localhost");
        int    port     = config.getInt("mysql.port", 3306);
        String db       = config.getString("mysql.db", "mcwebmap");
        String user     = config.getString("mysql.user", "root");
        String password = config.getString("mysql.password", "");

        /*
         * [ DEFAULT ]
         * jdbc:mysql://localhost:3306/mcwebmap
         */
        String url = "jdbc:mysql://" + host + ":" + port + "/" + db;

        try {
            Connection conn = DriverManager.getConnection(url, user, password);

            // 無ければ INSERT、既に id キーが存在するなら UPDATE
            String query = "INSERT INTO mcwebmap.waypoints (name, registered, date, environment, x, y, z, icon) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement ps = conn.prepareStatement(query);
            ps.setString(1, name);
            ps.setString(2, registered);
            ps.setString(3, date);
            ps.setString(4, environment);
            ps.setInt(5, x);
            ps.setInt(6, y);
            ps.setInt(7, z);
            ps.setString(8, icon);
            int rows = ps.executeUpdate();

            // メッセージ送信
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 2.0f);
            player.sendMessage("§a✔ ウェイポイントの登録に成功しました！");
            player.sendMessage("§aAffected: §c§l" + rows);

            // 接続を閉じる
            ps.close();
            conn.close();
        } catch (SQLException e) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.5f);
            player.sendMessage("§c✗ §lMySQL サーバーの接続に失敗しました");
            return false;
        }

        return true; // 成功したら true
    }


    /**
     * 128x128 区間を {@code String} に変換します
     * @param player 参照するプレイヤー
     * @param offsetX 128x128 に分割したとき、X軸は何番目か
     * @param offsetZ 128x128 に分割したとき、Z軸は何番目か
     * @return 文字列に変換したものを返します
     */
    public String get128x128ToString(Player player, int offsetX, int offsetZ) {
        StringBuilder result = new StringBuilder();

        World world = player.getWorld();

        int startX = offsetX * 128; // 開始地点 X
        int endX = startX + 127; // 終了地点 X
        int startZ = offsetZ * 128; // 開始地点 X
        int endZ = startZ + 127; // 終了地点 X

        for (int z = startZ; z <= endZ; z++) {
            for (int x = startX; x <= endX; x++) {
                Block highestBlock = world.getHighestBlockAt(x, z);
                result.append(blockToChar(highestBlock)); // ブロックから char に変換して append()
            }
        }

        return result.toString();
    }

    /**
     * ブロックの種類から @{code char} に変換します
     * 色分けについては <a href="https://minecraft.fandom.com/wiki/Map_item_format#Color_table">Map item format#Color Table</a> を参照
     * @return {@code 0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz} の62種類の {@code char} のいずれか
     */
    public char blockToChar(Block block) {
        Material m = block.getType();

        switch (m) {
            case GRASS_BLOCK,
                    SLIME_BLOCK -> {
                return '1';
            }
            case SAND,
                    BIRCH_PLANKS,
                    BIRCH_LOG,
                    BIRCH_WOOD,
                    STRIPPED_BIRCH_LOG,
                    STRIPPED_BIRCH_WOOD,
                    BIRCH_SIGN,
                    BIRCH_HANGING_SIGN,
                    BIRCH_WALL_SIGN,
                    BIRCH_WALL_HANGING_SIGN,
                    BIRCH_PRESSURE_PLATE,
                    BIRCH_TRAPDOOR,
                    BIRCH_STAIRS,
                    BIRCH_SLAB,
                    BIRCH_FENCE_GATE,
                    BIRCH_FENCE,
                    BIRCH_DOOR,
                    SANDSTONE,
                    SANDSTONE_STAIRS,
                    SANDSTONE_SLAB,
                    SANDSTONE_WALL,
                    CHISELED_SANDSTONE,
                    SMOOTH_SANDSTONE,
                    SMOOTH_SANDSTONE_STAIRS,
                    SMOOTH_SANDSTONE_SLAB,
                    CUT_SANDSTONE,
                    CUT_SANDSTONE_SLAB,
                    GLOWSTONE,
                    END_STONE,
                    END_STONE_BRICKS,
                    END_STONE_BRICK_STAIRS,
                    END_STONE_BRICK_SLAB,
                    END_STONE_BRICK_WALL,
                    BONE_BLOCK,
                    TURTLE_EGG,
                    SCAFFOLDING,
                    CANDLE,
                    OCHRE_FROGLIGHT -> {
                return '2';
            }
            case COBWEB,
                    MUSHROOM_STEM,
                    WHITE_CANDLE -> {
                return '3';
            }
            case LAVA,
                    TNT,
                    FIRE,
                    REDSTONE_BLOCK -> {
                return '4';
            }
            case ICE,
                    FROSTED_ICE,
                    PACKED_ICE,
                    BLUE_ICE -> {
                return '5';
            }
            case IRON_BLOCK,
                    IRON_DOOR,
                    BREWING_STAND,
                    HEAVY_WEIGHTED_PRESSURE_PLATE,
                    IRON_TRAPDOOR,
                    LANTERN,
                    ANVIL,
                    CHIPPED_ANVIL,
                    DAMAGED_ANVIL,
                    GRINDSTONE,
                    SOUL_LANTERN,
                    LODESTONE -> {
                return '6';
            }
            case OAK_SAPLING,
                    SPRUCE_SAPLING,
                    BIRCH_SAPLING,
                    JUNGLE_SAPLING,
                    ACACIA_SAPLING,
                    DARK_OAK_SAPLING,
                    CHERRY_SAPLING,
                    MANGROVE_PROPAGULE,
                    DANDELION,
                    POPPY,
                    BLUE_ORCHID,
                    ALLIUM,
                    AZURE_BLUET,
                    RED_TULIP,
                    ORANGE_TULIP,
                    WHITE_TULIP,
                    PINK_TULIP,
                    OXEYE_DAISY,
                    CORNFLOWER,
                    LILY_OF_THE_VALLEY,
                    TORCHFLOWER,
                    WITHER_ROSE,
                    PINK_PETALS,
                    SUNFLOWER,
                    LILAC,
                    ROSE_BUSH,
                    PEONY,
                    PITCHER_PLANT,
                    WHEAT,
                    SUGAR_CANE,
                    PUMPKIN_STEM,
                    MELON_STEM,
                    LILY_PAD,
                    COCOA,
                    CARROTS,
                    POTATOES,
                    BEETROOTS,
                    SWEET_BERRY_BUSH,
                    GRASS,
                    FERN,
                    VINE,
                    OAK_LEAVES,
                    SPRUCE_LEAVES,
                    BIRCH_LEAVES,
                    JUNGLE_LEAVES,
                    ACACIA_LEAVES,
                    DARK_OAK_LEAVES,
                    MANGROVE_LEAVES,
                    CHERRY_LEAVES,
                    AZALEA_LEAVES,
                    FLOWERING_AZALEA_LEAVES,
                    CACTUS,
                    BAMBOO,
                    CAVE_VINES,
                    SPORE_BLOSSOM,
                    AZALEA,
                    FLOWERING_AZALEA,
                    SMALL_DRIPLEAF,
                    BIG_DRIPLEAF -> {
                return '7';
            }
            case SNOW,
                    SNOW_BLOCK,
                    WHITE_BED,
                    WHITE_WOOL,
                    WHITE_STAINED_GLASS,
                    WHITE_CARPET,
                    WHITE_SHULKER_BOX,
                    WHITE_GLAZED_TERRACOTTA,
                    WHITE_CONCRETE,
                    WHITE_CONCRETE_POWDER,
                    POWDER_SNOW -> {
                return '8';
            }
            case CLAY,
                    INFESTED_STONE,
                    INFESTED_COBBLESTONE,
                    INFESTED_STONE_BRICKS,
                    INFESTED_MOSSY_STONE_BRICKS,
                    INFESTED_CRACKED_STONE_BRICKS,
                    INFESTED_CHISELED_STONE_BRICKS -> {
                return '9';
            }
            case DIRT,
                    COARSE_DIRT,
                    FARMLAND,
                    DIRT_PATH,
                    GRANITE,
                    GRANITE_STAIRS,
                    GRANITE_SLAB,
                    GRANITE_WALL,
                    POLISHED_GRANITE,
                    POLISHED_GRANITE_STAIRS,
                    POLISHED_GRANITE_SLAB,
                    JUNGLE_PLANKS,
                    JUNGLE_LOG,
                    JUNGLE_WOOD,
                    STRIPPED_JUNGLE_LOG,
                    STRIPPED_JUNGLE_WOOD,
                    JUNGLE_SIGN,
                    JUNGLE_HANGING_SIGN,
                    JUNGLE_WALL_SIGN,
                    JUNGLE_WALL_HANGING_SIGN,
                    JUNGLE_PRESSURE_PLATE,
                    JUNGLE_TRAPDOOR,
                    JUNGLE_STAIRS,
                    JUNGLE_SLAB,
                    JUNGLE_FENCE_GATE,
                    JUNGLE_FENCE,
                    JUNGLE_DOOR,
                    JUKEBOX,
                    BROWN_MUSHROOM_BLOCK,
                    ROOTED_DIRT,
                    HANGING_ROOTS,
                    PACKED_MUD -> {
                return 'A';
            }
            case STONE,
                    STONE_STAIRS,
                    STONE_SLAB,
                    ANDESITE,
                    ANDESITE_STAIRS,
                    ANDESITE_SLAB,
                    ANDESITE_WALL,
                    POLISHED_ANDESITE,
                    POLISHED_ANDESITE_STAIRS,
                    POLISHED_ANDESITE_SLAB,
                    COBBLESTONE,
                    COBBLESTONE_STAIRS,
                    COBBLESTONE_SLAB,
                    COBBLESTONE_WALL,
                    BEDROCK,
                    GOLD_ORE,
                    IRON_ORE,
                    COAL_ORE,
                    LAPIS_ORE,
                    DISPENSER,
                    MOSSY_COBBLESTONE,
                    MOSSY_COBBLESTONE_STAIRS,
                    MOSSY_COBBLESTONE_SLAB,
                    MOSSY_COBBLESTONE_WALL,
                    SPAWNER,
                    DIAMOND_ORE,
                    FURNACE,
                    STONE_PRESSURE_PLATE,
                    REDSTONE_ORE,
                    STONE_BRICKS,
                    STONE_BRICK_STAIRS,
                    STONE_BRICK_SLAB,
                    STONE_BRICK_WALL,
                    MOSSY_STONE_BRICKS,
                    MOSSY_STONE_BRICK_STAIRS,
                    MOSSY_STONE_BRICK_SLAB,
                    MOSSY_STONE_BRICK_WALL,
                    EMERALD_ORE,
                    ENDER_CHEST,
                    DROPPER,
                    SMOOTH_STONE,
                    SMOOTH_STONE_SLAB,
                    OBSERVER,
                    SMOKER,
                    BLAST_FURNACE,
                    STONECUTTER,
                    STICKY_PISTON,
                    PISTON,
                    PISTON_HEAD,
                    GRAVEL,
                    CAULDRON,
                    LAVA_CAULDRON,
                    POWDER_SNOW_CAULDRON,
                    HOPPER,
                    COPPER_ORE -> {
                return 'B';
            }
            case KELP_PLANT,
                    SEAGRASS,
                    WATER,
                    BUBBLE_COLUMN -> {
                return 'C';
            }
            case OAK_PLANKS,
                    OAK_LOG,
                    OAK_WOOD,
                    STRIPPED_OAK_LOG,
                    STRIPPED_OAK_WOOD,
                    OAK_SIGN,
                    OAK_HANGING_SIGN,
                    OAK_WALL_SIGN,
                    OAK_WALL_HANGING_SIGN,
                    OAK_DOOR,
                    OAK_PRESSURE_PLATE,
                    OAK_FENCE,
                    OAK_TRAPDOOR,
                    OAK_FENCE_GATE,
                    OAK_SLAB,
                    OAK_STAIRS,
                    NOTE_BLOCK,
                    BOOKSHELF,
                    CHEST,
                    CRAFTING_TABLE,
                    TRAPPED_CHEST,
                    DAYLIGHT_DETECTOR,
                    LOOM,
                    BARREL,
                    CARTOGRAPHY_TABLE,
                    FLETCHING_TABLE,
                    LECTERN,
                    SMITHING_TABLE,
                    COMPOSTER,
                    BAMBOO_SAPLING,
                    DEAD_BUSH,
                    PETRIFIED_OAK_SLAB,
                    BEEHIVE,
                    WHITE_BANNER,
                    LIGHT_GRAY_BANNER,
                    GRAY_BANNER,
                    BLACK_BANNER,
                    BROWN_BANNER,
                    RED_BANNER,
                    ORANGE_BANNER,
                    YELLOW_BANNER,
                    LIME_BANNER,
                    GREEN_BANNER,
                    CYAN_BANNER,
                    LIGHT_BLUE_BANNER,
                    BLUE_BANNER,
                    PURPLE_BANNER,
                    MAGENTA_BANNER,
                    PINK_BANNER -> {
                return 'D';
            }
            case DIORITE,
                    DIORITE_STAIRS,
                    DIORITE_SLAB,
                    DIORITE_WALL,
                    POLISHED_DIORITE,
                    POLISHED_DIORITE_STAIRS,
                    POLISHED_DIORITE_SLAB,
                    QUARTZ_BLOCK,
                    QUARTZ_STAIRS,
                    QUARTZ_SLAB,
                    CHISELED_QUARTZ_BLOCK,
                    QUARTZ_BRICKS,
                    QUARTZ_PILLAR,
                    SMOOTH_QUARTZ,
                    SMOOTH_QUARTZ_STAIRS,
                    SMOOTH_QUARTZ_SLAB,
                    SEA_LANTERN,
                    TARGET -> {
                return 'E';
            }
            case ACACIA_PLANKS,
                    ACACIA_LOG,
                    STRIPPED_ACACIA_LOG,
                    STRIPPED_ACACIA_WOOD,
                    ACACIA_SIGN,
                    ACACIA_TRAPDOOR,
                    ACACIA_STAIRS,
                    ACACIA_SLAB,
                    ACACIA_PRESSURE_PLATE,
                    ACACIA_FENCE_GATE,
                    ACACIA_FENCE,
                    ACACIA_DOOR,
                    RED_SAND,
                    ORANGE_WOOL,
                    ORANGE_CARPET,
                    ORANGE_SHULKER_BOX,
                    ORANGE_BED,
                    ORANGE_STAINED_GLASS,
                    ORANGE_GLAZED_TERRACOTTA,
                    ORANGE_CONCRETE,
                    ORANGE_CONCRETE_POWDER,
                    ORANGE_CANDLE,
                    PUMPKIN,
                    CARVED_PUMPKIN,
                    JACK_O_LANTERN,
                    TERRACOTTA,
                    RED_SANDSTONE,
                    RED_SANDSTONE_STAIRS,
                    RED_SANDSTONE_SLAB,
                    RED_SANDSTONE_WALL,
                    CHISELED_RED_SANDSTONE,
                    SMOOTH_RED_SANDSTONE,
                    SMOOTH_RED_SANDSTONE_STAIRS,
                    SMOOTH_RED_SANDSTONE_SLAB,
                    CUT_RED_SANDSTONE,
                    CUT_RED_SANDSTONE_SLAB,
                    HONEY_BLOCK,
                    HONEYCOMB_BLOCK,
                    COPPER_BLOCK,
                    CUT_COPPER,
                    CUT_COPPER_STAIRS,
                    CUT_COPPER_SLAB,
                    WAXED_COPPER_BLOCK,
                    WAXED_CUT_COPPER,
                    WAXED_CUT_COPPER_STAIRS,
                    WAXED_CUT_COPPER_SLAB,
                    LIGHTNING_ROD,
                    RAW_COPPER_BLOCK -> {
                return 'F';
            }
            case MAGENTA_WOOL,
                    MAGENTA_CARPET,
                    MAGENTA_SHULKER_BOX,
                    MAGENTA_BED,
                    MAGENTA_STAINED_GLASS,
                    MAGENTA_GLAZED_TERRACOTTA,
                    MAGENTA_CONCRETE,
                    MAGENTA_CONCRETE_POWDER,
                    MAGENTA_CANDLE,
                    PURPUR_BLOCK,
                    PURPUR_PILLAR,
                    PURPUR_STAIRS,
                    PURPUR_SLAB -> {
                return 'G';
            }
            case LIGHT_BLUE_WOOL,
                    LIGHT_BLUE_CARPET,
                    LIGHT_BLUE_SHULKER_BOX,
                    LIGHT_BLUE_BED,
                    LIGHT_BLUE_STAINED_GLASS,
                    LIGHT_BLUE_GLAZED_TERRACOTTA,
                    LIGHT_BLUE_CONCRETE,
                    LIGHT_BLUE_CONCRETE_POWDER,
                    LIGHT_BLUE_CANDLE,
                    SOUL_FIRE -> {
                return 'H';
            }
            case SPONGE,
                    WET_SPONGE,
                    YELLOW_WOOL,
                    YELLOW_CARPET,
                    YELLOW_SHULKER_BOX,
                    YELLOW_BED,
                    YELLOW_STAINED_GLASS,
                    YELLOW_GLAZED_TERRACOTTA,
                    YELLOW_CONCRETE,
                    YELLOW_CONCRETE_POWDER,
                    YELLOW_CANDLE,
                    HAY_BLOCK,
                    HORN_CORAL_BLOCK,
                    HORN_CORAL,
                    HORN_CORAL_FAN,
                    HORN_CORAL_WALL_FAN,
                    BEE_NEST -> {
                return 'I';
            }
            case LIME_WOOL,
                    LIME_CARPET,
                    LIME_SHULKER_BOX,
                    LIME_BED,
                    LIME_STAINED_GLASS,
                    LIME_GLAZED_TERRACOTTA,
                    LIME_CONCRETE,
                    LIME_CONCRETE_POWDER,
                    LIME_CANDLE,
                    MELON -> {
                return 'J';
            }
            case PINK_WOOL,
                    PINK_CARPET,
                    PINK_SHULKER_BOX,
                    PINK_BED,
                    PINK_STAINED_GLASS,
                    PINK_GLAZED_TERRACOTTA,
                    PINK_CONCRETE,
                    PINK_CONCRETE_POWDER,
                    PINK_CANDLE,
                    BRAIN_CORAL_BLOCK,
                    BRAIN_CORAL,
                    BRAIN_CORAL_FAN,
                    BRAIN_CORAL_WALL_FAN,
                    PEARLESCENT_FROGLIGHT -> {
                return 'K';
            }
            case ACACIA_WOOD,
                    GRAY_WOOL,
                    GRAY_CARPET,
                    GRAY_SHULKER_BOX,
                    GRAY_BED,
                    GRAY_STAINED_GLASS,
                    GRAY_GLAZED_TERRACOTTA,
                    GRAY_CONCRETE,
                    GRAY_CONCRETE_POWDER,
                    GRAY_CANDLE,
                    DEAD_TUBE_CORAL_BLOCK,
                    DEAD_TUBE_CORAL,
                    DEAD_TUBE_CORAL_FAN,
                    DEAD_TUBE_CORAL_WALL_FAN,
                    DEAD_BRAIN_CORAL_BLOCK,
                    DEAD_BRAIN_CORAL,
                    DEAD_BRAIN_CORAL_FAN,
                    DEAD_BRAIN_CORAL_WALL_FAN,
                    DEAD_BUBBLE_CORAL_BLOCK,
                    DEAD_BUBBLE_CORAL,
                    DEAD_BUBBLE_CORAL_FAN,
                    DEAD_BUBBLE_CORAL_WALL_FAN,
                    DEAD_FIRE_CORAL_BLOCK,
                    DEAD_FIRE_CORAL,
                    DEAD_FIRE_CORAL_FAN,
                    DEAD_FIRE_CORAL_WALL_FAN,
                    DEAD_HORN_CORAL_BLOCK,
                    DEAD_HORN_CORAL,
                    DEAD_HORN_CORAL_FAN,
                    DEAD_HORN_CORAL_WALL_FAN,
                    TINTED_GLASS -> {
                return 'L';
            }
            case LIGHT_GRAY_WOOL,
                    LIGHT_GRAY_CARPET,
                    LIGHT_GRAY_SHULKER_BOX,
                    LIGHT_GRAY_BED,
                    LIGHT_GRAY_STAINED_GLASS,
                    LIGHT_GRAY_GLAZED_TERRACOTTA,
                    LIGHT_GRAY_CONCRETE,
                    LIGHT_GRAY_CONCRETE_POWDER,
                    LIGHT_GRAY_CANDLE,
                    STRUCTURE_BLOCK,
                    JIGSAW -> {
                return 'M';
            }
            case CYAN_WOOL,
                    CYAN_CARPET,
                    CYAN_SHULKER_BOX,
                    CYAN_BED,
                    CYAN_STAINED_GLASS,
                    CYAN_GLAZED_TERRACOTTA,
                    CYAN_CONCRETE,
                    CYAN_CONCRETE_POWDER,
                    CYAN_CANDLE,
                    PRISMARINE,
                    PRISMARINE_STAIRS,
                    PRISMARINE_SLAB,
                    PRISMARINE_WALL,
                    WARPED_ROOTS,
                    WARPED_FUNGUS,
                    TWISTING_VINES,
                    NETHER_SPROUTS,
                    SCULK_SENSOR -> {
                return 'N';
            }
            case SHULKER_BOX,
                    PURPLE_WOOL,
                    PURPLE_CARPET,
                    PURPLE_BED,
                    PURPLE_STAINED_GLASS,
                    PURPLE_GLAZED_TERRACOTTA,
                    PURPLE_CONCRETE,
                    PURPLE_CONCRETE_POWDER,
                    PURPLE_CANDLE,
                    MYCELIUM,
                    CHORUS_PLANT,
                    CHORUS_FLOWER,
                    REPEATING_COMMAND_BLOCK,
                    BUBBLE_CORAL_BLOCK,
                    BUBBLE_CORAL,
                    BUBBLE_CORAL_FAN,
                    BUBBLE_CORAL_WALL_FAN,
                    AMETHYST_BLOCK,
                    BUDDING_AMETHYST,
                    AMETHYST_CLUSTER,
                    SMALL_AMETHYST_BUD,
                    MEDIUM_AMETHYST_BUD,
                    LARGE_AMETHYST_BUD -> {
                return 'O';
            }
            case BLUE_WOOL,
                    BLUE_CARPET,
                    BLUE_SHULKER_BOX,
                    BLUE_BED,
                    BLUE_STAINED_GLASS,
                    BLUE_GLAZED_TERRACOTTA,
                    BLUE_CONCRETE,
                    BLUE_CONCRETE_POWDER,
                    BLUE_CANDLE,
                    TUBE_CORAL_BLOCK,
                    TUBE_CORAL,
                    TUBE_CORAL_FAN,
                    TUBE_CORAL_WALL_FAN -> {
                return 'P';
            }
            case DARK_OAK_PLANKS,
                    DARK_OAK_LOG,
                    DARK_OAK_WOOD,
                    STRIPPED_DARK_OAK_LOG,
                    STRIPPED_DARK_OAK_WOOD,
                    DARK_OAK_SIGN,
                    DARK_OAK_HANGING_SIGN,
                    DARK_OAK_WALL_SIGN,
                    DARK_OAK_WALL_HANGING_SIGN,
                    DARK_OAK_PRESSURE_PLATE,
                    DARK_OAK_TRAPDOOR,
                    DARK_OAK_STAIRS,
                    DARK_OAK_SLAB,
                    DARK_OAK_FENCE_GATE,
                    DARK_OAK_FENCE,
                    DARK_OAK_DOOR,
                    BROWN_WOOL,
                    BROWN_CARPET,
                    BROWN_SHULKER_BOX,
                    BROWN_BED,
                    BROWN_STAINED_GLASS,
                    BROWN_GLAZED_TERRACOTTA,
                    BROWN_CONCRETE,
                    BROWN_CONCRETE_POWDER,
                    BROWN_CANDLE,
                    SOUL_SAND,
                    COMMAND_BLOCK,
                    BROWN_MUSHROOM,
                    SOUL_SOIL,
                    MUD_BRICK_SLAB -> {
                return 'Q';
            }
            case GREEN_WOOL,
                    GREEN_CARPET,
                    GREEN_SHULKER_BOX,
                    GREEN_BED,
                    GREEN_STAINED_GLASS,
                    GREEN_GLAZED_TERRACOTTA,
                    GREEN_CONCRETE,
                    GREEN_CONCRETE_POWDER,
                    GREEN_CANDLE,
                    END_PORTAL_FRAME,
                    CHAIN_COMMAND_BLOCK,
                    SEA_PICKLE,
                    MOSS_CARPET,
                    MOSS_BLOCK,
                    DRIED_KELP_BLOCK -> {
                return 'R';
            }
            case RED_WOOL,
                    RED_CARPET,
                    RED_SHULKER_BOX,
                    RED_BED,
                    RED_STAINED_GLASS,
                    RED_GLAZED_TERRACOTTA,
                    RED_CONCRETE,
                    RED_CONCRETE_POWDER,
                    RED_CANDLE,
                    BRICKS,
                    BRICK_STAIRS,
                    BRICK_SLAB,
                    BRICK_WALL,
                    RED_MUSHROOM_BLOCK,NETHER_WART,
                    ENCHANTING_TABLE,
                    NETHER_WART_BLOCK,
                    FIRE_CORAL_BLOCK,
                    FIRE_CORAL,
                    FIRE_CORAL_FAN,
                    FIRE_CORAL_WALL_FAN,
                    RED_MUSHROOM,
                    SHROOMLIGHT,
                    MANGROVE_PLANKS,
                    MANGROVE_LOG,
                    MANGROVE_WOOD,
                    STRIPPED_MANGROVE_LOG,
                    STRIPPED_MANGROVE_WOOD,
                    MANGROVE_SIGN,
                    MANGROVE_HANGING_SIGN,
                    MANGROVE_WALL_SIGN,
                    MANGROVE_WALL_HANGING_SIGN,
                    MANGROVE_DOOR,
                    MANGROVE_PRESSURE_PLATE,
                    MANGROVE_FENCE,
                    MANGROVE_TRAPDOOR,
                    MANGROVE_FENCE_GATE,
                    MANGROVE_SLAB,
                    MANGROVE_STAIRS -> {
                return 'S';
            }
            case BLACK_WOOL,
                    BLACK_CARPET,
                    BLACK_SHULKER_BOX,
                    BLACK_BED,
                    BLACK_STAINED_GLASS,
                    BLACK_GLAZED_TERRACOTTA,
                    BLACK_CONCRETE,
                    BLACK_CONCRETE_POWDER,
                    BLACK_CANDLE,
                    OBSIDIAN,
                    END_PORTAL,
                    DRAGON_EGG,
                    COAL_BLOCK,
                    END_GATEWAY,
                    BASALT,
                    POLISHED_BASALT,
                    SMOOTH_BASALT,
                    NETHERITE_BLOCK,
                    ANCIENT_DEBRIS,
                    CRYING_OBSIDIAN,
                    RESPAWN_ANCHOR,
                    BLACKSTONE,
                    GILDED_BLACKSTONE,
                    BLACKSTONE_STAIRS,
                    BLACKSTONE_SLAB,
                    BLACKSTONE_WALL,
                    CHISELED_POLISHED_BLACKSTONE,
                    POLISHED_BLACKSTONE,
                    POLISHED_BLACKSTONE_STAIRS,
                    POLISHED_BLACKSTONE_SLAB,
                    POLISHED_BLACKSTONE_WALL,
                    POLISHED_BLACKSTONE_PRESSURE_PLATE,
                    POLISHED_BLACKSTONE_BRICKS,
                    CRACKED_POLISHED_BLACKSTONE_BRICKS,
                    POLISHED_BLACKSTONE_BRICK_STAIRS,
                    POLISHED_BLACKSTONE_BRICK_SLAB,
                    POLISHED_BLACKSTONE_BRICK_WALL,
                    SCULK,
                    SCULK_VEIN,
                    SCULK_CATALYST,
                    SCULK_SHRIEKER -> {
                return 'T';
            }
            case GOLD_BLOCK,
                    LIGHT_WEIGHTED_PRESSURE_PLATE,
                    BELL,
                    RAW_GOLD_BLOCK -> {
                return 'U';
            }
            case DIAMOND_BLOCK,
                    BEACON,
                    PRISMARINE_BRICKS,
                    PRISMARINE_BRICK_STAIRS,
                    PRISMARINE_BRICK_SLAB,
                    DARK_PRISMARINE,
                    DARK_PRISMARINE_STAIRS,
                    DARK_PRISMARINE_SLAB,
                    CONDUIT -> {
                return 'V';
            }
            case LAPIS_BLOCK -> {
                return 'W';
            }
            case EMERALD_BLOCK -> {
                return 'X';
            }
            case PODZOL,
                    SPRUCE_PLANKS,
                    SPRUCE_LOG,
                    SPRUCE_WOOD,
                    STRIPPED_SPRUCE_LOG,
                    STRIPPED_SPRUCE_WOOD,
                    SPRUCE_PRESSURE_PLATE,
                    SPRUCE_TRAPDOOR,
                    SPRUCE_STAIRS,
                    SPRUCE_SLAB,
                    SPRUCE_FENCE_GATE,
                    SPRUCE_FENCE,
                    SPRUCE_DOOR,
                    CAMPFIRE,
                    SOUL_CAMPFIRE,
                    MANGROVE_ROOTS,
                    MUDDY_MANGROVE_ROOTS -> {
                return 'Y';
            }
            case NETHERRACK,
                    NETHER_BRICKS,
                    CRACKED_NETHER_BRICKS,
                    NETHER_BRICK_STAIRS,
                    NETHER_BRICK_SLAB,
                    NETHER_BRICK_WALL,
                    NETHER_BRICK_FENCE,
                    CHISELED_NETHER_BRICKS,
                    NETHER_GOLD_ORE,
                    NETHER_QUARTZ_ORE,
                    MAGMA_BLOCK,
                    RED_NETHER_BRICKS,
                    RED_NETHER_BRICK_STAIRS,
                    RED_NETHER_BRICK_SLAB,
                    RED_NETHER_BRICK_WALL,
                    CRIMSON_ROOTS,
                    CRIMSON_FUNGUS,
                    WEEPING_VINES -> {
                return 'Z';
            }
            case WHITE_TERRACOTTA,
                    CALCITE -> {
                return 'a';
            }
            case ORANGE_TERRACOTTA -> {
                return 'b';
            }
            case MAGENTA_TERRACOTTA -> {
                return 'c';
            }
            case LIGHT_BLUE_TERRACOTTA -> {
                return 'd';
            }
            case YELLOW_TERRACOTTA -> {
                return 'e';
            }
            case LIME_TERRACOTTA -> {
                return 'f';
            }
            case PINK_TERRACOTTA -> {
                return 'g';
            }
            case GRAY_TERRACOTTA,
                    TUFF -> {
                return 'h';
            }
            case LIGHT_GRAY_TERRACOTTA,
                    EXPOSED_COPPER,
                    EXPOSED_CUT_COPPER,
                    EXPOSED_CUT_COPPER_STAIRS,
                    EXPOSED_CUT_COPPER_SLAB,
                    WAXED_EXPOSED_COPPER,
                    WAXED_EXPOSED_CUT_COPPER,
                    WAXED_EXPOSED_CUT_COPPER_STAIRS,
                    WAXED_EXPOSED_CUT_COPPER_SLAB,
                    MUD_BRICKS,
                    MUD_BRICK_STAIRS,
                    MUD_BRICK_WALL -> {
                return 'i';
            }
            case CYAN_TERRACOTTA,
                    MUD -> {
                return 'j';
            }
            case PURPLE_TERRACOTTA,
                    PURPLE_SHULKER_BOX -> {
                return 'k';
            }
            case BLUE_TERRACOTTA -> {
                return 'l';
            }
            case BROWN_TERRACOTTA,
                    POINTED_DRIPSTONE,
                    DRIPSTONE_BLOCK -> {
                return 'm';
            }
            case GREEN_TERRACOTTA -> {
                return 'n';
            }
            case RED_TERRACOTTA,
                    DECORATED_POT -> {
                return 'o';
            }
            case BLACK_TERRACOTTA -> {
                return 'p';
            }
            case CRIMSON_NYLIUM -> {
                return 'q';
            }
            case CRIMSON_FENCE,
                    CRIMSON_FENCE_GATE,
                    CRIMSON_PLANKS,
                    CRIMSON_PRESSURE_PLATE,
                    CRIMSON_SIGN,
                    CRIMSON_HANGING_SIGN,
                    CRIMSON_WALL_SIGN,
                    CRIMSON_WALL_HANGING_SIGN,
                    CRIMSON_STAIRS,
                    CRIMSON_SLAB,
                    CRIMSON_STEM,
                    STRIPPED_CRIMSON_STEM,
                    CRIMSON_TRAPDOOR,
                    CRIMSON_DOOR -> {
                return 'r';
            }
            case CRIMSON_HYPHAE,
                    STRIPPED_CRIMSON_HYPHAE -> {
                return 's';
            }
            case WARPED_NYLIUM,
                    OXIDIZED_COPPER,
                    OXIDIZED_CUT_COPPER,
                    OXIDIZED_CUT_COPPER_STAIRS,
                    OXIDIZED_CUT_COPPER_SLAB,
                    WAXED_OXIDIZED_COPPER,
                    WAXED_OXIDIZED_CUT_COPPER,
                    WAXED_OXIDIZED_CUT_COPPER_STAIRS,
                    WAXED_OXIDIZED_CUT_COPPER_SLAB -> {
                return 't';
            }
            case WARPED_FENCE,
                    WARPED_FENCE_GATE,
                    WARPED_PLANKS,
                    WARPED_PRESSURE_PLATE,
                    WARPED_SIGN,
                    WARPED_HANGING_SIGN,
                    WARPED_WALL_SIGN,
                    WARPED_WALL_HANGING_SIGN,
                    WARPED_STAIRS,
                    WARPED_SLAB,
                    WARPED_STEM,
                    STRIPPED_WARPED_STEM,
                    WARPED_TRAPDOOR,
                    WARPED_DOOR,
                    WEATHERED_COPPER,
                    WEATHERED_CUT_COPPER,
                    WEATHERED_CUT_COPPER_STAIRS,
                    WEATHERED_CUT_COPPER_SLAB,
                    WAXED_WEATHERED_COPPER,
                    WAXED_WEATHERED_CUT_COPPER,
                    WAXED_WEATHERED_CUT_COPPER_STAIRS,
                    WAXED_WEATHERED_CUT_COPPER_SLAB -> {
                return 'u';
            }
            case WARPED_HYPHAE,
                    STRIPPED_WARPED_HYPHAE -> {
                return 'v';
            }
            case WARPED_WART_BLOCK -> {
                return 'w';
            }
            case DEEPSLATE_GOLD_ORE,
                    DEEPSLATE_IRON_ORE,
                    DEEPSLATE_COAL_ORE,
                    DEEPSLATE_LAPIS_ORE,
                    DEEPSLATE_DIAMOND_ORE,
                    DEEPSLATE_REDSTONE_ORE,
                    DEEPSLATE_EMERALD_ORE,
                    DEEPSLATE_COPPER_ORE,
                    DEEPSLATE,
                    COBBLED_DEEPSLATE,
                    COBBLED_DEEPSLATE_STAIRS,
                    COBBLED_DEEPSLATE_SLAB,
                    COBBLED_DEEPSLATE_WALL,
                    CHISELED_DEEPSLATE,
                    POLISHED_DEEPSLATE,
                    POLISHED_DEEPSLATE_STAIRS,
                    POLISHED_DEEPSLATE_SLAB,
                    POLISHED_DEEPSLATE_WALL,
                    DEEPSLATE_BRICKS,
                    CRACKED_DEEPSLATE_BRICKS,
                    DEEPSLATE_BRICK_STAIRS,
                    DEEPSLATE_BRICK_SLAB,
                    DEEPSLATE_BRICK_WALL,
                    DEEPSLATE_TILES,
                    CRACKED_DEEPSLATE_TILES,
                    DEEPSLATE_TILE_STAIRS,
                    DEEPSLATE_TILE_SLAB,
                    DEEPSLATE_TILE_WALL,
                    INFESTED_DEEPSLATE,
                    REINFORCED_DEEPSLATE -> {
                return 'x';
            }
            case RAW_IRON_BLOCK -> {
                return 'y';
            }
            case GLOW_LICHEN, VERDANT_FROGLIGHT -> {
                return 'z';
            }
        }

        return '0';
    }
}
