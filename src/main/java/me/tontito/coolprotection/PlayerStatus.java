package me.tontito.coolprotection;


import java.util.Date;
import java.util.LinkedList;

public class PlayerStatus {
    public boolean hack = false;
    public int counter = 0;
    public double hight = 0;
    public int kick = 0;
    public float speed = 0.2f;
    public LinkedList<Sus> suspicious = new LinkedList();

    public enum ActionCodes {
        flint,
        explosion,
        destroy,
        spam,
        language;
    }

    public PlayerStatus() {
    }

    public class Sus {
        public Date eventTime;
        public ActionCodes enume;

    }
}

