package me.tontito.coolprotection;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;


import static org.bukkit.Bukkit.isOwnedByCurrentRegion;

public class MyBukkit {
    private int myVersion = 0;
    private Main main;

    public MyBukkit(Main main, int version) {
        myVersion = version;
        this.main = main;
    }


    public void Run(Player player, Runnable myrun, int delay) {
        if (myVersion == 8) {
            if (player != null) player.getScheduler().run(main, st -> myrun.run(), null);
            else main.getServer().getGlobalRegionScheduler().run(main, st -> myrun.run());
        } else  {
            org.bukkit.Bukkit.getScheduler().runTaskLater(main, st -> myrun.run(), delay);
        }
    }


    public boolean allowContinue(Entity entity, Location local, Block block) {

        if (myVersion == 8) {
            if (entity != null) return isOwnedByCurrentRegion(entity);
            else if (local != null) return isOwnedByCurrentRegion(local);
            else if (block != null) return isOwnedByCurrentRegion(block);
        }

        return true;
    }

}
