package me.tontito.coolprotection;

import com.destroystokyo.paper.event.entity.PreCreatureSpawnEvent;
import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import dev.danablend.counterstrike.CounterStrike;
import fr.mrmicky.fastboard.FastBoard;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicInteger;


public class TpsCheck implements Listener {

    private final Hashtable<Player, String> playersWithScoreboard = new Hashtable<>();
    protected int redStoneObjs = 0;
    protected int lastRedStone = 0;
    protected Hashtable<Chunk, Integer> redStoneChunk = new Hashtable<>();
    protected Hashtable<Location, Integer> redStoneBlockComponents = new Hashtable<>();

    Main main;
    private LocalDateTime startShutTime;
    private int previousMinute = 0;
    private int lastSecond = 0;
    private int currSecond = 0;
    private int lastMinuteClean;
    private double average1, average2;
    private int lagDuration = 0;
    private String lastAlert = "";
    private int lastAlertCounter = 0;

    int[] tps = new int[61];//average tps for folia and full for others
    int[] regions = new int[61]; //region total by second
    long[][] ticks = new long[3][10]; //tick and region counter
    private long lastPoll = 0;
    private CounterStrike myCsmc;

    public TpsCheck(@NotNull Main main) {
        this.main = main;

        //for spigot and bukkit
        if (main.serverVersion == 2 || main.serverVersion == 3) {
            main.getServer().getScheduler().scheduleSyncRepeatingTask(
                    main, () -> {
                        lastPoll++;
                        updateTps(0, lastPoll);
                    }, 0,// waits 0 ticks
                    1 // 1 tick de interval (tick interno, 20 by second, if at least one is missing then we have problems)
            );
        }

        if (main.autoShutdown) {
            main.getLogger().info(" Auto Shutdown server is on! ");
        }

        try {
            Plugin test = main.getServer().getPluginManager().getPlugin("CSMC");
            if (test != null) myCsmc = ((dev.danablend.counterstrike.CounterStrike) test);
        } catch (NoClassDefFoundError e) {
        }

        main.getLogger().info("Checking for CSMC system availability: " + (myCsmc != null));

    }


    @EventHandler(ignoreCancelled = true)
    public void onpreCreatureSpawnEvent(@NotNull PreCreatureSpawnEvent event) {

        if (main.tpsProtection) {

            if (main.tpsLevel == 2 || lastTPS() <= 16) { //refusal mode for 1 second, if tps is lower
                main.tpsLevel = 2;
                event.setCancelled(true);
                main.alert = "TPS low, Pre Creature Spawn cancelled";
            }
        }
    }

    @EventHandler
    public void onTickEnd(@NotNull ServerTickEndEvent event) {

        updateTps(event.getTimeRemaining(), event.getTickNumber());
    }


