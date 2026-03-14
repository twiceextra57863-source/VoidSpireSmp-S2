package com.mythicabilities.listeners;

import com.mythicabilities.MythicAbilities;
import com.mythicabilities.abilities.Ability;
import com.mythicabilities.admin.ForceAbilityManager;
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
        ForceAbilityManager fam = plugin.getForceAbilityManager();
        
        if (!player.hasPlayedBefore()) {
            // First time join
            player.sendMessage(Component.text("§6§l✦ Welcome to Mythic SMP! ✦"));
            
            if (fam.shouldGiveOnFirstJoin()) {
                // Give ability via spin
                player.sendMessage(Component.text("§eGet ready for your ability spin!"));
                
                // Welcome title
                player.showTitle(Title.title(
                    Component.text("§6§lMYTHIC SMP"),
                    Component.text("§ePrepare to spin!"),
                    Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(1))
                ));
                
                // Play sound
                player.playSound(player.getLocation(), "minecraft:block.note_block.chime", 1, 1);
                
                // Small delay before spin
                org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    plugin.getSpinGUI().startVisualSpin(player);
                }, 40); // 2 second delay
            } else {
                // No ability on first join (disabled by admin)
                player.sendMessage(Component.text("§eAbilities are currently disabled by admin."));
                player.sendMessage(Component.text("§7Check back later for events!"));
            }
        } else {
            // Returning player
            String abilityName = plugin.getAbilityManager().getPlayerAbility(player);
            if (abilityName != null) {
                Ability playerAbility = plugin.getAbilityManager().getAbility(abilityName);
                if (playerAbility != null) {
                    player.sendMessage(Component.text("§aYour ability: " + playerAbility.getDisplayName()));
                    player.sendActionBar(Component.text("§eYour ability: " + playerAbility.getDisplayName()));
                }
            } else {
                // Player doesn't have ability
                if (fam.isEventActive()) {
                    // Event is active - will get ability when event ends
                    player.sendMessage(Component.text("§eAn ability event is active!"));
                    player.sendMessage(Component.text("§7You'll receive an ability when it ends."));
                } else {
                    player.sendMessage(Component.text("§eYou don't have an ability yet!"));
                }
            }
        }
    }
}
