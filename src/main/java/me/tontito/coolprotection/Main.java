package me.tontito.coolprotection;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameRule;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Hashtable;

import static org.bukkit.entity.SpawnCategory.MONSTER;


public class Main extends JavaPlugin {

    private TpsCheck tps;
    private Listeners listen;

    protected int totalMaxChunkEntities = 70;
    protected int maxChunkEntities = 35;
    protected int maxEntities = 1600;
    protected int maxLiving = 800;
    protected String alert = "";
    protected float maxSpeed = 0;
    protected int tpsLevel = 0; //0 = normal,1 = alert, 2 = very low
    protected String serverStatus = null;
    private boolean autoShutdown = false;
    private int autoShutDownTime = 0;
    private int autoShutDownCounterTime = 15;
    protected int serverVersion = 0;
    protected boolean ExplodeProtection;
    protected boolean WitherProtection;
    protected int WitherLevel;
    protected int ExplosionLevel;
    protected boolean speedProtection;
    protected boolean hackProtection;
    protected int maxRedstone;
    protected int maxRedstoneChunk;
    protected String DEFAULT_RESOURCE;
    protected String DEFAULT_RESOURCE_HASH;
    public Hashtable playerControl = new Hashtable();


    public void onEnable() {
        String version = Bukkit.getVersion();

        if (version.contains("Paper")) {
            serverVersion = 1;
        } else if (version.contains("Bukkit")) {
            serverVersion = 2;
        } else if (version.contains("Spigot")) {
            serverVersion = 3;
        } else if (version.contains("Purpur")) {
            serverVersion = 4;
        } else if (version.contains("Pufferfish")) {
            serverVersion = 5;
        } else {
            getLogger().info("Server type not supported or tested! " + version);
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

        tps = new TpsCheck(this, autoShutdown, autoShutDownCounterTime, autoShutDownTime);
        listen = new Listeners(this, tps);

        if (serverVersion == 1 || serverVersion == 4 || serverVersion == 5) {
            getServer().getPluginManager().registerEvents(listen, this);
            getServer().getPluginManager().registerEvents(tps, this);
        } else {
            getServer().getPluginManager().registerEvents(listen, this);
        }
        getLogger().info(ChatColor.GREEN + " Enabled and balanced for " + version);
    }


    public void onDisable() {
        getLogger().info("Disabled!");
    }


    private void setupConfig() {
        FileConfiguration config = getConfig();

        if (!config.contains("ExplodeProtection")) {
            File dataFolder = getDataFolder();

            if (!dataFolder.exists()) {
                dataFolder.mkdir();
            }

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

        config.options().copyDefaults(true);
        saveConfig();

        LoadSettings();
    }


    private void LoadSettings() {
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

        if (totalMaxChunkEntities != getServer().getWorld("world").getSpawnLimit(MONSTER)) {
            int tmpChuck = totalMaxChunkEntities;

            if (totalMaxChunkEntities < 50 || totalMaxChunkEntities > 200)
                tmpChuck = 70; //game defaults 70 from bukkit.yml

            getLogger().info("SpawnLimit value changed from " + getServer().getWorld("world").getSpawnLimit(MONSTER) + " to " + tmpChuck);
            getServer().getWorld("world").setSpawnLimit(MONSTER, tmpChuck);
            getServer().getWorld("world").setGameRule(GameRule.MAX_ENTITY_CRAMMING, 24); //to fix previous mistake
        }
    }


    protected void setStatus(String newStatus) {
        serverStatus = newStatus;
    }


    public void print(String text) {
        getLogger().info(text);
    }

}
