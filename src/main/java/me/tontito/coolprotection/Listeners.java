package me.tontito.coolprotection;

import org.apache.commons.lang.StringUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Hashtable;
import java.util.Map;

import static org.bukkit.Bukkit.getServer;
import static org.bukkit.entity.SpawnCategory.MONSTER;
import static org.bukkit.event.player.PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED;

public class Listeners implements Listener {

    protected int maxWithers = 8;
    protected int maxLighting = 4;
    private boolean AntigriefProtection = false;
    private TpsCheck tps;
    private Main main;
    private final Hashtable playerCountry = new Hashtable();


    public Listeners(Main main, TpsCheck tps) {
        this.main = main;
        this.tps = tps;

        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerStatus p = new PlayerStatus();
            p.speed = player.getWalkSpeed();

            main.playerControl.put(player, p);
        }
    }


    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {

        if (AntigriefProtection && event.getMessage().startsWith("/fill") && event.getMessage().toUpperCase().contains("TNT")) {

            if (main.playerControl.get(event.getPlayer()) != null) {
                Player player = event.getPlayer();

                PlayerStatus play = (PlayerStatus) main.playerControl.get(player);
                play.speed = play.speed - 0.02f;

                player.setWalkSpeed(play.speed);
                main.getLogger().info("Slowing player " + player.getName());
            }

            event.setCancelled(true);
        }

        if (AntigriefProtection && event.getMessage().startsWith("/execute as ")) {

            if (main.playerControl.get(event.getPlayer()) != null) {
                Player player = event.getPlayer();

                PlayerStatus play = (PlayerStatus) main.playerControl.get(player);
                play.speed = play.speed - 0.02f;

                player.setWalkSpeed(play.speed);
                main.getLogger().info("Slowing player " + player.getName());
            }

            event.setCancelled(true);
        }
    }


    @EventHandler(ignoreCancelled = true)
    public void onPlayerChat(@NotNull PlayerChatEvent chat) {

        Player player = chat.getPlayer();
        String message = chat.getMessage().toLowerCase();

        if (message.equalsIgnoreCase("!lagmeter on") && player.isOp()) {
            tps.registerScoreBoards(chat.getPlayer());
            chat.setCancelled(true);

        } else if (message.equalsIgnoreCase("!lagmeter off") && player.isOp()) {
            tps.deleteScoreBoards(chat.getPlayer());
            chat.setCancelled(true);

        } else if (message.startsWith("!maxchunk ") && player.isOp()) {

            if (message.split(" ").length > 1) {
                String valor = message.split(" ")[1];

                if (StringUtils.isNumeric(valor)) {
                    FileConfiguration config = main.getConfig();
                    config.set("maxChunkEntities", Integer.parseInt(valor));
                    main.saveConfig();
                    main.totalMaxChunkEntities = Integer.parseInt(valor);
                    player.sendRawMessage("Max chunkEntity value changed to " + valor + "  prev Game limits: " + main.getServer().getWorld("world").getSpawnLimit(MONSTER));

                    if (Integer.parseInt(valor) < 50 || Integer.parseInt(valor) > 200)
                        valor = "70"; //game defaults 70 from bukkit.yml

                    main.getServer().getWorld("world").setGameRule(GameRule.MAX_ENTITY_CRAMMING, 24); //to fix previous mistake
                    main.getServer().getWorld("world").setSpawnLimit(MONSTER, Integer.parseInt(valor));

                    chat.setCancelled(true);
                }
            }
        } else if (message.equalsIgnoreCase("!antigrief on") && player.isOp()) {
            AntigriefProtection = true;
            player.sendRawMessage("Enabled antigrief");
            chat.setCancelled(true);

        } else if (message.equalsIgnoreCase("!antigrief off") && player.isOp()) {
            AntigriefProtection = false;
            player.sendRawMessage("Disabled antigrief");
            chat.setCancelled(true);

        } else if (message.equalsIgnoreCase("!autoBalance off") && player.isOp()) {
            if (main.totalMaxChunkEntities > 0) {
                main.totalMaxChunkEntities = 0;
                player.sendRawMessage("Disabled adaptative balancing");
            }
            chat.setCancelled(true);
        } else if (message.equalsIgnoreCase("!autoBalance on") && player.isOp()) {
            if (main.totalMaxChunkEntities == 0) {
                try {
                    main.totalMaxChunkEntities = main.getConfig().getInt("maxChunkEntities");
                } catch (Exception e) {
                    main.totalMaxChunkEntities = 30;
                }
                if (main.totalMaxChunkEntities == 0) {
                    main.totalMaxChunkEntities = 30;
                }

                player.sendRawMessage("Enabled adaptative balancing");
                player.sendRawMessage(("Current values are TPS: " + tps.lastTPS() + " Optimal Max LivingEntities: " + main.maxLiving + "  maxEntities: " + main.maxEntities + "  maxChunkEntities: " + main.maxChunkEntities));
                chat.setCancelled(true);
            }
        }
    }


    @EventHandler
    public void onPlayerResourcePackStatusEvent(@NotNull PlayerResourcePackStatusEvent event) {
        if (event.getStatus().equals(SUCCESSFULLY_LOADED)) {

            Player player = event.getPlayer();
            String country = (String) playerCountry.get(player.getName());

            if (main.DEFAULT_RESOURCE_HASH.equals("8DF285F52CBC6CE770D42B31DAE4535AECB6BE5C") && country.contains("Russia")) {
                player.playSound(player.getLocation(), Sound.MUSIC_DISC_OTHERSIDE, SoundCategory.AMBIENT, 5.0F, 1.0F);
            }
        }
    }


    @EventHandler
    public void onPlayerJoinEvent(@NotNull PlayerJoinEvent event) {

        Player player = event.getPlayer();
        String country = "";

        try {
            country = Utils.getCountry(player.getAddress().getHostString());

            if (country == null) country = "";
        } catch (Exception e) {
            country = "";
        }

        Utils.logToFile("Protection Manager", player.getName() + " joined " + country);

        if (playerCountry.get(player.getName()) == null) {
            playerCountry.put(player.getName(), country);
        }

        if (main.playerControl.get(player) == null) {
            PlayerStatus p = new PlayerStatus();
            p.speed = player.getWalkSpeed();

            main.playerControl.put(player, p);
        }

        if (!main.DEFAULT_RESOURCE.equals("")) {
            event.getPlayer().setResourcePack(main.DEFAULT_RESOURCE, main.DEFAULT_RESOURCE_HASH);
        }

        if (main.hackProtection) {
            try {
                for (int i = 0; i < player.getInventory().getSize(); i++) {
                    ItemStack item = player.getInventory().getItem(i);
                    if (item != null && item.getEnchantments() != null) {

                        for (Map.Entry<Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
                            int level = entry.getValue();

                            if (level > 6) {
                                if (main.hackProtection) {
                                    Utils.logToFile("Protection Manager", "To remove from player item " + item.getType() + "   " + item.getEnchantments());
                                    int amt = item.getAmount() - 1;
                                    item.setAmount(amt);
                                    player.getInventory().setItem(i, item);
                                    player.updateInventory();
                                }
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
            }
        }

        if (country.contains("Russia") || country.contains("Belarus")) {
            player.sendTitle(ChatColor.RED + "Hello dear fellow from Russia", ChatColor.AQUA + "This is an alert to give news about the world.", 20 * 5, 20 * 10, 20 * 3);

            main.getServer().getScheduler().scheduleSyncDelayedTask(main, () -> {
                player.sendTitle(ChatColor.RED + "Putin continues with terror", ChatColor.AQUA + "Kiev attacked while been visited by United Nations secretary!", 20 * 5, 20 * 10, 20 * 3);
            }, 400);

            main.getServer().getScheduler().scheduleSyncDelayedTask(main, () -> {
                player.sendTitle(ChatColor.RED + "Nuclear power plants threaten", ChatColor.AQUA + "Thousands of war crimes committed on people!", 20 * 5, 20 * 10, 20 * 3);
            }, 800);
        }
    }


    @EventHandler
    public void onPlayerQuitEvent(@NotNull PlayerQuitEvent event) {
        tps.deleteScoreBoards(event.getPlayer());
    }


    @EventHandler
    public void onServerListPingEvent(final ServerListPingEvent event) {
        if (main.serverStatus != null) {
            event.setMotd(ChatColor.AQUA + main.serverStatus);
        }
    }


    @EventHandler(ignoreCancelled = true)
    public void onEntityExplodeEvent(EntityExplodeEvent event) {

        if (main.totalMaxChunkEntities > 0) {
            int mytps = tps.lastTPS();

            if (mytps < 18) {
                event.setCancelled(true);

                if (main.tpsLevel != 2) {
                    main.tpsLevel = 2;
                    Utils.logToFile("Protection Manager", "TPS " + tps.lastTPS() + "(Explosion)Very Low tps " + event.getEntity().getType());
                }
                return;
            }
        }

        if (!main.ExplodeProtection) return;

        int level = event.getEntity().getLocation().getBlockY();

        String mundo = event.getLocation().getWorld().getName();

        if (event.getEntityType() == EntityType.PRIMED_TNT && level > main.ExplosionLevel) {

            Entity causer = ((TNTPrimed) event.getEntity()).getSource();

            if (causer instanceof Player && main.playerControl.get(((Player) causer)) != null) {
                Player player = ((Player) causer);

                PlayerStatus play = (PlayerStatus) main.playerControl.get(player);
                play.speed = play.speed - 0.02f;

                player.setWalkSpeed(play.speed);
                main.getLogger().info("Slowing player " + player.getName());
            }

            getServer().broadcastMessage(" No TNT party here!");
            //  Utils.logToFile("Protection Manager", " No TNT party here! Try layer < " + main.ExplosionLevel);
            event.setCancelled(true);
            return;

        } else if (event.getEntityType() == EntityType.MINECART_TNT && level > main.ExplosionLevel) {

            getServer().broadcastMessage(" No TNT party here!!");
            //  Utils.logToFile("Protection Manager", " No TNT party here!! Try layer < " + main.ExplosionLevel);
            event.setCancelled(true);
            return;
        }

        if (event.getEntityType() == EntityType.ENDER_CRYSTAL && level > main.ExplosionLevel && !(mundo.endsWith("_nether") || mundo.endsWith("_end"))) {
            getServer().broadcastMessage(" No end crystals here!");
            //  Utils.logToFile("Protection Manager", " No end crystals here! Try layer < " + main.ExplosionLevel);
            event.setCancelled(true);

        }

        // Utils.logToFile("Protection Manager", "TPS " + tps.lastTPS() + " debug entity" + event.getEntity().getType());
    }


    @EventHandler(ignoreCancelled = true)
    public void onLightningSpawn(LightningStrikeEvent event) {

        if (main.totalMaxChunkEntities > 0) {
            int mytps = tps.lastTPS();

            if (mytps <= 14) {
                event.setCancelled(true);

                if (main.tpsLevel != 2) {
                    main.tpsLevel = 2;
                    Utils.logToFile("Protection Manager", "TPS " + tps.lastTPS() + "(Light)Very Low tps " + event.getCause());
                }
                return;
            }
        }

        if (maxLighting > 0) {
            int counter = 0;

            for (Entity entities : event.getLightning().getLocation().getChunk().getEntities()) {
                if (entities.getType().equals(EntityType.LIGHTNING)) {
                    counter++;

                    if (counter > maxLighting) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }


    @EventHandler(ignoreCancelled = true)
    public void onCreatureSpawnEvent(@NotNull CreatureSpawnEvent event) {

        if (main.tpsLevel == 2) { //refusal mode for 1 second, if tps is lower
            event.setCancelled(true);
            return;
        }

        EntityType entidade = event.getEntityType();
        int mytps = tps.lastTPS();

        if (mytps <= 14) {
            event.setCancelled(true);
            main.tpsLevel = 2;
            main.alert = "TPS " + mytps + " Spawn Mob: " + entidade;
            Utils.logToFile("Protection Manager", main.alert);
            return;
        }

        Location location = event.getLocation();
        World world = location.getWorld();
        World.Environment mundo = world.getEnvironment();
        int yLevel = location.getBlockY();
        Entity[] listagem = location.getChunk().getEntities();

        if (main.totalMaxChunkEntities > 0) {
            if (yLevel > 200 && (entidade.equals(EntityType.SPIDER) || entidade.equals(EntityType.PILLAGER) || entidade.equals(EntityType.VINDICATOR) || entidade.equals(EntityType.RAVAGER))) {
                event.setCancelled(true);
                return;
            }

            int nearbyEntities = 0;

            if (main.serverVersion == 2 || main.serverVersion == 3) {
                for (Object entity : world.getNearbyEntities(location, 30, 200, 30)) {
                    if (entity instanceof LivingEntity) {
                        nearbyEntities++;
                    }
                }
            } else {
                nearbyEntities = world.getNearbyLivingEntities(location, 30, 200).size();
            }

            if (nearbyEntities > (main.totalMaxChunkEntities * 2)) {
                event.setCancelled(true);
                main.alert = "Near " + location.getBlockX() + "::" + location.getBlockZ() + " count " + nearbyEntities + " (SpawnMob) " + entidade;
                return;
            }

            int currentChunkLivingEntities = 0;

            for (Object entity : listagem) {
                if (entity instanceof LivingEntity) {
                    currentChunkLivingEntities++;
                }
            }

            if (mundo == World.Environment.THE_END) {
                if (currentChunkLivingEntities > main.maxChunkEntities - 20) { //lower @end
                    event.setCancelled(true);
                    main.alert = "maxChunk (SpawnMob) " + entidade;
                    return;
                }
            } else {
                if (currentChunkLivingEntities > main.maxChunkEntities + 5) {
                    event.setCancelled(true);
                    main.alert = "maxChunk (SpawnMob) " + entidade;
                    return;
                }
            }

            if (mytps < 18) {
                int currentLiving = world.getLivingEntities().size();

                if (currentLiving > main.maxLiving + 8) {
                    event.setCancelled(true);
                    main.tpsLevel = 2;
                    main.alert = "MaxLiv (SpawnMob) " + entidade;
                    return;
                }

                int currentEntities = world.getEntities().size();

                if (currentEntities > main.maxEntities + 8) {
                    event.setCancelled(true);
                    main.tpsLevel = 2;
                    main.alert = "MaxEnt (SpawnMob) " + entidade;
                    return;
                }
            }
        }

        if (!main.WitherProtection) return;

        if (entidade.equals(EntityType.WITHER)) {

            if (yLevel < main.WitherLevel && maxWithers > 0) {
                int counter = 0;

                for (Entity entities : listagem) {
                    if (entities.getType().equals(EntityType.WITHER)) {
                        counter++;

                        if (counter > maxWithers) {
                            getServer().broadcastMessage("No more then " + maxWithers + " withers at same time per chunk");
                            Utils.logToFile("Protection Manager", "No more then " + maxWithers + " withers at same time per chunk");
                            event.setCancelled(true);
                            return;
                        }
                    }
                }
            }

            if (yLevel > main.WitherLevel && !(mundo == World.Environment.NETHER || mundo == World.Environment.THE_END)) {
                event.setCancelled(true);
                getServer().broadcastMessage(" No withers here!!!");
                Utils.logToFile("Protection Manager", " No withers here!!!");
            }
        }

    }


    @EventHandler(ignoreCancelled = true)
    public void onEntitySpawnEvent(@NotNull EntitySpawnEvent event) { //DROPPED_ITEM

        if (main.tpsLevel == 3) { //refusal mode for 1 second, if tps is lower
            event.setCancelled(true);
            return;
        }

        EntityType entidade = event.getEntityType();

        if (entidade.isAlive()) {
            return;
        }

        int mytps = tps.lastTPS();

        if (mytps <= 14) {
            event.setCancelled(true);
            main.tpsLevel = 3;
            main.alert = "TPS " + mytps + " SpawnEntity: " + entidade;
            Utils.logToFile("Protection Manager", main.alert);
            return;
        }

        Location location = event.getLocation();
        World world = location.getWorld();

        if (main.totalMaxChunkEntities > 0) {
            int nearbyEntities = world.getNearbyEntities(location, 30, 200, 30).size();

            if (nearbyEntities > (main.totalMaxChunkEntities * 3)) {
                event.setCancelled(true);
                main.alert = "Near " + location.getBlockX() + "::" + location.getBlockZ() + " count " + nearbyEntities + " (SpawnEntity) blocking during 1 sec";
                Utils.logToFile("Protection Manager", main.alert);
                main.tpsLevel = 3; //to avoid multiple blocks and writes @log
                return;
            }
        }
    }


    @EventHandler(ignoreCancelled = true)
    public void onEntityPlaceEvent(@NotNull EntityPlaceEvent event) {

        if (main.tpsLevel == 4) { //refusal mode for 1 second
            event.setCancelled(true);
            return;
        }

        Entity entidade = event.getEntity();

        if (entidade.getType().isAlive()) {
            return;
        }

        int mytps = tps.lastTPS();

        if (mytps <= 14) {
            event.setCancelled(true);
            main.tpsLevel = 4;
            main.alert = "TPS " + mytps + " PlaceEntity: " + entidade;
            Utils.logToFile("Protection Manager", main.alert);
            return;
        }

        Location location = event.getBlock().getLocation();
        World world = location.getWorld();
        Entity[] listagem = location.getChunk().getEntities();

        if (main.totalMaxChunkEntities > 0) {

            int nearbyEntities = world.getNearbyEntities(location, 30, 200, 30).size();

            if (nearbyEntities > (main.totalMaxChunkEntities * 2.1) && entidade instanceof Minecart) {
                event.setCancelled(true);
                main.alert = location.getBlockX() + "::" + location.getBlockZ() + " count " + nearbyEntities + " (PlaceEntity) near Cart ";
                Utils.logToFile("Protection Manager", main.alert);
                return;
            }

            if (nearbyEntities > (main.totalMaxChunkEntities * 2.1) && entidade instanceof Boat) {
                event.setCancelled(true);
                main.alert = location.getBlockX() + "::" + location.getBlockZ() + " count " + nearbyEntities + " (PlaceEntity) near Boat " + event;
                Utils.logToFile("Protection Manager", main.alert);
                return;
            }

            int currentChunkEntities = listagem.length;

            if (currentChunkEntities > main.maxChunkEntities + 5) {
                event.setCancelled(true);
                main.alert = "maxChunk (PlaceEntity) " + entidade;
                Utils.logToFile("Protection Manager", main.alert);
                return;
            }

            if (mytps < 18) {
                int currentEntities = world.getEntities().size();

                if (currentEntities > main.maxEntities + 8) {
                    event.setCancelled(true);
                    main.tpsLevel = 4;
                    main.alert = "MaxEnt (PlaceEntity) " + entidade;
                    Utils.logToFile("Protection Manager", main.alert);
                    return;
                }
            }
        }
    }


    @EventHandler(ignoreCancelled = true)
    public void onFireworkExplodeEvent(FireworkExplodeEvent event) {  //limite tested 150+ with tps18
        int mytps = tps.lastTPS();
        int counter = 0;

        if (mytps >= 18) { //if stable
            return;
        }

        for (Entity entities : event.getEntity().getLocation().getChunk().getEntities()) {
            if (entities.getType().equals(EntityType.FIREWORK)) {
                counter++;

                if (counter > 15) entities.remove();
            }
        }

        if (counter > 15) {
            main.alert = "TPS " + mytps + " Controlling fireworks! " + counter;
            main.getLogger().info("TPS " + mytps + " Controlling fireworks! " + counter);
        }
    }


    @EventHandler(ignoreCancelled = true)
    public void onProcessBlockBreakEvent(BlockBreakEvent e) {
        if (!AntigriefProtection || e.getPlayer().isOp()) return;
        e.setCancelled(true);
    }


    @EventHandler(ignoreCancelled = true)
    public void onBlockPlaceEvent(BlockPlaceEvent e) {
        if (!AntigriefProtection || e.getPlayer().isOp()) return;
        e.setCancelled(true);
    }


    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerMoveEvent(@NotNull PlayerMoveEvent event) {
        Player player = event.getPlayer();
        double distance = event.getFrom().distance(event.getTo());
        double currentHeight = event.getTo().getY();
        double heightDiff = event.getFrom().getY() - currentHeight;

        if (main.hackProtection) {
            Block block = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
            Block block2 = player.getLocation().getBlock();
            PlayerStatus p = (PlayerStatus) main.playerControl.get(player);

            if (!player.isSneaking() && !player.isFlying() && !player.isGliding() && !(player.getVehicle() instanceof Horse) && block.getType().equals(Material.AIR) && !player.hasPotionEffect(PotionEffectType.LEVITATION)) { //jumping

                if (p.hight == 0) p.hight = currentHeight;

                if (p.hack && ((heightDiff < -1 && p.counter > 8) || p.counter >= 35 || ((p.hight - currentHeight) < -2.2) && p.counter >= 0)) {

                    //slab detection
                    if (!block2.getType().equals(Material.AIR)) {
                        if (block.getType().equals(Material.SLIME_BLOCK) || block2.equals(Material.SLIME_BLOCK)) {
                            p.counter = -25;
                        } else {
                            if (p.counter > 0) p.counter = 0;
                        }

                        p.hack = false;
                        p.hight = 0;
                        return;
                    }

                    Location location = block.getLocation();

                    for (double x = location.getX() - 1; x <= location.getX() + 1; x++) {
                        for (double z = location.getZ() - 1; z <= location.getZ() + 1; z++) {
                            Location loc = new Location(location.getWorld(), x, location.getBlockY(), z);
                            Block testBlock = loc.getBlock();

                            if (!testBlock.getType().equals(Material.AIR)) {
                                if (block.getType().equals(Material.SLIME_BLOCK) || block2.equals(Material.SLIME_BLOCK)) {
                                    p.counter = -25;
                                } else {
                                    if (p.counter > 0) p.counter = 0;
                                }

                                p.hack = false;
                                p.hight = 0;
                                return;
                            }
                        }
                    }

                    Utils.logToFile("Protection Manager", player.getName() + "   " + heightDiff + "    counter " + p.counter + "    " + (p.hight - currentHeight) + "    " + block2 + "   " + player.hasPotionEffect(PotionEffectType.LEVITATION));

                    double currentVecX = player.getVelocity().getX();
                    double currentVecZ = player.getVelocity().getZ();
                    double currentVecY = player.getVelocity().getY();

                    player.setVelocity(new Vector(currentVecX, currentVecY - 2, currentVecZ));

                    if (p.counter > 0) p.counter = 0;
                    p.hack = false;
                    p.hight = 0;
                    p.kick++;

                    if (p.kick == 2) {
                        player.kickPlayer("You are been warned " + player.getName() + " don't hack!");
                    }

                    if (p.kick == 4) {
                        player.banPlayer("Hacking in server");
                    }

                    return;

                } else if (heightDiff <= 0) { //if jumping and getting higher or flying
                    //@slab
                    if (!block2.getType().equals(Material.AIR)) {

                        if (block.getType().equals(Material.SLIME_BLOCK) || block2.equals(Material.SLIME_BLOCK)) {
                            p.counter = -25;
                        } else {
                            if (p.counter > 0) p.counter = 0;
                        }

                        p.hack = false;
                        p.hight = 0;
                        return;
                    } else {
                        p.hack = true;
                        p.counter++;
                    }
                }

            } else {

                if (block.getType().equals(Material.SLIME_BLOCK) || block2.equals(Material.SLIME_BLOCK)) {
                    p.counter = -25;
                } else {
                    if (p.counter > 0) p.counter = 0;
                }

                p.hack = false;
                p.hight = 0;
            }
        }

        if (main.totalMaxChunkEntities > 0) {
            if (player.isGliding()) {
                if (distance > main.maxSpeed && Math.abs(heightDiff) < 0.6) {
                    player.setVelocity(player.getVelocity().multiply(0.98));
                    return;
                }
            } else if (player.isFlying() && !player.isOp() && distance > 0.1 && player.getGameMode().equals(GameMode.SURVIVAL)) {
                player.setVelocity(player.getVelocity().multiply(0.5));
                return;

            } else if (distance > main.maxSpeed) {
                if (Math.abs(heightDiff) > 0.6) return;
                player.setVelocity(player.getVelocity().multiply(0.5)); //anti cheats
                return;
            }
        }

        if (main.speedProtection && player.getWalkSpeed() > (float) 0.2 && player.getActivePotionEffects().size() == 0) {
            player.setWalkSpeed((float) 0.2);
            Utils.logToFile("Protection Manager", player.getName() + " got its walk speed adjusted");
            main.alert = player.getName() + " got its walk speed reset";
            //max recommended 0.335
            return;
        }
    }


    @EventHandler(ignoreCancelled = true)
    public void onBlockDispenseEvent(BlockDispenseEvent e) {

        int mytps = tps.lastTPS();
        if (mytps >= 18) return;

        Material mat = e.getItem().getType();

        if (mat == Material.SPLASH_POTION) {
            main.alert = "Blocked dispenser splash potion";
            e.setCancelled(true);
        }
    }


    @EventHandler(ignoreCancelled = true)
    public void onThrow(PlayerInteractEvent event) {

        int mytps = tps.lastTPS();
        if (mytps >= 18) return;

        Action action = event.getAction();
        Player player = event.getPlayer();

        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            ItemStack it = player.getInventory().getItemInMainHand();
            if (it.getType() == Material.SPLASH_POTION) {
                main.alert = "Blocked thrown splash potion";
                event.setCancelled(true);
            }
        }
    }


    @EventHandler
    public void onBlockRedstoneEvent(BlockRedstoneEvent event) {

        int mytps = tps.lastTPS();
        if (mytps >= 18) return;

        int penalty = 2; //valor para entre 17 e 18
        if (mytps < 18) penalty = 5;
        if (mytps < 14) penalty = 20;

        Block blo = event.getBlock();
        Material mat = blo.getType();
        Location loc = blo.getLocation();

        if (tps.redStoneBlockComponents.get(loc) == null) {
            tps.redStoneBlockComponents.put(loc, 1);
        } else {
            return; //already registered
        }

        Chunk chk = loc.getWorld().getChunkAt(loc);
        Integer counter = tps.redStoneChunk.get(chk);

        if ((mat == Material.COMPARATOR || mat == Material.OBSERVER || mat == Material.REPEATER) && event.getNewCurrent() == 15) {

            if (tps.redStoneObjs < (main.maxRedstone - penalty)) {
                tps.redStoneObjs++;
            } else if (tps.redStoneObjs == (main.maxRedstone - penalty)) {
                main.alert = "Adjusting redstone to limit " + tps.redStoneObjs;
                Utils.logToFile("Protection Manager", mat + "   " + main.alert);
                tps.redStoneObjs++;
                event.setNewCurrent(0);
            } else {
                event.setNewCurrent(0);
            }

            if (counter == null) {
                tps.redStoneChunk.put(chk, 1);
            } else {
                if (counter == main.maxRedstoneChunk) {
                    main.alert = "Redstone chunk limit of " + counter + " at chunk " + chk;
                    Utils.logToFile("Protection Manager", mat + " - chunk - " + main.alert);
                    event.setNewCurrent(0);
                } else if (counter > main.maxRedstoneChunk) {
                    event.setNewCurrent(0);
                }

                tps.redStoneChunk.replace(chk, counter + 1);
            }
        }

    }

}
