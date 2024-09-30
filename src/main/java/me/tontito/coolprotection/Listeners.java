package me.tontito.coolprotection;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Levelled;
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

import java.util.Iterator;
import java.util.Map;

import static org.bukkit.Bukkit.getServer;
import static org.bukkit.Material.WIND_CHARGE;
import static org.bukkit.entity.SpawnCategory.MONSTER;


public class Listeners implements Listener {

    protected int maxWithers = 8;
    protected int maxLighting = 4;
    private final TpsCheck tps;
    private final Main main;

    public Listeners(Main main, TpsCheck tps) {
        this.main = main;
        this.tps = tps;

        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerStatus p = new PlayerStatus(main, player);
            p.speed = player.getWalkSpeed();

            main.playerControl.put(player.getUniqueId().toString(), p);
        }
    }


    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();

        PlayerStatus pls = main.playerControl.get(player.getUniqueId().toString());

        if (pls.kick > 2) {
            event.setCancelled(true);
            return;
        }

        if (event.getMessage().startsWith("/fill") || event.getMessage().toUpperCase().contains("TNT")) {

            if (!player.hasPermission("coolprotection.admin")) {

                //must be before register or will count twice at start
                if (pls.applyPenalty(true)) {
                    main.alert = "Blocked commands";
                    Utils.logToFile("Protection Manager", player.getName() + " had commands blocked");
                    event.setCancelled(true);
                    return;
                }

                if (main.dinamicHackProtection) {
                    pls.registerSusList(PlayerStatus.ActionCodes.commands, 60, 3);
                }

                event.setCancelled(true);
                player.sendMessage("You dont have permission");
            }
        }

        if (event.getMessage().startsWith("/execute as ")) {

            if (!player.hasPermission("coolprotection.admin")) {

                //must be before register or will count twice at start
                if (pls.applyPenalty(true)) {
                    main.alert = "Blocked commands";
                    Utils.logToFile("Protection Manager", player.getName() + " had commands blocked");
                    event.setCancelled(true);
                    return;
                }

                if (main.dinamicHackProtection) {
                    pls.registerSusList(PlayerStatus.ActionCodes.commands, 60, 3);
                }

                event.setCancelled(true);
                player.sendMessage("You dont have permission");
            }
        }
    }


    @EventHandler(ignoreCancelled = true)
    public void onPlayerChat(@NotNull AsyncPlayerChatEvent chat) {

        Player player = chat.getPlayer();
        PlayerStatus pls = main.playerControl.get(player.getUniqueId().toString());

        if (pls.kick > 2) {
            return;
        }

        String message = chat.getMessage().toLowerCase();

        if (message.equalsIgnoreCase("!lagmeter on") && (player.hasPermission("coolprotection.admin") || player.hasPermission("coolprotection.monitor"))) {
            tps.registerScoreBoards(chat.getPlayer());
            chat.setCancelled(true);

        } else if (message.equalsIgnoreCase("!lagmeter off") && (player.hasPermission("coolprotection.admin") || player.hasPermission("coolprotection.monitor"))) {
            tps.deleteScoreBoards(chat.getPlayer());
            chat.setCancelled(true);

        } else if (message.startsWith("!maxchunk ") && player.hasPermission("coolprotection.admin")) {

            if (message.split(" ").length > 1) {
                String valor = message.split(" ")[1];

                if (StringUtils.isNumeric(valor)) {
                    FileConfiguration config = main.getConfig();
                    config.set("maxChunkEntities", Integer.parseInt(valor));
                    main.saveConfig();
                    main.totalMaxChunkEntities = Integer.parseInt(valor);
                    player.sendRawMessage("Max chunkEntity value changed to " + valor + "  prev Game limits: " + main.getServer().getWorld("world").getSpawnLimit(MONSTER));

                    if (Integer.parseInt(valor) < 10 || Integer.parseInt(valor) > 200)
                        valor = "70"; //game defaults 70 from bukkit.yml

                    final int chunckLimit = Integer.parseInt(valor);
                    main.myBukkit.runTask(null, null, null, () -> main.setWorldConfigs(chunckLimit));

                    chat.setCancelled(true);
                }
            }
        } else if (message.equalsIgnoreCase("!antigrief on") && player.hasPermission("coolprotection.admin")) {
            main.AntigriefProtection = true;
            player.sendRawMessage("Enabled antigrief");
            chat.setCancelled(true);

        } else if (message.equalsIgnoreCase("!antigrief off") && player.hasPermission("coolprotection.admin")) {
            main.AntigriefProtection = false;
            player.sendRawMessage("Disabled antigrief");
            chat.setCancelled(true);

        } else if (message.equalsIgnoreCase("!autoBalance off") && player.hasPermission("coolprotection.admin")) {
            if (main.tpsProtection) {
                main.tpsProtection = false;

                FileConfiguration config = main.getConfig();
                config.set("tpsProtection", main.tpsProtection);
                main.saveConfig();

                player.sendRawMessage("Disabled adaptative balancing");
            }
            chat.setCancelled(true);
        } else if (message.equalsIgnoreCase("!autoBalance on") && player.hasPermission("coolprotection.admin")) {
            if (!main.tpsProtection) {
                main.tpsProtection = true;

                FileConfiguration config = main.getConfig();
                config.set("tpsProtection", main.tpsProtection);
                main.saveConfig();

                player.sendRawMessage("Enabled adaptative balancing");
                player.sendRawMessage(("Current values are TPS: " + tps.lastTPS() + " Optimal Max LivingEntities: " + main.maxLiving + "  maxEntities: " + main.maxEntities + "  maxChunkEntities: " + main.maxChunkEntities));
                chat.setCancelled(true);
            }
        } else if (message.equalsIgnoreCase("!emergency on") && player.hasPermission("coolprotection.admin")) {
            main.tpsProtection = true;
            main.AntigriefProtection = true;
            main.speedProtection = true;
            main.hackProtection = true;
            main.ExplodeProtection = true;
            main.WitherProtection = true;
            main.antiChatReport = true;
            main.autoShutdown = false;
            main.Emergency = true;
            main.totalMaxChunkEntities = 30; //safe default
            main.myBukkit.runTask(null, null, null, () -> main.setWorldConfigs(main.totalMaxChunkEntities));
            player.sendRawMessage("Enabled emergency mode, all protections temporarily on!");
            tps.registerScoreBoards(chat.getPlayer());
            chat.setCancelled(true);

        } else if (message.equalsIgnoreCase("!emergency off") && player.hasPermission("coolprotection.admin")) {
            main.LoadSettings();
            main.Emergency = false;
            player.sendRawMessage("Disabled emergency mode, reloaded configs!");
            tps.registerScoreBoards(chat.getPlayer());
            chat.setCancelled(true);
        }
    }


    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChatReportPrevent(@NotNull AsyncPlayerChatEvent e) {

        if (!main.antiChatReport) return;

        e.setCancelled(true);
        Player player = e.getPlayer();

        PlayerStatus pls = main.playerControl.get(player.getUniqueId().toString());
        ChatColor extra = ChatColor.RED;

        if (pls.kick > 2) {
            extra = ChatColor.RED;
        }

        String message = "<" + extra + player.getName() + ChatColor.WHITE + "> " + e.getMessage();

        main.myBukkit.runTaskLater(null, null, null, () -> {
            Utils.logToFile("Chat Manager", message);
        }, 1);

        Iterator var8 = Bukkit.getOnlinePlayers().iterator();

        while (var8.hasNext()) {
            Player onlinePlayer = (Player) var8.next();
            onlinePlayer.sendMessage(message);
        }
    }


    @EventHandler
    public void onPlayerJoinEvent(@NotNull PlayerJoinEvent event) {

        Player player = event.getPlayer();

        if (main.playerControl.get(player.getUniqueId().toString()) == null) {
            PlayerStatus p = new PlayerStatus(main, player);
            p.speed = player.getWalkSpeed();

            main.playerControl.put(player.getUniqueId().toString(), p);
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

        Location location = event.getLocation();

        if (main.serverVersion == 8 && !main.myBukkit.isOwnedby(null, location, null)) return;

        if (main.tpsProtection) {
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

        int level = location.getBlockY();
        String mundo = location.getWorld().getName();


        if (event.getEntityType() == EntityType.TNT && level > main.ExplosionLevel) {

            Entity causer = ((TNTPrimed) event.getEntity()).getSource();

            if (causer instanceof Player && main.playerControl.get(causer.getUniqueId().toString()) != null) {
                Player player = ((Player) causer);

                PlayerStatus play = main.playerControl.get(player.getUniqueId().toString());
                play.speed = play.speed - 0.02f;

                player.setWalkSpeed(play.speed);
                main.getLogger().info("Slowing player " + player.getName());
            }

            getServer().broadcastMessage(" No TNT party here!");
            event.setCancelled(true);
            return;

        } else if (event.getEntityType() == EntityType.TNT_MINECART && level > main.ExplosionLevel) {

            getServer().broadcastMessage(" No TNT party here!!");
            event.setCancelled(true);
            return;
        }

        if (event.getEntityType() == EntityType.END_CRYSTAL && level > main.ExplosionLevel && !(mundo.endsWith("_nether") || mundo.endsWith("_end"))) {
            getServer().broadcastMessage(" No end crystals here!");
            event.setCancelled(true);
        }
    }


    @EventHandler(ignoreCancelled = true)
    public void onLightningSpawn(LightningStrikeEvent event) {

        if (!main.tpsProtection) return;

        Location location = event.getLightning().getLocation();

        if (main.serverVersion == 8 && !main.myBukkit.isOwnedby(null, location, null)) return;

        int mytps = tps.lastTPS();

        if (mytps <= 14) {
            event.setCancelled(true);

            if (main.tpsLevel != 2) {
                main.tpsLevel = 2;
                Utils.logToFile("Protection Manager", "TPS " + tps.lastTPS() + "(Light)Very Low tps " + event.getCause());
            }
            return;
        }

        if (maxLighting > 0) {
            int counter = 0;

            for (Entity entities : location.getChunk().getEntities()) {
                if (entities.getType().equals(EntityType.LIGHTNING_BOLT)) {
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

        EntityType entidade = event.getEntityType();
        Location location = event.getLocation();

        if (main.serverVersion == 8 && !main.myBukkit.isOwnedby(null, location, null)) return;

        World world = location.getWorld();
        World.Environment mundo = world.getEnvironment();
        int yLevel = location.getBlockY();
        Entity[] listagem = location.getChunk().getEntities();

        if (main.tpsProtection) {

            if (main.tpsLevel == 2) { //refusal mode for 1 second, if tps is lower
                event.setCancelled(true);
                return;
            }

            int mytps = tps.lastTPS();

            if (mytps <= 14) {
                event.setCancelled(true);
                main.tpsLevel = 2;
                main.alert = "TPS " + mytps + " Spawn Mob: " + entidade;
                Utils.logToFile("Protection Manager", main.alert);
                return;
            }

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

        if (!main.tpsProtection) return;

        if (main.tpsLevel == 2) { //refusal mode for 1 second, if tps is lower
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
            main.tpsLevel = 2;
            main.alert = "TPS " + mytps + " SpawnEntity: " + entidade;
            Utils.logToFile("Protection Manager", main.alert);
            return;
        }

        Location location = event.getLocation();

        if (main.serverVersion == 8 && !main.myBukkit.isOwnedby(null, location, null)) return;

        World world = location.getWorld();

        if (main.totalMaxChunkEntities > 0) {
            int nearbyEntities = world.getNearbyEntities(location, 30, 200, 30).size();

            if (nearbyEntities > (main.totalMaxChunkEntities * 3)) {
                event.setCancelled(true);
                main.alert = "Near " + location.getBlockX() + "::" + location.getBlockZ() + " count " + nearbyEntities + " (SpawnEntity) blocking during 1 sec";
                Utils.logToFile("Protection Manager", main.alert);
                main.tpsLevel = 2; //to avoid multiple blocks and writes @log
            }
        }
    }


    @EventHandler(ignoreCancelled = true)
    public void onEntityPlaceEvent(@NotNull EntityPlaceEvent event) {

        if (!main.tpsProtection) return;

        if (main.tpsLevel == 2) { //refusal mode for 1 second
            event.setCancelled(true);
            return;
        }

        Entity entidade = event.getEntity();

        if (main.serverVersion == 8 && !main.myBukkit.isOwnedby(entidade, null, null)) return;

        if (entidade.getType().isAlive()) {
            return;
        }

        int mytps = tps.lastTPS();

        if (mytps <= 14) {
            event.setCancelled(true);
            main.tpsLevel = 2;
            main.alert = "TPS " + mytps + " PlaceEntity: " + entidade;
            Utils.logToFile("Protection Manager", main.alert);
            return;
        }

        Location location = event.getBlock().getLocation();
        World world = location.getWorld();
        Entity[] listagem = location.getChunk().getEntities();

        if (main.totalMaxChunkEntities > 0) {

            int nearbyEntities = location.getNearbyEntities(30, 200, 30).size();

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
                    main.tpsLevel = 2;
                    main.alert = "MaxEnt (PlaceEntity) " + entidade;
                    Utils.logToFile("Protection Manager", main.alert);
                }
            }
        }
    }


    @EventHandler(ignoreCancelled = true)
    public void onFireworkExplodeEvent(FireworkExplodeEvent event) {  //limit tested 150+ with tps18

        if (!main.tpsProtection) return;

        Location location = event.getEntity().getLocation();

        if (main.serverVersion == 8 && !main.myBukkit.isOwnedby(null, location, null)) return;

        int mytps = tps.lastTPS();
        int counter = 0;

        if (mytps >= 18) { //if stable
            return;
        }

        for (Entity entities : location.getChunk().getEntities()) {
            if (entities.getType().equals(EntityType.FIREWORK_ROCKET)) {
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
        Player player = e.getPlayer();
        PlayerStatus pls = main.playerControl.get(player.getUniqueId().toString());

        if (pls.applyPenalty(false)) {
            e.setCancelled(true);
            return;
        }

        if (!main.AntigriefProtection || player.hasPermission("coolprotection.admin") || player.hasPermission("coolprotection.monitor"))
            return;
        e.setCancelled(true);
    }


    @EventHandler(ignoreCancelled = true)
    public void onBlockPlaceEvent(BlockPlaceEvent e) {
        Player player = e.getPlayer();
        PlayerStatus pls = main.playerControl.get(player.getUniqueId().toString());

        if (pls.applyPenalty(false)) {
            e.setCancelled(true);
            return;
        }

        if (!main.AntigriefProtection || player.hasPermission("coolprotection.admin") || player.hasPermission("coolprotection.monitor"))
            return;

        e.setCancelled(true);
    }


    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerMoveEvent(@NotNull PlayerMoveEvent event) {

        Player player = event.getPlayer();

        if (!player.getGameMode().equals(GameMode.SURVIVAL)) {
            return;
        }

        double currentHeight = event.getTo().getY();
        double heightDiff = event.getFrom().getY() - currentHeight;

        if (main.hackProtection) {
            Block block = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
            Block block2 = player.getLocation().getBlock();
            PlayerStatus p = main.playerControl.get(player.getUniqueId().toString());

            if (!player.isSneaking() && !player.isFlying() && !player.isGliding() && !(player.getVehicle() instanceof Horse) && block.getType().equals(Material.AIR) && !player.hasPotionEffect(PotionEffectType.LEVITATION)) { //jumping

                ItemStack it = player.getInventory().getItemInMainHand();

                if ((it.getType() == Material.TRIDENT && it.containsEnchantment(Enchantment.RIPTIDE)) || it.getType() == WIND_CHARGE || (it.getType() == Material.MACE && it.containsEnchantment(Enchantment.WIND_BURST))) {
                    p.counter = -25;
                    return;
                }

                ItemStack it2 = player.getInventory().getItemInOffHand();

                if ((it2.getType() == Material.TRIDENT && it2.containsEnchantment(Enchantment.RIPTIDE)) || it2.getType() == WIND_CHARGE || (it2.getType() == Material.MACE && it2.containsEnchantment(Enchantment.WIND_BURST))) {
                    p.counter = -25;
                    return;
                }

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

                    Utils.logToFile("Protection Manager debug", player.getName() + "   " + it + "     " + it2 + "    " + it.containsEnchantment(Enchantment.RIPTIDE) + "    " + it2.containsEnchantment(Enchantment.RIPTIDE));
                    Utils.logToFile("Protection Manager debug", player.getName() + "   " + heightDiff + "    counter " + p.counter + "    " + (p.hight - currentHeight) + "    " + block2 + "   " + player.hasPotionEffect(PotionEffectType.LEVITATION));

                    double currentVecX = player.getVelocity().getX();
                    double currentVecZ = player.getVelocity().getZ();
                    double currentVecY = player.getVelocity().getY();

                    player.setVelocity(new Vector(currentVecX, currentVecY - 2, currentVecZ));

                    if (p.counter > 0) p.counter = 0;
                    p.hack = false;
                    p.hight = 0;

                    //will self increment after kick >= 1
                    p.applyPenalty((p.kick >= 1));

                    //initial trigger, after applyPenalty so that it doesnt inc twice for 0
                    if (p.kick < 1) p.kick++;

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

        if (main.tpsProtection) {
            double distance = event.getFrom().distance(event.getTo());

            if (player.isGliding()) {
                if (distance > main.maxSpeed && Math.abs(heightDiff) < 0.6) {
                    player.setVelocity(player.getVelocity().multiply(0.98));
                }
            } else if (player.isFlying() && distance > 0.1) {
                player.setVelocity(player.getVelocity().multiply(0.5));

            } else if (distance > main.maxSpeed && player.getActivePotionEffects().size() == 0) {
                if (Math.abs(heightDiff) > 0.6) return;
                player.setVelocity(player.getVelocity().multiply(0.5)); //anti cheats
            }
        }
    }


    @EventHandler(ignoreCancelled = true)
    public void onThrow(PlayerInteractEvent event) {

        Player player = event.getPlayer();
        PlayerStatus pls = main.playerControl.get(player.getUniqueId().toString());

        if (pls.applyPenalty(false)) {
            event.setCancelled(true);
            return;
        }

        if (!main.tpsProtection) return;

        int mytps = tps.lastTPS();
        if (mytps >= 18) return;

        Action action = event.getAction();

        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            ItemStack it = player.getInventory().getItemInMainHand();
            if (it.getType() == Material.SPLASH_POTION) {
                main.alert = "Blocked thrown splash potion";
                event.setCancelled(true);
            }
        }
    }


    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onFlintAndSteel(PlayerInteractEvent event) {

        Action action = event.getAction();

        if (action == Action.RIGHT_CLICK_BLOCK && event.getItem() != null && event.getItem().getType() == Material.FLINT_AND_STEEL) {
            Player player = event.getPlayer();
            PlayerStatus pls = main.playerControl.get(player.getUniqueId().toString());

            //must be before register or will count twice at start
            if (pls.applyPenalty(true)) {
                main.alert = "Blocked flintAndSteel fire";
                Utils.logToFile("Protection Manager", player.getName() + " had flint and steel blocked");
                event.setCancelled(true);
            }

            //3 flints in less then 60s, sus...
            if (main.dinamicHackProtection) {
                pls.registerSusList(PlayerStatus.ActionCodes.flint, 60, 3);
            }

        }

    }


    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerGriefAtempt(PlayerBucketEmptyEvent event) {

        Player player = event.getPlayer();
        PlayerStatus pls = main.playerControl.get(player.getUniqueId().toString());

        if (event.getBucket() == Material.WATER_BUCKET) {

            if (pls.applyPenalty(false)) {
                event.setCancelled(true);
            }

        } else if (event.getBucket() == Material.LAVA_BUCKET) {

            //must be before register or will count twice at start
            if (pls.applyPenalty(true)) {
                main.alert = "Blocked Lava Bucket";
                Utils.logToFile("Protection Manager", player.getName() + " had lava bucket blocked");
                event.setCancelled(true);
            }

            //3 lava Buckets in less then 60s, sus...
            if (main.dinamicHackProtection) {
                pls.registerSusList(PlayerStatus.ActionCodes.lavaBucket, 60, 3);
            }
        }
    }


    @EventHandler(ignoreCancelled = true)
    public void onEntityHit(EntityDamageByEntityEvent event) {

        if (event.getDamager() instanceof Player && (event.getEntity() instanceof Villager)) {

            Player player = (Player) event.getDamager();

            PlayerStatus pls = main.playerControl.get(player.getUniqueId().toString());

            //must be before register or will count twice at start
            if (pls.applyPenalty(true)) {
                main.alert = "Blocked Villager damage";
                Utils.logToFile("Protection Manager", player.getName() + " had Villager damage blocked");
                event.setCancelled(true);
            }

            if (main.dinamicHackProtection) {
                pls.registerSusList(PlayerStatus.ActionCodes.villagers, 90, 3);
            }
        }
    }


    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockDispenseEvent(BlockDispenseEvent e) {

        if (!main.tpsProtection) return;

        int mytps = tps.lastTPS();
        if (mytps >= 18) return;

        Material mat = e.getItem().getType();

        if (mat == Material.SPLASH_POTION) {
            main.alert = "Blocked dispenser splash potion";
            e.setCancelled(true);
        }
    }


    @EventHandler(ignoreCancelled = true)
    public void onBlockRedstoneEvent(BlockRedstoneEvent event) {

        if (!main.tpsProtection) return;

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


    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPhysicsv2(BlockPhysicsEvent event) {

        if (!main.tpsProtection) return;

        if (main.serverVersion == 2 || main.serverVersion == 3) return;

        int mytps = tps.lastTPS();
        if (mytps >= 13) return; //less them 13 tps will trigger this analysis

        long global = -1;
        Long testGlobal = main.chunkWater.get(global);

        if (testGlobal == null) {
            main.chunkWater.put(global, (long) 1);
        } else {
            main.chunkWater.replace(global, (testGlobal + 1));
        }

        long time = -4;
        Long testtime = main.chunkWater.get(time);

        //max 5ms to process these events each tick if low tps triggered
        if (testtime != null && testtime > 5000000) {
            long overtime = -5;
            Long testproce = main.chunkWater.get(overtime);

            if (testproce == null) {
                main.chunkWater.put(overtime, (long) 1);
            } else {
                main.chunkWater.replace(overtime, (testproce + 1));
            }
            return;
        }

        Block block = event.getBlock();

        if (block.getType() == Material.WATER) {
            long mytime = System.nanoTime();

            synchronized (this) {

                long chunk = block.getChunk().getChunkKey();

                Long testChunk = main.chunkWater.get(chunk);

                if (testChunk == null) {
                    testChunk = (long) 1;
                    main.chunkWater.put(chunk, (long) 1);
                } else {
                    main.chunkWater.replace(chunk, (testChunk + 1));
                }

                long filter = -2;

                if (!registerBlock(block)) {
                    //if already existed, filters and inc the filter counter
                    Long testfilter = main.chunkWater.get(filter);

                    if (testfilter == null) {
                        main.chunkWater.put(filter, (long) 1);
                    } else {
                        main.chunkWater.replace(filter, (testfilter + 1));
                    }
                    return;
                }

                //limit of 200 for each chunk
                if (testChunk > 50) {

                    long processed = -3;
                    Long testproce = main.chunkWater.get(processed);

                    if (testproce == null) {
                        main.chunkWater.put(processed, (long) 1);
                    } else {
                        main.chunkWater.replace(processed, (testproce + 1));
                    }

                    Location loc = block.getLocation();
                    int jumpy = 0;

                    for (int i = 1; i < 350; i++) {
                        if (jumpy > 4) {

                            testtime = main.chunkWater.get(time);

                            if (testtime == null) {
                                main.chunkWater.put(time, (System.nanoTime() - mytime));
                            } else {
                                main.chunkWater.replace(time, (testtime + (System.nanoTime() - mytime)));
                            }

                            return;
                        }

                        Location newLoc = loc.add(0, 1, 0); //incrementa 1 ao existente em loop
                        block = newLoc.getBlock();

                        if (registerBlock(block) && block.getType() == Material.WATER) {

                            int level = ((Levelled) block.getBlockData()).getLevel();

                            if (level == 0) {
                                block.setType(Material.AIR);

                                testtime = main.chunkWater.get(time);

                                if (testtime == null) {
                                    main.chunkWater.put(time, (System.nanoTime() - mytime));
                                } else {
                                    main.chunkWater.replace(time, (testtime + (System.nanoTime() - mytime)));
                                }

                                return; //game over mf
                            }
                            if (level == 8) {
                                block.setType(Material.AIR);
                            }

                            jumpy = 0;
                        } else
                            jumpy++;
                    }
                }

            }
        }
    }


    private boolean registerBlock(Block block) {
        long KeyCoord = Math.abs((10000 * block.getX())) + Math.abs((100 * block.getY())) + Math.abs(block.getZ());

        Long testCoord = main.chunkWater.get(KeyCoord);

        if (testCoord != null) {
            return false;

        } else {
            main.chunkWater.put(KeyCoord, (long) 1);
            return true;
        }
    }


    ////test code
//    @EventHandler(ignoreCancelled = true)
//    public void onPlayerDropItemEvent(PlayerDropItemEvent event) {  //limit tested 150+ with tps18
//        event.getPlayer().sendMessage("dropping stuff");
//
//        Vector vel = new Vector(0, 1, 0);
//
//        event.getPlayer().setVelocity(vel);
//
//        main.myBukkit.runTaskLater(event.getPlayer(), null, null, () -> {
//            Vector vel1 = new Vector(0, 0, 0);
//            event.getPlayer().setVelocity(vel1);
//        }, 30);
//
//    }


}
