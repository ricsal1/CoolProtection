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
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicInteger;


public class TpsCheck implements Listener {

    private final boolean autoShutdown;
    private final int shutCounterTime;
    private final int autoShutdownTime;
    int[] tps = new int[61];
    int[] regions = new int[61];
    int countTicks = 0;
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

    private long tickNumber = 0;
    private long lastTick = 0;
    private int maxRegions = 0;


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
        tickNumber = event.getTickNumber();
        if (tickNumber == (lastTick + 5)) maxRegions = maxRegions + 1;

        updateTps(event.getTimeRemaining());
    }


    private void updateTps(long remainDuration) {

        LocalDateTime date = LocalDateTime.now();
        int seconds = date.toLocalTime().getSecond();

        synchronized (this) {
            if (remainDuration < 0) tps[seconds] = tps[seconds] - 1;
            else tps[seconds] = tps[seconds] + 1;

            countTicks++;

            if (currSecond != seconds) {
                lastTick = tickNumber;
                //updates last second
                if (main.myBukkit.isFolia()) {
                    regions[currSecond] = maxRegions;

                    if (regions[currSecond] > 0) {
                        tps[currSecond] = tps[currSecond] / regions[currSecond];
                    }

                    maxRegions = 0;

                    if (currSecond == 59) {
                        for (int i = 0; i < 57; i++) {
                            regions[i] = 0;
                        }
                    } else regions[currSecond + 1] = 0;
                }

                lastSecond = currSecond;
                currSecond = seconds;
                countTicks = 0;

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

                if (main.tpsProtection)  adjustRuntimeSettings(date, lastTPS());

                if (autoShutdown) checkTurnOff(date);

                showLagToPlayers();

                updatePlayerStatus();

                if (main.chunkWater.size() > 0 && main.chunkWater.get((long) -4) != null)
                    System.out.println("sz: " + main.chunkWater.size() + " te: " + main.chunkWater.get((long) -1) + " fl: " + main.chunkWater.get((long) -2) + " pr: " + main.chunkWater.get((long) -3) + " tt: " + (main.chunkWater.get((long) -4) / 1000000)  + " ot: " + main.chunkWater.get((long) -5));

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


    private void adjustRuntimeSettings(LocalDateTime date, int mytps) {

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
        main.myBukkit.runTask(null,local,null, () -> currentLiving.set(world.getEntities().size()));

        AtomicInteger currentEntities = new AtomicInteger(); //= world.getEntities().size();
        main.myBukkit.runTask(null,local,null, () -> currentEntities.set(world.getEntities().size()));

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

           // emergencyClean(date);
           // main.myBukkit.run(null, () -> emergencyClean(date), main);
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

                if (main.myBukkit.isFolia() && !main.myBukkit.isOwnedby(entity, null, null)) {
                    continue;
                }

                if (entity instanceof Item && entity.isOnGround() && entity.getType() == EntityType.DROPPED_ITEM) {
                    Item item = (Item) entity;

                    if (item.getTicksLived() > 3000 && item.getItemStack().getEnchantments().size() == 0) {
                        main.getLogger().info("ticks droped: " + item.getTicksLived() + " type: " + item.getName() + " in world: " + world.getName() + " amount: " + item.getItemStack().getAmount());

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

                if (main.myBukkit.isFolia()) {
                    continue;
                }

                player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
                continue;
            }

            updateScoreBoard(player);
        }
    }


    private void updatePlayerStatus() {

        if (!main.hackProtection && !main.speedProtection) return;

        if (main.playerControl == null || main.playerControl.size() == 0) return;

        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerStatus p = main.playerControl.get(player.getUniqueId().toString());

            if (p != null) {
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
    }


    public void sendActionBar(Player player) {
        long global = -1;
        String x;

        if (main.chunkWater.get(global) == null || main.chunkWater.get(global) < 300) {
            x = "";
        } else {
            x = " Water count: " + main.chunkWater.get(global) + " " + (main.chunkWater.size() - 1) + " chunks";
        }

        String message = ChatColor.GREEN + "Last: " + lastTPS() + "  " + getCountRegions() + " regions  CurAvg: " + Math.round(average1) + "  PrevAvg: " + Math.round(average2) + x + "  " + main.alert;
        player.sendActionBar(message);
    }



    private void setScoreBoard(Player player) {

//                if (main.myBukkit.isFolia()) {
//            dev.danablend.counterstrike.utils.Board.FastBoard board = new dev.danablend.counterstrike.utils.Board.FastBoard(player);
//            board.updateTitle(ChatColor.BOLD + "----Miner Strike v" + CounterStrike.i.getDescription().getVersion() + "----");
//            csplayer.setBoard(board);
//return;
//        }



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
        Maps.setPrefix(ChatColor.WHITE + "Max allowed Entities: ");
        Maps.setSuffix(ChatColor.GREEN + "");
        obj.getScore(ChatColor.BLUE.toString()).setScore(4);

        Maps = board.registerNewTeam("Lagmeter4");
        Maps.addEntry(ChatColor.LIGHT_PURPLE.toString());
        Maps.setPrefix(ChatColor.WHITE + "Confs: ");
        Maps.setSuffix(ChatColor.GREEN + "");
        obj.getScore(ChatColor.LIGHT_PURPLE.toString()).setScore(3);

        Maps = board.registerNewTeam("Lagmeter5");
        Maps.addEntry(ChatColor.DARK_PURPLE.toString());
        Maps.setPrefix(ChatColor.WHITE + "Alert: ");
        Maps.setSuffix(ChatColor.GREEN + "");
        obj.getScore(ChatColor.DARK_PURPLE.toString()).setScore(2);

        Maps = board.registerNewTeam("Lagmeter6");
        Maps.addEntry(ChatColor.DARK_BLUE.toString());
        Maps.setPrefix(ChatColor.WHITE + "-----------------------------------------------");
        Maps.setSuffix(ChatColor.GREEN + "");
        obj.getScore(ChatColor.DARK_BLUE.toString()).setScore(1);

        player.setScoreboard(board);
    }


    private void updateScoreBoard(Player player) {

        if (main.myBukkit.isFolia()) {
            sendActionBar(player);
            return;
        }

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

        int playerCount;

        if (main.serverVersion == 2 || main.serverVersion == 3) {
            playerCount = Bukkit.getOnlinePlayers().size();
        } else {
            playerCount = player.getWorld().getPlayerCount();
        }

        Maps = board.getTeam("Lagmeter2");
        Maps.setSuffix(ChatColor.GREEN + "Entit: " + currentEntities + "  Living: " + currentLiving + "  Players: " + playerCount);

        Maps = board.getTeam("Lagmeter3");
        Maps.setSuffix(ChatColor.GREEN + "Chk: " + main.maxChunkEntities + "  Ent: " + main.maxEntities + "  Liv: " + main.maxLiving);

        Maps = board.getTeam("Lagmeter4");
        Maps.setSuffix(ChatColor.GREEN + "Shut:" + autoShutdown + "@" + String.format("00",autoShutdownTime) + "h  TpsCrt:" + main.tpsProtection + "  NChatRep:" + main.antiChatReport + "  AntiHack:"+ main.hackProtection);

        Maps = board.getTeam("Lagmeter5");
        Maps.setSuffix(ChatColor.GREEN + "" + main.alert);
    }





//    public void setScoreBoard(CSPlayer csplayer) {
//        Player player = csplayer.getPlayer();
//
//        if (!player.isOnline()) return;
//
//        //inits player colour
//        player.setPlayerListName(ChatColor.valueOf(csplayer.getColour()) + player.getName());
//
//        if (isFolia) {
//            dev.danablend.counterstrike.utils.Board.FastBoard board = new dev.danablend.counterstrike.utils.Board.FastBoard(player);
//            board.updateTitle(ChatColor.BOLD + "----Miner Strike v" + CounterStrike.i.getDescription().getVersion() + "----");
//            csplayer.setBoard(board);
//
//        } else {
//            //CSPlayer csplayer = CounterStrike.i.getCSPlayer(player, false, null);
//
//            Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
//
//            Objective obj = board.registerNewObjective("counterStrike", "dummy", "counterStrike");
//            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
//            obj.setDisplayName(ChatColor.BOLD + "----Miner Strike v" + CounterStrike.i.getDescription().getVersion() + "----");
//
//            dev.danablend.counterstrike.csplayer.Team myTeam;
//
//            if (csplayer.getTeam().equals(TeamEnum.COUNTER_TERRORISTS)) {
//                myTeam = CounterStrike.i.getCounterTerroristsTeam();
//            } else {
//                myTeam = CounterStrike.i.getTerroristsTeam();
//            }
//
//            // Teams to hide name tags
//            Team t = board.registerNewTeam("t");
//            Team ct = board.registerNewTeam("ct");
//            t.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OWN_TEAM);
//            ct.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.FOR_OWN_TEAM);
//
//            for (CSPlayer p : CounterStrike.i.getTerroristsTeam().getCsPlayers()) {
//                t.addEntry(p.getPlayer().getName());
//            }
//            for (CSPlayer p : CounterStrike.i.getCounterTerroristsTeam().getCsPlayers()) {
//                ct.addEntry(p.getPlayer().getName());
//            }
//
//            // Display the round Map
//            Team Maps = board.registerNewTeam("MapsRound");
//            Maps.addEntry(ChatColor.GRAY.toString());
//            Maps.setPrefix(ChatColor.LIGHT_PURPLE + "Map: ");
//            Maps.setSuffix(ChatColor.GREEN + "");
//            obj.getScore(ChatColor.GRAY.toString()).setScore(18);
//
//            Team roundTeams = board.registerNewTeam("Teams");
//            roundTeams.addEntry(ChatColor.RED.toString());
//            roundTeams.setPrefix(ChatColor.LIGHT_PURPLE + "Teams: ");
//            roundTeams.setSuffix("");
//            obj.getScore(ChatColor.RED.toString()).setScore(17);
//
//            Integer scoreCounter = 16;
//
//            for (CSPlayer csplayer1 : myTeam.getCsPlayers()) {
//                Team killCounter = board.registerNewTeam("k" + scoreCounter);
//
//                killCounter.addEntry(ChatColor.valueOf(csplayer1.getColour()) + csplayer1.getPlayer().getName());
//                killCounter.setPrefix("");
//                killCounter.setSuffix(": " + ChatColor.GREEN + "$" + csplayer1.getMoney() + ChatColor.LIGHT_PURPLE + " K: " + ChatColor.GREEN + "" + csplayer1.getKills() + "  " + ChatColor.LIGHT_PURPLE + "D: " + ChatColor.GREEN + "" + csplayer1.getDeaths());
//                obj.getScore(ChatColor.valueOf(csplayer1.getColour()) + csplayer1.getPlayer().getName()).setScore(scoreCounter);
//                scoreCounter--;
//            }
//
//            player.setScoreboard(board);
//        }
//    }
//
//
//    public void updateScoreBoard(CSPlayer csplayer) {
//        Player player = csplayer.getPlayer();
//        //CSPlayer csplayer = CounterStrike.i.getCSPlayer(player, false, null);
//
//        if (!player.isOnline()) return;
//
//        if (isFolia) {
//            updateFastScoreBoard(csplayer);
//        } else {
//            Scoreboard board = player.getScoreboard();
//            board.resetScores(player.getName());
//
//            dev.danablend.counterstrike.csplayer.Team myTeam;
//
//            if (csplayer.getTeam().equals(TeamEnum.COUNTER_TERRORISTS)) {
//                myTeam = CounterStrike.i.getCounterTerroristsTeam();
//            } else {
//                myTeam = CounterStrike.i.getTerroristsTeam();
//            }
//
//            // Display the world
//            Team Maps = board.getTeam("MapsRound");
//
//            if (Maps == null) return;
//
//            Maps.setPrefix(ChatColor.LIGHT_PURPLE + "Map: ");
//            Maps.setSuffix(ChatColor.GREEN + plugin.Map + "  " + ChatColor.LIGHT_PURPLE + "R: " + ChatColor.GREEN + "" + (myTeam.getLosses() + myTeam.getWins() + 1) + " of " + MAX_ROUNDS);
//
//
//            Team roundTeams = board.getTeam("Teams");
//            roundTeams.setPrefix(ChatColor.LIGHT_PURPLE + "Teams: ");
//
//            String TeamA = ChatColor.valueOf(csplayer.getColour()) + "" + csplayer.getColour() + ": ";
//            String TeamB = ChatColor.valueOf(csplayer.getOpponentColour()) + csplayer.getOpponentColour() + ": ";
//
//            String teamString = TeamA + myTeam.getWins() + ChatColor.GRAY + " vs " + TeamB + myTeam.getLosses();
//            roundTeams.setSuffix(teamString);
//
//
//            Integer scoreCounter = 16;
//            boolean wasnull;
//
//            for (CSPlayer csplayer1 : myTeam.getCsPlayers()) {
//                Team killCounter = board.getTeam("k" + scoreCounter);
//                wasnull = false;
//
//                if (killCounter == null) {
//                    wasnull = true;
//                    killCounter = board.registerNewTeam("k" + scoreCounter);
//                }
//                killCounter.addEntry(ChatColor.valueOf(csplayer1.getColour()) + csplayer1.getPlayer().getName());
//                killCounter.setPrefix("");
//
//                Player play = csplayer1.getPlayer();
//
//                if (play.isDead()) {
//                    killCounter.setSuffix(": " + ChatColor.WHITE + ChatColor.UNDERLINE + " DEAD  " + ChatColor.BOLD + "$" + csplayer1.getMoney() + " K: " + "" + csplayer1.getKills() + "  " + "D: " + "" + csplayer1.getDeaths());
//                } else {
//                    killCounter.setSuffix(": " + ChatColor.GREEN + "$" + csplayer1.getMoney() + ChatColor.LIGHT_PURPLE + " K:" + ChatColor.GREEN + "" + csplayer1.getKills() + " " + ChatColor.LIGHT_PURPLE + "D:" + ChatColor.GREEN + "" + csplayer1.getDeaths() + " " + ChatColor.LIGHT_PURPLE + "MVP:" + ChatColor.GREEN + "" + csplayer1.getMVP());
//                }
//
//                if (wasnull) {
//                    Objective obj = board.getObjective("counterStrike");
//                    obj.getScore(ChatColor.valueOf(csplayer1.getColour()) + csplayer1.getPlayer().getName()).setScore(scoreCounter);
//                }
//                scoreCounter--;
//            }
//        }
//    }
//
//
//    public void updateFastScoreBoard(CSPlayer csplayer) {
//
//        if (csplayer.returnBoard() == null) return;
//
//        dev.danablend.counterstrike.csplayer.Team myTeam;
//
//        if (csplayer.getTeam().equals(TeamEnum.COUNTER_TERRORISTS)) {
//            myTeam = CounterStrike.i.getCounterTerroristsTeam();
//        } else {
//            myTeam = CounterStrike.i.getTerroristsTeam();
//        }
//
//        String TeamA = ChatColor.valueOf(csplayer.getColour()) + "" + csplayer.getColour() + ": ";
//        String TeamB = ChatColor.valueOf(csplayer.getOpponentColour()) + csplayer.getOpponentColour() + ": ";
//
//        String[] lines = new String[21];
//
//        lines[0] = ChatColor.LIGHT_PURPLE + "Map: " + ChatColor.GREEN + plugin.Map + "  " + ChatColor.LIGHT_PURPLE + "R: " + ChatColor.GREEN + "" + (myTeam.getLosses() + myTeam.getWins() + 1) + " of " + MAX_ROUNDS;
//        lines[1] = ChatColor.LIGHT_PURPLE + "Teams: " + TeamA + myTeam.getWins() + ChatColor.GRAY + " vs " + TeamB + myTeam.getLosses();
//
//        ChatColor c1 = ChatColor.valueOf(plugin.counterTerroristsTeam.getColour());
//        lines[2] = ChatColor.BOLD + "" + plugin.counterTerrorists.size() + " " + c1 + "Counters" + ChatColor.WHITE + " with " + plugin.counterTerroristsTeam.getWins() + " wins: ";
//        int linha = 3;
//
//        for (CSPlayer csplayer1 : plugin.counterTerrorists) {
//
//            Player play = csplayer1.getPlayer();
//
//            if (play.isDead()) {
//                lines[linha] = play.getName() + ": " + ChatColor.WHITE + ChatColor.UNDERLINE + " DEAD  " + ChatColor.BOLD + "$" + csplayer1.getMoney() + " K: " + "" + csplayer1.getKills() + "  " + "D: " + csplayer1.getDeaths();
//            } else {
//                lines[linha] = play.getName() + ": " + ChatColor.GREEN + "$" + csplayer1.getMoney() + ChatColor.LIGHT_PURPLE + " K:" + ChatColor.GREEN + "" + csplayer1.getKills() + " " + ChatColor.LIGHT_PURPLE + "D:" + ChatColor.GREEN + csplayer1.getDeaths() + " " + ChatColor.LIGHT_PURPLE + "MVP:" + ChatColor.GREEN + "" + csplayer1.getMVP();
//            }
//            linha++;
//        }
//
//        c1 = ChatColor.valueOf(plugin.terroristsTeam.getColour());
//        lines[linha] = ChatColor.BOLD + "" + plugin.terrorists.size() + " " + c1 + "Terrors" + ChatColor.WHITE + " with " + plugin.terroristsTeam.getWins() + " wins: ";
//        linha++;
//
//        for (CSPlayer csplayer1 : plugin.terrorists) {
//            Player play = csplayer1.getPlayer();
//
//            if (play.isDead()) {
//                lines[linha] = play.getName() + ": " + ChatColor.WHITE + ChatColor.UNDERLINE + " DEAD  " + ChatColor.BOLD + "$" + csplayer1.getMoney() + " K: " + "" + csplayer1.getKills() + "  " + "D: " + csplayer1.getDeaths();
//            } else {
//                lines[linha] = play.getName() + ": " + ChatColor.GREEN + "$" + csplayer1.getMoney() + ChatColor.LIGHT_PURPLE + " K:" + ChatColor.GREEN + "" + csplayer1.getKills() + " " + ChatColor.LIGHT_PURPLE + "D:" + ChatColor.GREEN + csplayer1.getDeaths() + " " + ChatColor.LIGHT_PURPLE + "MVP:" + ChatColor.GREEN + "" + csplayer1.getMVP();
//            }
//            linha++;
//        }
//
//        String[] finalFines = new String[linha];
//
//        for (int i = 0; i < linha; i++) {
//            finalFines[i] = lines[i];
//        }
//
//        csplayer.returnBoard().updateLines(finalFines);
//    }
//
//
//    public void deleteScoreBoards(Player player) {
//        if (playersWithScoreboard.contains(player.getUniqueId())) {
//            playersWithScoreboard.remove(player.getUniqueId());
//
//            if (isFolia) {
//                CSPlayer csplayer = CounterStrike.i.getCSPlayer(player, false, null);
//
//                if (csplayer == null) {
//                    return;
//                }
//
//                dev.danablend.counterstrike.utils.Board.FastBoard board = csplayer.returnBoard();
//
//                if (board != null) {
//                    board.delete();
//                }
//            } else {
//                player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
//            }
//        }
//    }

}


