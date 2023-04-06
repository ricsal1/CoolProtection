package me.tontito.coolprotection;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker extends BukkitRunnable {

    private final Main main;
    private final String project;
    private final String projectName;

    public UpdateChecker(Main main) {
        this.main = main;
        projectName = main.getDescription().getName();
        this.project = main.getDescription().getName().toLowerCase();

        if (main.serverVersion == 8) {
            run();
        } else {
            runTaskLaterAsynchronously(main, 20);
        }

    }

    public void run() {
        try {
            StringBuilder page = makeAsyncGetRequest("https://cld.pt/dl/download/51c19f75-8900-49f2-8e1b-a92256bf2d4a/bukkit.txt?download=true/");

            if (page != null && page.length() > 10) {
                String pagina = page.toString();
                int pointer = pagina.indexOf("project-file-name-container");
                pagina = pagina.substring(pointer); //smaller data

                String tmp = pagina.substring(pagina.indexOf("/projects/" + project));
                String version = tmp.substring(tmp.indexOf("data-name=\"") + 11).split("\"")[0];
                String url = tmp.split("\"")[0];

                promptUpdate(version, url);
            }
        } catch (Exception e) {
            Bukkit.getLogger().info("[" + projectName + "] Connection exception: " + e.getMessage());

            Bukkit.getConsoleSender().sendMessage(net.kyori.adventure.text.Component
                    .text("[" + projectName + "] Connection exception: " + e.getMessage())
                    .color(net.kyori.adventure.text.format.NamedTextColor.RED)
            );
        }
    }

    private StringBuilder makeAsyncGetRequest(String url) {
        StringBuilder response = new StringBuilder();
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.connect();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                    response.append(line);
                }
            }
        } catch (Exception ex) {
        }
        return response;
    }

    private void promptUpdate(String serverVersion, String Url) {
        TextComponent component;

        if (serverVersion == null) {
            component = Component.text(" Unknown error checking version");

            Bukkit.getConsoleSender().sendMessage(Component
                    .text("[" + projectName + "]")
                    .color(NamedTextColor.RED)
                    .append(component)
            );

            return;
        }

        String tmpServerVersion = null;
        if (serverVersion.split(" v").length > 1) tmpServerVersion = serverVersion.split(" v")[1];
        if (tmpServerVersion == null) tmpServerVersion = serverVersion.split(" ")[1];
        serverVersion = tmpServerVersion;

        String currentVersion = main.getDescription().getVersion();
        int versionStatus = Utils.checkGreater(serverVersion, currentVersion);
        NamedTextColor color = NamedTextColor.GRAY;

        if (versionStatus == -1) {
            component = Component.text(" THERE IS A NEW UPDATE AVAILABLE Version: " + serverVersion +
                    " Download it from here: https://dev.bukkit.org" + Url, NamedTextColor.GREEN);
        } else if (versionStatus == 0) {
            component = Component.text(" You have the latest released version", NamedTextColor.GREEN);
        } else if (versionStatus == 1) {
            component = Component.text(" Congrats, you are testing a new version!", NamedTextColor.YELLOW);
        } else {
            component = Component.text(" Unknown error checking version (" + versionStatus + ")" + serverVersion + "   " + currentVersion, NamedTextColor.RED);
        }

        Bukkit.getConsoleSender().sendMessage(Component
                .text("[" + projectName + "]")
                .color(color)
                .append(component)
        );
    }

}