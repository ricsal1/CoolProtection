package me.tontito.coolprotection;

import me.tontito.coolprotection.Updater.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameRule;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Hashtable;

import static org.bukkit.entity.SpawnCategory.MONSTER;

public class Main extends JavaPlugin {

    public TpsCheck tps;
    public int serverVersion = 0;
    public Hashtable<String, PlayerStatus> playerControl = new Hashtable();
    public Hashtable<Long, Long> chunkWater = new Hashtable();
    public MyBukkit myBukkit;
    protected int totalMaxChunkEntities = 70;
    protected int maxChunkEntities = 35;
    protected int maxEntities = 1600;
    protected int maxLiving = 800;
    protected String alert = "";
    protected float maxSpeed = 0;
    protected int tpsLevel = 0; //0 = normal, 1 = alert, 2 = very low
    protected String serverStatus = null;
    protected boolean tpsProtection;
    protected boolean ExplodeProtection;
    protected boolean WitherProtection;
    protected int WitherLevel;
    protected int ExplosionLevel;
    protected boolean dinamicHackProtection;
    protected boolean speedProtection;
    protected boolean hackProtection;
    protected boolean antiChatReport;
    protected int maxRedstone;
    protected int maxRedstoneChunk;
    protected String DEFAULT_RESOURCE;
    protected String DEFAULT_RESOURCE_HASH;
    private Listeners listen;
    protected boolean autoShutdown = false;
    protected int autoShutDownTime = 0;
    protected int autoShutDownCounterTime = 15;
    protected boolean AntigriefProtection = false;
    protected boolean Emergency = false;

