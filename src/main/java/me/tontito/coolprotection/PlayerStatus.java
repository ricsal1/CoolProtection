package me.tontito.coolprotection;


import fr.mrmicky.fastboard.FastBoard;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.util.Hashtable;

public class PlayerStatus {
    public boolean hack = false;
    public int counter = 0;
    public double hight = 0;
    public int kick = 0;
    public float speed = 0.2f;
    private Hashtable<ActionCodes, Sus> suspicious = new Hashtable();
    private FastBoard board;
    private Main main;
    private Player player;

    public PlayerStatus(Main main, Player player) {
        this.main = main;
        this.player = player;
    }


    public void setBoard(FastBoard board) {
        this.board = board;
    }


    public FastBoard returnBoard() {
        return board;
    }


    public boolean registerSusList(ActionCodes code, int timeLimit, int countLimit) {
        Sus mysus = suspicious.get(code);

        if (mysus != null) {
            if (((System.currentTimeMillis() / 1000) - mysus.eventTime) < timeLimit) {
                mysus.counter++;
            } else {
                mysus.counter = 1;
                mysus.eventTime = (System.currentTimeMillis() / 1000);
            }

            if (mysus.counter >= countLimit) {
                if (kick == 0) {
                    kick = 1;
                    player.sendMessage("You have entered on verification list, make sure you are just playing");

                    for (Player mplayer : Bukkit.getOnlinePlayers()) {
                        if (mplayer.hasPermission("coolprotection.admin")) {
                            mplayer.sendMessage("System: Player " + player.getName() + " has entered on suspicious verification list due to usage of " + code);
                        }
                    }
                }
                return true;
            }
            return false;
        } else {
            mysus = new Sus();
            mysus.counter = 1;
            mysus.eventTime = (System.currentTimeMillis() / 1000);
            suspicious.put(code, mysus);
        }

        return false;
    }


    /**
     * Applies penalties and returns is event should be canceled
     *
     * @param increment
     * @returnif operation should be blocked
     */
    public boolean applyPenalty(Boolean increment) {

        if (kick == 0) return false;

        //if it was just a check, then return
        if (kick >= 2 && !increment) {
            return true;
        }

        //if already under observation, and with inc order
        if (kick > 0 && increment) {

            if (speed > 0) {
                speed = speed - 0.02f;
                player.setWalkSpeed(speed);
            }

            kick++;
        }

        if (kick == 2) {
            player.kickPlayer("You are been warned " + player.getName() + " don't hack/grief!");
            return true;

        } else if (kick > 2) {

            if (kick == 8) {

                if (main.dinamicHackProtection && (player.hasPermission("coolprotection.admin") || player.hasPermission("coolprotection.monitor"))) {
                    PermissionAttachment pa = player.addAttachment(main);
                    for (String pe : pa.getPermissions().keySet()) {
                        pa.setPermission(pe, false);
                    }

                    for (Player mplayer : Bukkit.getOnlinePlayers()) {
                        if (mplayer.hasPermission("coolprotection.admin")) {
                            mplayer.sendMessage("System: Player " + player.getName() + " got all his permissions removed!");
                        }
                    }
                }

                main.myBukkit.runTaskTimer(player, null, null, () -> {
                    player.getWorld().strikeLightningEffect(player.getLocation());
                }, 1, 400);

                player.kickPlayer("Last warning " + player.getName() + "!");
            }
            if (kick == 10) {
                player.banPlayer("You have been warned " + player.getName() + ", goodbye!");
            }

            return true;
        }

        //should trigger only for 1
        return false;
    }


    public enum ActionCodes {
        flint,
        lavaBucket,
        commands,
        explosion,
        destroy,
        spam,
        language,
        villagers
    }

    protected class Sus {
        public long eventTime = 0;
        public int counter = 0;
    }
}