    private void updateTps(long remainDuration, long tick) {

        LocalDateTime date = LocalDateTime.now();
        int seconds = date.toLocalTime().getSecond();

        synchronized (this) {
            int inc;

            if (remainDuration < 0) inc = -1;
            else inc = 1;

            tps[seconds] = tps[seconds] + inc;

            if (main.myBukkit.isFolia()) {

                int i = 0;
                long value = ticks[0][i];
                long maxSize = ticks[0].length;

                while (value != 0 && value != (tick - 1)) {
                    i++;

                    if (i >= maxSize) return;

                    value = ticks[0][i];
                }

                ticks[0][i] = tick;
                ticks[1][i] = (ticks[1][i] + inc);
            }

            //changed second
            if (currSecond != seconds) {

                lastSecond = currSecond;
                currSecond = seconds;

                //updates last second
                if (main.myBukkit.isFolia()) {

                    int maxRegions = ticks[0].length - 1;
                    int minRegions = 0;
                    String output = "";

                    //how many are set
                    while (minRegions <= maxRegions && ticks[0][minRegions] > 0) {
                        output = output + "Region " + (minRegions + 1) + ": " + ticks[1][minRegions] + "\n";
                        minRegions++;
                    }

                    if (minRegions > 0) {
                        regions[lastSecond] = minRegions;
                        tps[lastSecond] = tps[lastSecond] / regions[lastSecond];
                    }

                    //reset with small margin
                    ticks = new long[3][maxRegions + 4];

                    if (lastSecond == 59) {
                        for (int i = 0; i < 57; i++) {
                            regions[i] = 0;
                        }
                    } else regions[lastSecond + 1] = 0;
                }

                lastRedStone = redStoneObjs;
                redStoneObjs = 0;
                redStoneChunk.clear();
                redStoneBlockComponents.clear();

                if (seconds == 0) {
                    average2 = average1;
                    average1 = lastTPS();
                } else {
                    average1 = (average1 * seconds + lastTPS()) / (seconds + 1);
                }

                if (seconds == 59) {
                    for (int i = 0; i < 57; i++) {
                        tps[i] = 0;
                    }
                } else {
                    tps[seconds + 1] = 0;
                }

                if (main.tpsProtection) adjustRuntimeSettings(date, lastTPS(), lastSecond);

                if (main.autoShutdown) checkTurnOff(date);

                showLagToPlayers();

                if (lastSecond == 59) updatePlayerStatus();

                main.chunkWater.clear();
            }
        }
    }


    public int lastTPS() {
        return tps[lastSecond];
    }


    public int getCountRegions() {
        return regions[lastSecond];
    }


    private void adjustRuntimeSettings(LocalDateTime date, int mytps, int lastSecond) {

        Player player = null;

        for (Player pls : Bukkit.getOnlinePlayers()) {
            player = pls;

            if (main.myBukkit.isFolia() && main.myBukkit.isOwnedby(player, null, null)) {
                break;
            }
        }

        if (player == null) return;

        World world = player.getWorld();
        Location local = player.getLocation();

        AtomicInteger currentLiving = new AtomicInteger();// = world.getLivingEntities().size();
        AtomicInteger currentEntities = new AtomicInteger(); //= world.getEntities().size();


        if (main.myBukkit.isFolia()) {
            //can't check this
        } else {
            currentLiving.set(world.getLivingEntities().size());
            currentEntities.set(world.getEntities().size());
        }

        if (mytps >= 18) {
            main.tpsLevel = 0;
            lagDuration = 0;

            boolean changed = false;

            if (currentLiving.get() > main.maxLiving + 6) {
                main.maxLiving = currentLiving.get();
                changed = true;
            }

            if (currentEntities.get() > main.maxEntities + 6) {
                main.maxEntities = currentEntities.get();
                changed = true;
            }

            if (main.maxChunkEntities > 10 && main.myBukkit.isOwnedby(null, local, null)) {
                int currentChunkEntities = local.getChunk().getEntities().length;

                if (currentChunkEntities > main.maxChunkEntities + 3 && currentChunkEntities < main.totalMaxChunkEntities) {
                    if (currentChunkEntities < 20) {
                        main.maxChunkEntities = currentChunkEntities;
                    } else {
                        main.maxChunkEntities = main.maxChunkEntities + 3; //slower growth
                    }
                    changed = true;
                }
            }

            if (changed)
                Utils.logToFile("Protection Manager", "TPS " + lastTPS() + " Increased: Max LivingEntities " + main.maxLiving + "  maxEntities " + main.maxEntities + "  maxChunkEntities " + main.maxChunkEntities);

        } else if (mytps > 14) {

            main.tpsLevel = 1;
            lagDuration = 0;
            boolean changed = false;

            if (main.maxLiving > 900) {
                main.maxLiving = main.maxLiving - 5;
                changed = true;
            } else {
                main.maxLiving = main.maxLiving - 1;
            }

            if (main.maxEntities > 1800) {
                main.maxEntities = main.maxEntities - 10;
                changed = true;
            } else {
                main.maxEntities = main.maxEntities - 1;
            }

            if (main.maxChunkEntities > 10) {
                if (main.maxChunkEntities > 60) {
                    main.maxChunkEntities = main.maxChunkEntities - 3;
                    changed = true;
                } else {
                    main.maxChunkEntities = main.maxChunkEntities - 1;
                }
            }

            if (changed)
                Utils.logToFile("Protection Manager", "TPS " + lastTPS() + " Adjusted: Max LivingEntities " + main.maxLiving + "  maxEntities " + main.maxEntities + "  maxChunkEntities " + main.maxChunkEntities);

        } else {

            if (main.maxChunkEntities > 10 && lagDuration > 2) {
                main.maxLiving = main.maxLiving - 10;
                main.maxEntities = main.maxEntities - 10;
                main.maxChunkEntities = main.maxChunkEntities - 5;

                Utils.logToFile("Protection Manager", "TPS " + lastTPS() + " Emergency reduction: Max LivingEntities " + main.maxLiving + "  maxEntities " + main.maxEntities + "  maxChunkEntities " + main.maxChunkEntities);
                lagDuration = -2;
            }
            lagDuration++;

            emergencyClean(date);
        }
    }


