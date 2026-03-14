package com.mythicabilities.listeners;

import com.mythicabilities.MythicAbilities;
import com.mythicabilities.gui.AbilitySpinGUI;
import net.kyori.adventure.text.Component;
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
            // First time join - give random ability via visual spin
            player.sendMessage(Component.text("§6§l✦ Welcome to Mythic SMP! ✦"));
            player.sendMessage(Component.text("§eGet ready for your ability spin!"));
            
            // Welcome title
            player.showTitle(Title.title(
                Component.text("§6§lMYTHIC SMP"),
                Component.text("§ePrepare to spin!"),
                Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(1))
            ));
            
            // Play sound
            player.playSound(player.getLocation(), "minecraft:block.note_block.chime", 1, 1);
            
            // Small delay before spin for dramatic effect
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Open visual spin directly (not GUI)
                AbilitySpinGUI spinGUI = new AbilitySpinGUI(plugin);
                spinGUI.startVisualSpin(player);
            }, 40); // 2 second delay
        } else {
            // Returning player
            String ability = plugin.getAbilityManager().getPlayerAbility(player);
            if (ability != null) {
                Ability playerAbility = plugin.getAbilityManager().getAbility(ability);
                if (playerAbility != null) {
                    player.sendMessage(Component.text("§aYour ability: " + playerAbility.getDisplayName()));
                    
                    // Show action bar with ability
                    player.sendActionBar(Component.text("§eYour ability: " + playerAbility.getDisplayName()));
                }
            } else {
                // Player somehow doesn't have ability - give them one
                player.sendMessage(Component.text("§eYou don't have an ability! Spinning for you..."));
                org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    AbilitySpinGUI spinGUI = new AbilitySpinGUI(plugin);
                    spinGUI.startVisualSpin(player);
                }, 20);
            }
        }
    }
}