    public void onEnable() {
        myBukkit = new MyBukkit(this);

        String versionnumber = Bukkit.getVersion().toUpperCase();
        String version = Bukkit.getServer().getName().toUpperCase();

        if (version.contains("PAPER")) {
            serverVersion = 1;
        } else if (version.contains("BUKKIT")) {
            serverVersion = 2;
        } else if (version.contains("SPIGOT")) {
            serverVersion = 3;
        } else if (version.contains("PURPUR")) {
            serverVersion = 4;
        } else if (version.contains("PUFFERFISH")) {
            serverVersion = 5;
        } else if (version.contains("-PETAL-")) {
            serverVersion = 6;
        } else if (version.contains("-SAKURA-")) {
            serverVersion = 7;
        } else if (myBukkit.isFolia()) {
            serverVersion = 8;

        } else {
            getLogger().info(ChatColor.RED + "Server type not supported or tested! " + version);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Utils.SetMain(this);
        setupConfig();

        tps = new TpsCheck(this);
        listen = new Listeners(this, tps);

        if (serverVersion == 1 || serverVersion == 4 || serverVersion == 5 || serverVersion == 6 || serverVersion == 7 || serverVersion == 8) {
            getServer().getPluginManager().registerEvents(listen, this);
            getServer().getPluginManager().registerEvents(tps, this);
        } else {
            getServer().getPluginManager().registerEvents(listen, this);
        }

        if (myBukkit.checkGreater("1.21.1", Bukkit.getServer().getBukkitVersion()) == -1) {
            getLogger().severe(" You are using a Minecraft Server version with possible problems of data loss and known exploits, get informed and evaluate updating to at least 1.21.1");
        }

        if (tpsProtection) {
            getLogger().info(ChatColor.GREEN + " Enabled and balanced for " + version + " v" + versionnumber + "     :::" + Bukkit.getServer().getBukkitVersion());
        } else {
            getLogger().info(ChatColor.GREEN + " Enabled without TPS control, for " + version);
        }

        try {
            myBukkit.runTaskLater(null, null, null, () -> new Metrics(this, 22317), 5);
        } catch (Exception e) {
            getLogger().info(ChatColor.RED + " Failed to register into Bstats");
        }

        myBukkit.UpdateChecker(true);
    }


    public void onDisable() {
        getLogger().info("Disabled!");
    }


    private void setupConfig() {
        FileConfiguration config = getConfig();
        File dataFolder = getDataFolder();

        if (!dataFolder.exists()) dataFolder.mkdir();

        if (!config.contains("ExplodeProtection")) {
            config.options().header("==== CoolProtection Configs ====");
            config.addDefault("ExplodeProtection", true);
            config.addDefault("WitherProtection", true);
            config.addDefault("WitherLevel", 35);
        }

        if (!config.contains("autoShutdown")) {
            config.addDefault("autoShutdown", false);
            config.addDefault("autoShutdownTime", 0);
            config.addDefault("autoShutDownCounterTime", 15);
            config.addDefault("maxTravelSpeed", 1.2);
            config.addDefault("maxChunkEntities", 70);
            config.addDefault("ExplosionLevel", -20);
        }

        if (!config.contains("speedProtection")) {
            config.addDefault("speedProtection", true);
        }

        if (!config.contains("hackProtection")) {
            config.addDefault("hackProtection", false);
        }

        if (!config.contains("maxRedstoneComponents")) {
            config.addDefault("maxRedstoneComponents", 150);
            config.addDefault("maxRedstoneChunkComponents", 40);
        }

        if (!config.contains("antiChatReport")) {
            config.addDefault("antiChatReport", false);
        }

        if (!config.contains("tpsProtection")) {
            config.addDefault("tpsProtection", true);
        }

        if (!config.contains("dinamicProtection")) {
            config.addDefault("dinamicHackProtection", true);
        }

        config.options().copyDefaults(true);
        saveConfig();

        LoadSettings();
    }


    public void LoadSettings() {
        FileConfiguration config = getConfig();

        ExplodeProtection = config.getBoolean("ExplodeProtection", false);
        WitherProtection = config.getBoolean("WitherProtection", false);
        WitherLevel = config.getInt("WitherLevel", 0);

        autoShutdown = config.getBoolean("autoShutdown", false);
        autoShutDownCounterTime = config.getInt("autoShutDownCounterTime", 10);
        autoShutDownTime = config.getInt("autoShutdownTime", 0);

        maxSpeed = (float) config.getDouble("maxTravelSpeed", 1.2f);
        totalMaxChunkEntities = config.getInt("maxChunkEntities", 70);
        ExplosionLevel = config.getInt("ExplosionLevel", 0);

        speedProtection = getConfig().getBoolean("speedProtection", false);
        hackProtection = getConfig().getBoolean("hackProtection", false);

        maxRedstone = getConfig().getInt("maxRedstoneComponents", 150);
        maxRedstoneChunk = getConfig().getInt("maxRedstoneChunkComponents", 40);


        DEFAULT_RESOURCE = config.getString("DEFAULT_RESOURCE", "");
        DEFAULT_RESOURCE_HASH = config.getString("DEFAULT_RESOURCE_HASH", "");

        antiChatReport = getConfig().getBoolean("antiChatReport", false);

        if (totalMaxChunkEntities != getServer().getWorld("world").getSpawnLimit(MONSTER)) {
            int tmpChuck = totalMaxChunkEntities;

            if (totalMaxChunkEntities < 10 || totalMaxChunkEntities > 200)
                tmpChuck = 70; //game defaults 70 from bukkit.yml

            getLogger().info("SpawnLimit value changed from " + getServer().getWorld("world").getSpawnLimit(MONSTER) + " to " + tmpChuck);

            if (serverVersion != 8) {
                getServer().getWorld("world").setSpawnLimit(MONSTER, tmpChuck);
                getServer().getWorld("world").setGameRule(GameRule.MAX_ENTITY_CRAMMING, 24); //to fix previous mistake
            } else {
                final int chunckLimit = tmpChuck;
                this.myBukkit.runTask(null, null, null, () -> this.setWorldConfigs(chunckLimit));
            }
        }

        tpsProtection = getConfig().getBoolean("tpsProtection", false);
        dinamicHackProtection = getConfig().getBoolean("dinamicHackProtection", false);
    }


    protected void setWorldConfigs(int tmpChuck) {
        getServer().getWorld("world").setSpawnLimit(MONSTER, tmpChuck);
        getServer().getWorld("world").setGameRule(GameRule.MAX_ENTITY_CRAMMING, 24); //to fix previous mistake
    }


    protected void setStatus(String newStatus) {
        serverStatus = newStatus;
    }

}
