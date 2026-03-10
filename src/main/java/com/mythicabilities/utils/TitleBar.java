package com.mythicabilities.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.time.Duration;

public class TitleBar {
    
    public static void sendTimerBar(Player player, int seconds) {
        // Create progress bar
        int total = 60; // Total grace period
        int progress = (int) ((seconds * 20) / total); // 20 chars max
        
        StringBuilder bar = new StringBuilder("§8[");
        for (int i = 0; i < 20; i++) {
            if (i < progress) {
                bar.append("§a█");
            } else {
                bar.append("§7█");
            }
        }
        bar.append("§8]");
        
        // Send action bar
        player.sendActionBar(Component.text(
            "§eGrace Period: §a" + seconds + "s " + bar.toString()
        ));
        
        // Send title at specific times
        if (seconds == 10) {
            player.showTitle(Title.title(
                Component.text("§c§l10 SECONDS!"),
                Component.text("§eBorder expanding soon"),
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(1), Duration.ofMillis(500))
            ));
        } else if (seconds == 5) {
            player.showTitle(Title.title(
                Component.text("§4§l5... 4... 3..."),
                Component.text("§cGet ready!"),
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(1), Duration.ofMillis(500))
            ));
        } else if (seconds == 0) {
            player.showTitle(Title.title(
                Component.text("§6§lBORDER EXPANDED!"),
                Component.text("§aPVP is now enabled"),
                Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(3), Duration.ofSeconds(1))
            ));
        }
    }
    
    public static void sendWelcomeTitle(Player player) {
        player.showTitle(Title.title(
            Component.text("§6§lMYTHIC SMP"),
            Component.text("§eUse §a/abilities §eto get started"),
            Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(3), Duration.ofSeconds(1))
        ));
    }
    
    public static void sendBorderWarning(Player player) {
        player.showTitle(Title.title(
            Component.text("§c§lBORDER WARNING!"),
            Component.text("§eYou are near the edge"),
            Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(1), Duration.ofMillis(500))
        ));
    }
}
