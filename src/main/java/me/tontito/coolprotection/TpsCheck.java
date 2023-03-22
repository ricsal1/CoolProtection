package me.tontito.coolprotection;

import com.destroystokyo.paper.event.entity.PreCreatureSpawnEvent;
import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;


public class TpsCheck implements Listener {

    private final boolean autoShutdown;
    private final int shutCounterTime;
    private final int autoShutdownTime;
    int[] tps = new int[61];
    //private ArrayList<int[]> foliaTps;
    Main main;
    private LocalDateTime startShutTime;
    private int previousMinute = 0;
    private int lastSecond = 0;
    private int currSecond = 0;
    private int lastMinuteClean;
    private final Hashtable<Player, String> playersWithScoreboard = new Hashtable<>();
    private double average1, average2;

    protected int redStoneObjs = 0;
    protected int lastRedStone = 0;
    protected Hashtable<Chunk, Integer> redStoneChunk = new Hashtable<>();
    protected Hashtable<Location, Integer> redStoneBlockComponents = new Hashtable<>();
    private int lagDuration = 0;
    private String lastAlert = "";
    private int lastAlertCounter = 0;


    public TpsCheck(@NotNull Main main, boolean AutoShut, int shutCounterTime, int autoShutdownTime) {
        this.main = main;
        autoShutdown = AutoShut;
        this.shutCounterTime = shutCounterTime;
        this.autoShutdownTime = autoShutdownTime;

        //for spigot and bukkit
        if (main.serverVersion == 2 || main.serverVersion == 3) {
            main.getServer().getScheduler().scheduleSyncRepeatingTask(
                    main, () -> {
                        updateTps(0);
                    }, 0,// waits 0 ticks
                    1 // 1 tick de interval (tick interno, 20 by second, if at least one is missing then we have problems)
            );
        }
//        else if (main.serverVersion == 8) {
//            //Folia server specific tick handler, will handle all regional ticks (bubbles)
//            foliaTps = new ArrayList();
//
//        }

        if (autoShutdown) {
            main.getLogger().info(" Auto Shutdown server is on! ");
        }
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
        updateTps(event.getTimeRemaining());
    }