    private void checkTurnOff(@NotNull LocalDateTime date) {

        int minute = date.toLocalTime().getMinute();
        int hours = date.toLocalTime().getHour();

        if (main.autoShutdown && (main.autoShutDownTime <= 6 && hours >= main.autoShutDownTime && hours <= 6)) {

            if (main.getServer().getOnlinePlayers().isEmpty()) {

                if (startShutTime == null) {
                    startShutTime = date;
                    return;
                }

                long limit = main.autoShutDownCounterTime - ChronoUnit.MINUTES.between(startShutTime, date);

                if (limit == 0) {
                    main.getLogger().info("Shutdown server!");

                    Runtime r = Runtime.getRuntime();

                    try {
                        r.exec("shutdown -s -t 40");
                    } catch (Exception e) {
                        main.getLogger().info("Server got error turning off windows " + e.getMessage());
                    }

                    main.getServer().shutdown();
                } else {

                    if (previousMinute != minute) {
                        main.setStatus("Server will turn off in " + limit + "m.");
                        main.getLogger().info("Server will turn off in " + limit + "m.");
                        previousMinute = minute;
                    }
                }
            } else {
                if (startShutTime != null) {
                    main.setStatus("Server is online!");
                    startShutTime = null;
                }
            }
        }

    }


    private void emergencyClean(@NotNull LocalDateTime date) {

        if (!main.tpsProtection) return;

        int tmpMinute = date.toLocalTime().getMinute();
        if (lastMinuteClean == tmpMinute) return; //apenas a cada minuto

        long millis = System.currentTimeMillis();
        AtomicInteger counter = new AtomicInteger();
        lastMinuteClean = tmpMinute;

        //can't check world, so will check next to players
        if (main.myBukkit.isFolia()) {
            Hashtable cacheHash = new Hashtable();

            for (Player pls : Bukkit.getOnlinePlayers()) {
                cacheHash.put(pls, 1);

                Location local = pls.getLocation();

                main.myBukkit.runTask(null, local, null, () -> {
                    for (Entity entity : local.getNearbyEntities(200, 200, 200)) {

                        if (entity instanceof Item && entity.isOnGround()) {
                            Item item = (Item) entity;

                            if (cacheHash.get(item) != null) {
                                main.getLogger().info("already processed :" + item);
                                continue;
                            }

                            cacheHash.put(item, 1);

                            if (item.getTicksLived() > 3000 && item.getItemStack().getEnchantments().isEmpty()) {

                                main.getLogger().info("ticks droped: " + item.getTicksLived() + " type: " + item.getName() + " in world: " + local.getWorld().getName() + " amount: " + item.getItemStack().getAmount());
                                counter.set(counter.get() + item.getItemStack().getAmount());
                                item.remove();
                            }
                        }
                    }
                });
            }
        } else {
            for (World world : main.getServer().getWorlds()) {

                for (Entity entity : world.getEntities()) {

                    if (entity instanceof Item && entity.isOnGround()) {
                        Item item = (Item) entity;

                        if (item.getTicksLived() > 3000 && item.getItemStack().getEnchantments().isEmpty()) {
                            main.getLogger().info("ticks droped: " + item.getTicksLived() + " type: " + item.getName() + " in world: " + world.getName() + " amount: " + item.getItemStack().getAmount());

                            counter.set(counter.get() + item.getItemStack().getAmount());

                            main.myBukkit.runTask(null, null, entity, () -> {
                                item.remove();
                            });
                        }
                    }
                }

            }
        }

        if (counter.get() > 0) {
            main.getServer().broadcastMessage("Removed " + counter + " droped items " + (System.currentTimeMillis() - millis) + "ms");
            main.getLogger().info("tps: " + lastTPS() + "  Removed " + counter + " droped items " + (System.currentTimeMillis() - millis) + "ms");
        }
    }


