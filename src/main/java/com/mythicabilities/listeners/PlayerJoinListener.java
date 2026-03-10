package com.mythicabilities.listeners;

import com.mythicabilities.MythicAbilities;
import com.mythicabilities.gui.AbilitySpinGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.time.Duration;

public class PlayerJoinListener implements Listener {
    
    private final MythicAbilities plugin;
    
    public PlayerJoinListener(MythicAbilities plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        if (!player.hasPlayedBefore()) {
            // First time join - give random ability via spin
            player.sendMessage(Component.text("§6§l✦ Welcome to Mythic SMP! ✦"));
            player.sendMessage(Component.text("§eYou've earned a random ability!"));
            
            // Welcome title
            player.showTitle(Title.title(
                Component.text("§6§lMYTHIC SMP"),
                Component.text("§eSpin for your ability!"),
                Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(3), Duration.ofSeconds(1))
            ));
            
            // Play sound
            player.playSound(player.getLocation(), "minecraft:block.note_block.chime", 1, 1);
            
            // Open ability spin GUI
            new AbilitySpinGUI(plugin).openSpinGUI(player);
        } else {
            // Returning player
            String ability = plugin.getAbilityManager().getPlayerAbility(player);
            if (ability != null) {
                player.sendMessage(Component.text("§aYour ability: §e" + ability));
            }
        }
    }
}
