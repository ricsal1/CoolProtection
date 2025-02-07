package me.tontito.coolprotection;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class Utils {
    private static Main main;

    public static void SetMain(Main mymain) {
        main = mymain;
    }


    public static void logToFile(String name, String message) {
        logToFile(name, message, true, true);
    }


    public static void logToFile(String name, String message, boolean newline, boolean timestamp) {
        try {
            File dataFolder = main.getDataFolder();

            if (!dataFolder.exists()) {
                dataFolder.mkdir();
            }

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd"); // Set the Time Format
            LocalDateTime now = LocalDateTime.now(); // Get the time
            File saveTo = new File(main.getDataFolder(), dtf.format(now) + " " + name + ".txt");

            if (!saveTo.exists()) {
                saveTo.createNewFile();
            }

            if (timestamp) {
                dtf = DateTimeFormatter.ofPattern("HH:mm:ss"); // Set the Time Format
                now = LocalDateTime.now(); // Get the time
                message = dtf.format(now) + ": " + message;
            }

            FileWriter fw = new FileWriter(saveTo, true);
            PrintWriter pw = new PrintWriter(fw);

            if (newline) pw.println(message);
            else pw.print(message);

            pw.flush();
            pw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static double round(double num, int digits) {
        // epsilon correction
        double n = Double.longBitsToDouble(Double.doubleToLongBits(num) + 1);
        double p = Math.pow(10, digits);
        return Math.round(n * p) / p;
    }


    public static int checkGreater(String v1, String v2) {
        int counter = v1.split("\\.").length;

        if (counter > v2.split("\\.").length) v2 = v2 + ".0";
        if (counter < v2.split("\\.").length) {
            v1 = v1 + ".0";
            counter++;
        }

        for (int k = 0; k < counter; k++) {
            try {
                if (Integer.parseInt(v1.split("\\.")[k]) > Integer.parseInt(v2.split("\\.")[k])) {
                    return -1;
                } else if (Integer.parseInt(v1.split("\\.")[k]) < Integer.parseInt(v2.split("\\.")[k])) {
                    return 1;
                } else {
                    //next loop
                }
            } catch (Exception e) {
                return -2;
            }
        }
        return 0;//same version
    }

 }