    public void registerScoreBoards(Player player) {
        if (playersWithScoreboard.get(player) == null) {
            playersWithScoreboard.put(player, "-1");
        }
    }


    private void showLagToPlayers() {

        if (main.alert.length() > 0 && lastAlertCounter > 4 && lastAlert.equals(main.alert)) {
            main.alert = "";
            lastAlert = "";
            lastAlertCounter = 0;
        } else if (main.alert.length() > 0 && lastAlertCounter <= 4 && lastAlert.equals(main.alert)) {
            lastAlertCounter++;
        } else if (main.alert.length() > 0) { //and alert not equal
            lastAlert = main.alert;
            lastAlertCounter = 0;
        }

        if (playersWithScoreboard.isEmpty()) return;

        Enumeration<Player> e = playersWithScoreboard.keys();

        while (e.hasMoreElements()) {

            Player player = e.nextElement();

            if (playersWithScoreboard.get(player).equals("-1")) {

                main.myBukkit.runTask(player, null, null, () -> setScoreBoardNew(player));
                playersWithScoreboard.replace(player, "1");
            }

            main.myBukkit.runTask(player, null, null, () -> updateNewScoreBoard(player));
        }
    }


    private void updatePlayerStatus() {

        if (!main.hackProtection && !main.speedProtection) return;

        if (main.playerControl == null || main.playerControl.isEmpty()) return;

        for (Player player : Bukkit.getOnlinePlayers()) {

            if (myCsmc != null && myCsmc.getCSPlayer(player, false, null) != null) {
                Utils.logToFile("Protection Manager", player.getName() + " is playing cs");
                continue;
            }

            PlayerStatus p = main.playerControl.get(player.getUniqueId().toString());

            if (p != null) {
                //for players with penalties
                if (main.hackProtection && p.speed < 0.2) {
                    p.speed = p.speed + 0.01f;
                    player.setWalkSpeed(p.speed);
                }
                //for players with wrong speed
                else if (main.speedProtection && player.getWalkSpeed() > (float) 0.2 && player.getActivePotionEffects().isEmpty()) {
                    player.setWalkSpeed((float) 0.2);
                    Utils.logToFile("Protection Manager", player.getName() + " got its walk speed adjusted");
                    main.alert = player.getName() + " got its walk speed reset";
                    //max recommended 0.335
                }
            }
        }
    }


    public void setScoreBoardNew(Player player) {

        if (!player.isOnline()) return;

        FastBoard board = new FastBoard(player);
        board.updateTitle(ChatColor.BOLD + "----Lagmeter " + main.getDescription().getVersion() + "----");
        PlayerStatus pls = main.playerControl.get(player.getUniqueId().toString());
        pls.setBoard(board);
    }


