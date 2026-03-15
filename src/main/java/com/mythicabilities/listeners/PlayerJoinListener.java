package com.mythicabilities.listeners;

import com.mythicabilities.MythicAbilities;
import com.mythicabilities.abilities.Ability;
import com.mythicabilities.admin.ForceAbilityManager;
import com.mythicabilities.gui.AbilitySpinGUI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.time.Duration;
import java.util.Random;

public class PlayerJoinListener implements Listener {
    
    private final MythicAbilities plugin;
    private final Random random = new Random();
    
    public PlayerJoinListener(MythicAbilities plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ForceAbilityManager fam = plugin.getForceAbilityManager();
        
        if (!player.hasPlayedBefore()) {
            // First time join
            player.sendMessage("§6§l╔════════════════════════════════════╗");
            player.sendMessage("§6§l║     §e✦ WELCOME TO MYTHIC SMP! ✦   §6§l║");
            player.sendMessage("§6§l║                                    §6§l║");
            player.sendMessage("§6§l║    §fPrepare for your ability spin  §6§l║");
            player.sendMessage("§6§l║        §7Starting in 5 seconds...    §6§l║");
            player.sendMessage("§6§l╚════════════════════════════════════╝");
            
            // Welcome title
            player.showTitle(Title.title(
                Component.text("§6§lMYTHIC SMP"),
                Component.text("§ePrepare to spin!"),
                Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(1))
            ));
            
            // Play welcome sound
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1, 1);
            
            // Random delay between 5-10 seconds
            int delaySeconds = 5 + random.nextInt(6); // 5 to 10 seconds
            int delayTicks = delaySeconds * 20;
            
            player.sendMessage("§eSpin starting in §a" + delaySeconds + " §eseconds...");
            
            // Start countdown in action bar
            startCountdown(player, delaySeconds);
            
            // Schedule spin after delay
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (fam.shouldGiveOnFirstJoin()) {
                    player.sendMessage("§a§l✦ SPINNING NOW! ✦");
                    plugin.getSpinGUI().startVisualSpin(player);
                } else {
                    player.sendMessage("§cAbilities are currently disabled by admin.");
                }
            }, delayTicks);
            
        } else {
            // Returning player
            String abilityName = plugin.getAbilityManager().getPlayerAbility(player);
            if (abilityName != null) {
                Ability playerAbility = plugin.getAbilityManager().getAbility(abilityName);
                if (playerAbility != null) {
                    player.sendMessage("§aYour ability: " + playerAbility.getDisplayName());
                    
                    // Show action bar with ability
                    player.sendActionBar(Component.text("§eYour ability: " + playerAbility.getDisplayName()));
                    
                    // Small welcome back effect
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.3f, 1.5f);
                    player.spawnParticle(org.bukkit.Particle.END_ROD, player.getLocation().add(0, 2, 0), 10, 0.3, 0.3, 0.3, 0.02);
                }
            } else {
                // Player doesn't have ability
                if (fam.isEventActive()) {
                    player.sendMessage("§eAn ability event is active!");
                    player.sendMessage("§7You'll receive an ability when it ends.");
                } else {
                    player.sendMessage("§eYou don't have an ability yet! Use §a/abilities §eto spin!");
                }
            }
        }
    }
    
    private void startCountdown(Player player, int totalSeconds) {
        new org.bukkit.scheduler.BukkitRunnable() {
            int secondsLeft = totalSeconds;
            
            @Override
            public void run() {
                if (secondsLeft <= 0) {
                    this.cancel();
                    return;
                }
                
                // Create countdown bar
                StringBuilder bar = new StringBuilder("§8[");
                int progress = (int) ((secondsLeft * 20) / totalSeconds);
                
                for (int i = 0; i < 20; i++) {
                    if (i < progress) {
                        if (secondsLeft > totalSeconds/2) bar.append("§a█");
                        else if (secondsLeft > totalSeconds/4) bar.append("§e█");
                        else bar.append("§c█");
                    } else {
                        bar.append("§7█");
                    }
                }
                bar.append("§8]");
                
                player.sendActionBar(Component.text(
                    "§eSpin in: §a" + secondsLeft + "s " + bar.toString()
                ));
                
                // Beep sound on last seconds
                if (secondsLeft <= 3) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, 1.5f);
                }
                
                secondsLeft--;
            }
        }.runTaskTimer(plugin, 0, 20);
    }
}
