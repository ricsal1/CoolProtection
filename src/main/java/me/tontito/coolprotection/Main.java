package me.tontito.coolprotection;

import me.tontito.coolprotection.Updater.UpdateChecker;
import me.tontito.coolprotection.Updater.UpdateCheckerBukkSpig;
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

        if (serverVersion == 2 || serverVersion == 3) {
            new UpdateCheckerBukkSpig(this);
        } else {
            new UpdateChecker(this);
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

        if (tpsProtection) {
            getLogger().info(ChatColor.GREEN + " Enabled and balanced for " + version);
        } else {
            getLogger().info(ChatColor.GREEN + " Enabled without TPS control, for " + version);
        }
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

        config.options().copyDefaults(true);
        saveConfig();

        LoadSettings();
    }


    public void LoadSettings() {
        FileConfiguration config = getConfig();

        try {
            ExplodeProtection = config.getBoolean("ExplodeProtection");
            WitherProtection = config.getBoolean("WitherProtection");
            WitherLevel = config.getInt("WitherLevel");

            autoShutdown = config.getBoolean("autoShutdown");
            autoShutDownCounterTime = config.getInt("autoShutDownCounterTime");
            autoShutDownTime = config.getInt("autoShutdownTime");

            maxSpeed = (float) config.getDouble("maxTravelSpeed");
            totalMaxChunkEntities = config.getInt("maxChunkEntities");
            ExplosionLevel = config.getInt("ExplosionLevel");

        } catch (Exception e) {
            getLogger().info("## Error loading configs, disabling protections!");
            ExplodeProtection = false;
            WitherProtection = false;
            autoShutdown = false;
            autoShutDownCounterTime = 10;
            autoShutDownTime = 0;
            totalMaxChunkEntities = 70;
            maxSpeed = (float) 1.2;
        }

        try {
            speedProtection = getConfig().getBoolean("speedProtection");
            hackProtection = getConfig().getBoolean("hackProtection");
        } catch (Exception e) {
            speedProtection = false;
            hackProtection = false;
        }

        try {
            maxRedstone = getConfig().getInt("maxRedstoneComponents");
            maxRedstoneChunk = getConfig().getInt("maxRedstoneChunkComponents");
        } catch (Exception e) {
            maxRedstone = 150;
            maxRedstoneChunk = 40;
        }

        try {
            DEFAULT_RESOURCE = config.getString("DEFAULT_RESOURCE", "");
            DEFAULT_RESOURCE_HASH = config.getString("DEFAULT_RESOURCE_HASH", "");
        } catch (Exception e) {
            DEFAULT_RESOURCE = "";
            DEFAULT_RESOURCE_HASH = "";
        }

        try {
            antiChatReport = getConfig().getBoolean("antiChatReport");
        } catch (Exception e) {
            antiChatReport = false;
        }

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

        try {
            tpsProtection = getConfig().getBoolean("tpsProtection");
        } catch (Exception e) {
            tpsProtection = false;
        }
    }


    protected void setWorldConfigs(int tmpChuck) {
        getServer().getWorld("world").setSpawnLimit(MONSTER, tmpChuck);
        getServer().getWorld("world").setGameRule(GameRule.MAX_ENTITY_CRAMMING, 24); //to fix previous mistake
    }


    protected void setStatus(String newStatus) {
        serverStatus = newStatus;
    }

}