    public void updateNewScoreBoard(Player player) {

        PlayerStatus pls = main.playerControl.get(player.getUniqueId().toString());

        if (pls.returnBoard() == null) return;

        int currentLiving = player.getWorld().getLivingEntities().size();
        int currentEntities = player.getWorld().getEntities().size();
        int currentChunkEntities = player.getLocation().getChunk().getEntities().length;
        int currentChunkTileEntities = player.getLocation().getChunk().getTileEntities().length;

        int nearbyLivingEntities = 0;
        int nearbyEntities = 0;

        if (main.serverVersion == 2 || main.serverVersion == 3) {
            for (Object entity : player.getWorld().getNearbyEntities(player.getLocation(), 30, 200, 30)) {
                if (entity instanceof LivingEntity) {
                    nearbyLivingEntities++;
                }
                nearbyEntities++;
            }
        } else {
            nearbyLivingEntities = player.getWorld().getNearbyLivingEntities(player.getLocation(), 30, 200).size();
            nearbyEntities = player.getWorld().getNearbyEntities(player.getLocation(), 30, 200, 30).size();
        }

        int playerCount;

        if (main.serverVersion == 2 || main.serverVersion == 3) {
            playerCount = Bukkit.getOnlinePlayers().size();
        } else {
            playerCount = player.getWorld().getPlayerCount();
        }

        String[] lines = new String[10];
        String auxStr = "";
        if (main.Emergency) auxStr = "from " + main.totalMaxChunkEntities;

        if (main.myBukkit.isFolia())
            lines[0] = ChatColor.GREEN + "Last: " + lastTPS() + "  Regions: " + getCountRegions() + "  CurAvg: " + Math.round(average1) + "  PrevAvg: " + Math.round(average2);
        else
            lines[0] = ChatColor.GREEN + "Last: " + lastTPS() + "  CurAvg: " + Math.round(average1) + "  PrevAvg: " + Math.round(average2);

        lines[1] = ChatColor.GREEN + "NBL: " + nearbyLivingEntities + "  NBE: " + nearbyEntities + "  Chk: " + currentChunkEntities + auxStr + "  CHKTile: " + currentChunkTileEntities;
        lines[2] = ChatColor.GREEN + "Entit: " + currentEntities + "  Living: " + currentLiving + "  Players: " + playerCount;

        if (main.Emergency) {
            lines[3] = ChatColor.RED + "Shut:" + main.autoShutdown + "@" + String.format("00", main.autoShutDownTime) + "h  TpsCrt:" + main.tpsProtection + "  NChatRep:" + main.antiChatReport + "  AntiHack:" + main.hackProtection;
            lines[4] = ChatColor.RED + "AntiGrief: " + main.AntigriefProtection + "  Speed: " + main.speedProtection + "  Expl: " + main.ExplodeProtection + "  Wither: " + main.WitherProtection;
            lines[5] = ChatColor.YELLOW + main.alert;
        } else {
            lines[3] = ChatColor.GREEN + "Shut:" + main.autoShutdown + "@" + String.format("00", main.autoShutDownTime) + "h  TpsCrt:" + main.tpsProtection + "  NChatRep:" + main.antiChatReport + "  AntiHack:" + main.hackProtection;
            lines[4] = ChatColor.YELLOW + main.alert;
        }

        String[] finalFines;

        if (main.Emergency) finalFines = new String[6];
        else finalFines = new String[5];

        System.arraycopy(lines, 0, finalFines, 0, finalFines.length);

        pls.returnBoard().updateLines(finalFines);
    }


    public void deleteScoreBoards(Player player) {

        if (playersWithScoreboard.get(player) != null) {
            playersWithScoreboard.remove(player);

            PlayerStatus pls = main.playerControl.get(player.getUniqueId().toString());
            FastBoard board = pls.returnBoard();

            if (board != null) {
                board.delete();
            }

        }
    }

}


