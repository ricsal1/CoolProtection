package me.tontito.coolprotection;


import fr.mrmicky.fastboard.FastBoard;

import java.util.Date;
import java.util.LinkedList;

public class PlayerStatus {
    public boolean hack = false;
    public int counter = 0;
    public double hight = 0;
    public int kick = 0;
    public float speed = 0.2f;
    public LinkedList<Sus> suspicious = new LinkedList();
    private FastBoard board;

    public PlayerStatus() {
    }

    public void setBoard(FastBoard board) {
        this.board = board;
    }

    public FastBoard returnBoard() {
        return board;
    }


    public enum ActionCodes {
        flint,
        explosion,
        destroy,
        spam,
        language;
    }

    public class Sus {
        public Date eventTime;
        public ActionCodes enume;

    }
}

