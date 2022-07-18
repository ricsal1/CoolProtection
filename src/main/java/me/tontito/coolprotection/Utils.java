package me.tontito.coolprotection;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class Utils {
   private static Main main;

    public static void SetMain(Main mymain) {
     main = mymain;
    }


    public static void logToFile(String name, String message) {
        logToFile(name,message,true,true);
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


//            main.getServer().getScheduler().runTaskAsynchronously(main, () -> {
//                try {
//                    Utils.getScreenshot();
//                } catch (Exception e) {
//                    Utils.logToFile("Ticks Logger_debug",e.getMessage());
//                }
//            }, 0L);

    public static void getScreenshot() throws Exception {
        Rectangle rec = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        Robot robot = new Robot();
        BufferedImage img = robot.createScreenCapture(rec);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH_mm_ss"); // Set the Time Format
        LocalDateTime now = LocalDateTime.now(); // Get the time
        File saveTo = new File(main.getDataFolder(), dtf.format(now) + "imagem.jpg");

        ImageIO.write(img, "jpg", saveTo);
    }


    public static double round(double num, int digits) {

        // epsilon correction
        double n = Double.longBitsToDouble(Double.doubleToLongBits(num) + 1);
        double p = Math.pow(10, digits);
        return Math.round(n * p) / p;
    }


    public static String getCountry(String ip) throws Exception {

        URL url = new URL("http://ip-api.com/json/" + ip+ "?fields=country");

        BufferedReader stream = new BufferedReader(new InputStreamReader(url.openStream()));
        StringBuilder entirePage = new StringBuilder();
        String inputLine;

        while ((inputLine = stream.readLine()) != null)
            entirePage.append(inputLine);

        stream.close();

        if (!(entirePage.toString().contains("\"country\":\"")))
            return null;

        return entirePage.toString().split("\"country\":\"")[1].split("\",")[0].split("\"}")[0];
    }

}