    private void updateTps(long remainDuration) {

        LocalDateTime date = LocalDateTime.now();
        int seconds = date.toLocalTime().getSecond();

        if (remainDuration < 0) tps[seconds] = tps[seconds] - 1;
        else tps[seconds] = tps[seconds] + 1;

        if (currSecond != seconds) {
            lastSecond = currSecond;
            currSecond = seconds;

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
            } else tps[seconds + 1] = 0;

            if (main.tpsProtection) adjustRuntimeSettings(date, lastTPS());
            if (autoShutdown) checkTurnOff(date);

            showLagToPlayers();

            updatePlayerStatus();
        }
    }


    public int lastTPS() {
        return tps[lastSecond];
    }


    private void adjustRuntimeSettings(LocalDateTime date, int mytps) {

        Player player = null;

        for (Player pls : Bukkit.getOnlinePlayers()) {
            player = pls;
            break;
        }

        if (player == null) return;

        World world = player.getWorld();
        int currentLiving = world.getLivingEntities().size();
        int currentEntities = world.getEntities().size();
        int currentChunkEntities = player.getLocation().getChunk().getEntities().length;

        if (mytps >= 18) {
            main.tpsLevel = 0;
            lagDuration = 0;

            boolean changed = false;

            if (currentLiving > main.maxLiving + 6) {
                main.maxLiving = currentLiving;
                changed = true;
            }

            if (currentEntities > main.maxEntities + 6) {
                main.maxEntities = currentEntities;
                changed = true;
            }

            if (currentChunkEntities > main.maxChunkEntities + 3 && currentChunkEntities < main.totalMaxChunkEntities) {
                if (currentChunkEntities < 20) {
                    main.maxChunkEntities = currentChunkEntities;
                } else {
                    main.maxChunkEntities = main.maxChunkEntities + 3; //slower growth
                }
                changed = true;
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

            if (main.maxChunkEntities > 60) {
                main.maxChunkEntities = main.maxChunkEntities - 3;
                changed = true;
            } else {
                main.maxChunkEntities = main.maxChunkEntities - 1;
            }

            if (changed)
                Utils.logToFile("Protection Manager", "TPS " + lastTPS() + " Adjusted: Max LivingEntities " + main.maxLiving + "  maxEntities " + main.maxEntities + "  maxChunkEntities " + main.maxChunkEntities);

        } else {

            if (lagDuration > 2) {
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

        if (autoShutdown && (autoShutdownTime <= 7 && hours >= autoShutdownTime && hours <= 7)) {

            if (main.getServer().getOnlinePlayers().size() == 0) {

                if (startShutTime == null) {
                    startShutTime = date;
                    return;
                }

                long limit = shutCounterTime - ChronoUnit.MINUTES.between(startShutTime, date);

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

        if (!main.tpsProtection) {
            return;
        }

        int tmpMinute = date.toLocalTime().getMinute();
        if (lastMinuteClean == tmpMinute) return; //apenas a cada minuto

        long millis = System.currentTimeMillis();
        int counter = 0;
        lastMinuteClean = tmpMinute;

        for (World world : main.getServer().getWorlds()) {

            for (Entity entity : world.getEntities()) {

                if (entity instanceof Item && entity.isOnGround() && entity.getType() == EntityType.DROPPED_ITEM) {
                    Item item = (Item) entity;

                    if (item.getTicksLived() > 3000 && item.getItemStack().getEnchantments().size() == 0) {
                        main.getLogger().info("ticks: " + item.getTicksLived() + " type: " + item.getName() + " in world: " + world.getName() + " amount: " + item.getItemStack().getAmount());

                        counter = counter + item.getItemStack().getAmount();
                        item.remove();
                    }
                }
            }
        }

        if (counter > 0) {
            main.getServer().broadcastMessage("Removed " + counter + " droped items " + (System.currentTimeMillis() - millis) + "ms");
            main.getLogger().info("tps: " + lastTPS() + "  Removed " + counter + " droped items " + (System.currentTimeMillis() - millis) + "ms");
        }
    }


    public void registerScoreBoards(Player player) {
        if (!playersWithScoreboard.contains(player)) {
            playersWithScoreboard.put(player, "-1");
        }
    }


    public void deleteScoreBoards(Player player) {
        playersWithScoreboard.replace(player, "0");
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

        if (playersWithScoreboard.size() == 0) return;

        Enumeration<Player> e = playersWithScoreboard.keys();

        while (e.hasMoreElements()) {

            Player player = e.nextElement();

            if (playersWithScoreboard.get(player).equals("-1")) {
                setScoreBoard(player);
                playersWithScoreboard.replace(player, "1");
            } else if (playersWithScoreboard.get(player).equals("0")) {
                playersWithScoreboard.remove(player);
                player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
                continue;
            }

            updateScoreBoard(player);
        }
    }


    private void updatePlayerStatus() {

        if (!main.hackProtection && !main.speedProtection) return;

        if (main.playerControl == null || main.playerControl.size() == 0) return;

        Enumeration<Player> e = main.playerControl.keys();

        while (e.hasMoreElements()) {

            Player player = e.nextElement();
            PlayerStatus p = (PlayerStatus) main.playerControl.get(player);

            //for players with penalties
            if (main.hackProtection && p.speed < 0.2) {
                p.speed = p.speed + 0.02f;
                player.setWalkSpeed(p.speed);
            }
            //for players with wrong speed
            else if (main.speedProtection && player.getWalkSpeed() > (float) 0.2 && player.getActivePotionEffects().size() == 0) {
                player.setWalkSpeed((float) 0.2);
                Utils.logToFile("Protection Manager", player.getName() + " got its walk speed adjusted");
                main.alert = player.getName() + " got its walk speed reset";
                //max recommended 0.335
            }
        }
    }


    private void setScoreBoard(Player player) {

        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();

        Objective obj = board.registerNewObjective("Lagmeter", "dummy", "Lagmeter");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        obj.setDisplayName(ChatColor.BOLD + "----Lagmeter " + main.getDescription().getVersion() + "----");

        Team t = board.registerNewTeam("t");
        t.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);

        Team Maps = board.registerNewTeam("Lagmeter");
        Maps.addEntry(ChatColor.GRAY.toString());
        Maps.setPrefix(ChatColor.WHITE + "TPS: ");
        Maps.setSuffix(ChatColor.GREEN + "");
        obj.getScore(ChatColor.GRAY.toString()).setScore(7);

        Maps = board.registerNewTeam("Lagmeter1");
        Maps.addEntry(ChatColor.YELLOW.toString());
        Maps.setPrefix(ChatColor.WHITE + "Player NearBy: ");
        Maps.setSuffix(ChatColor.GREEN + "");
        obj.getScore(ChatColor.YELLOW.toString()).setScore(6);

        Maps = board.registerNewTeam("Lagmeter2");
        Maps.addEntry(ChatColor.RED.toString());
        Maps.setPrefix(ChatColor.WHITE + "World Counts: ");
        Maps.setSuffix(ChatColor.GREEN + "");
        obj.getScore(ChatColor.RED.toString()).setScore(5);

        Maps = board.registerNewTeam("Lagmeter3");
        Maps.addEntry(ChatColor.BLUE.toString());
        Maps.setPrefix(ChatColor.WHITE + "Current Max allowed Entities: ");
        Maps.setSuffix(ChatColor.GREEN + "");
        obj.getScore(ChatColor.BLUE.toString()).setScore(4);

        Maps = board.registerNewTeam("Lagmeter4");
        Maps.addEntry(ChatColor.LIGHT_PURPLE.toString());
        Maps.setPrefix(ChatColor.WHITE + "Alert: ");
        Maps.setSuffix(ChatColor.GREEN + "");
        obj.getScore(ChatColor.LIGHT_PURPLE.toString()).setScore(3);

        Maps = board.registerNewTeam("Lagmeter5");
        Maps.addEntry(ChatColor.GREEN.toString());
        Maps.setPrefix(ChatColor.WHITE + "--------------------------------------------------");
        Maps.setSuffix(ChatColor.GREEN + "");
        obj.getScore(ChatColor.GREEN.toString()).setScore(2);

        player.setScoreboard(board);
    }


    private void updateScoreBoard(Player player) {
        Scoreboard board = player.getScoreboard();
        board.resetScores(player.getName());

        int currentLiving = player.getWorld().getLivingEntities().size();
        int currentEntities = player.getWorld().getEntities().size();
        int currentChunkEntities = player.getLocation().getChunk().getEntities().length;
        int currentChunkTileEntities = player.getLocation().getChunk().getTileEntities().length;

        Team Maps = board.getTeam("Lagmeter");

        if (Maps == null) {
            deleteScoreBoards(player);
            return;
        }

        Maps.setSuffix(ChatColor.GREEN + "Last: " + lastTPS() + "  CurAvg: " + Math.round(average1) + "  PrevAvg: " + Math.round(average2));

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

        Maps = board.getTeam("Lagmeter1");
        Maps.setSuffix(ChatColor.GREEN + "NBL: " + nearbyLivingEntities + "  NBE: " + nearbyEntities + "  Chk: " + currentChunkEntities + "  CHKTile: " + currentChunkTileEntities);

        Maps = board.getTeam("Lagmeter2");
        Maps.setSuffix(ChatColor.GREEN + "Entit: " + currentEntities + "  Living: " + currentLiving + "  Players: " + player.getWorld().getPlayerCount());

        Maps = board.getTeam("Lagmeter3");
        Maps.setSuffix(ChatColor.GREEN + "Chk: " + main.maxChunkEntities + "  Ent: " + main.maxEntities + "  Liv: " + main.maxLiving);

        Maps = board.getTeam("Lagmeter4");
        Maps.setSuffix(ChatColor.GREEN + "" + main.alert);
    }

}


