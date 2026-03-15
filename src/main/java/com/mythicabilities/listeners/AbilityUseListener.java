package com.mythicabilities.listeners;

import com.mythicabilities.MythicAbilities;
import com.mythicabilities.abilities.Ability;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent; // NEW

public class AbilityUseListener implements Listener {
    
    private final MythicAbilities plugin;
    
    public AbilityUseListener(MythicAbilities plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Check if damager is player
        if (!(event.getDamager() instanceof Player player)) return;
        
        // Check if entity is LivingEntity (player or mob)
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        
        // Get player's ability
        String abilityName = plugin.getAbilityManager().getPlayerAbility(player);
        if (abilityName == null) {
            // Player has no ability - still track combos for practice
            plugin.getAbilityManager().addCombo(player);
            int combo = plugin.getAbilityManager().getCombo(player);
            showPracticeCombo(player, combo);
            return;
        }
        
        Ability ability = plugin.getAbilityManager().getAbility(abilityName);
        if (ability == null) return;
        
        // Add combo counter
        plugin.getAbilityManager().addCombo(player);
        int combo = plugin.getAbilityManager().getCombo(player);
        
        // Visual combo effect with fighter-style animation
        showComboAnimation(player, combo, target);
        
        // Play combo sound
        playComboSound(player, combo);
        
        // Check for 3 combos
        if (combo >= 3) {
            // Check cooldown
            if (!plugin.getCooldownManager().isOnCooldown(player, abilityName)) {
                // Trigger ability
                if (target instanceof Player) {
                    ability.onCombo(player, (Player) target);
                } else {
                    // For mobs, just trigger without specific target
                    ability.onTrigger(player);
                }
                
                // Set cooldown
                plugin.getCooldownManager().setCooldown(player, abilityName, ability.getCooldown());
                
                // Reset combo
                plugin.getAbilityManager().resetCombo(player);
                
                // Success message with epic effect
                showAbilityTriggered(player, ability);
                
            } else {
                // On cooldown - show cooldown message
                long remaining = plugin.getCooldownManager().getRemainingCooldown(player, abilityName);
                player.sendActionBar(Component.text("§c❌ Cooldown: " + remaining + "s ❌"));
                
                // Visual feedback for cooldown
                player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 
                    10, 0.3, 0.5, 0.3, 0.02);
                
                // Reset combo
                plugin.getAbilityManager().resetCombo(player);
            }
        }
        
        // NEW: Track for assists (store damage dealt)
        if (target instanceof Player) {
            // Store damage for assist tracking (simplified - you can expand this)
            double finalDamage = event.getFinalDamage();
            if (target.getHealth() - finalDamage <= 0) {
                // This kill will be handled in onPlayerDeath event
            }
        }
    }
    
    // NEW: Handle player death for kill/assist tracking
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        
        if (killer != null) {
            // Record kill
            plugin.getScoreboardManager().recordKill(killer, victim);
            
            // Check for assists (players who damaged victim recently)
            // In a real implementation, you'd track damage history
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p != killer && p != victim && p.getLocation().distance(victim.getLocation()) < 15) {
                    // Simple proximity-based assist
                    plugin.getScoreboardManager().recordAssist(p);
                }
            }
        }
    }
    
    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        // Optional: Handle self-damage prevention from abilities
        if (event.getEntity() instanceof Player player) {
            // You can add logic here to prevent ability self-damage if needed
        }
    }
    
    /**
     * Show fighter-style combo animation with textures
     */
    private void showComboAnimation(Player player, int combo, LivingEntity target) {
        String targetName = (target instanceof Player) ? ((Player) target).getName() : "Mob";
        
        // Create combo display with different styles based on combo count
        StringBuilder comboText = new StringBuilder();
        
        // Add combo number with fighter-style graphics
        if (combo == 1) {
            comboText.append("§7┌─ §f⚡ §7─┐  §fCombo §7x§f1  ");
            comboText.append("§8[§7█§8░░]");
        } else if (combo == 2) {
            comboText.append("§e┌─ §6⚡⚡ §e─┐  §6Combo §ex§6" + combo + "  ");
            comboText.append("§8[§e██§8░░]");
        } else if (combo == 3) {
            comboText.append("§c┌─ §4⚡⚡⚡ §c─┐  §4§lCOMBO x3!  ");
            comboText.append("§8[§c███§8]");
        }
        
        // Add target info
        comboText.append(" §7vs §f").append(targetName);
        
        // Send to action bar
        player.sendActionBar(Component.text(comboText.toString()));
        
        // Spawn particles based on combo level
        spawnComboParticles(player, combo, target);
    }
    
    /**
     * Show practice combo for players without abilities
     */
    private void showPracticeCombo(Player player, int combo) {
        StringBuilder comboText = new StringBuilder();
        comboText.append("§7[ §8Practice §7]  §fCombo §7x§f" + combo + "  ");
        comboText.append("§8[");
        for (int i = 1; i <= 3; i++) {
            if (i <= combo) {
                comboText.append("§7█");
            } else {
                comboText.append("§8░");
            }
        }
        comboText.append("§8]");
        
        player.sendActionBar(Component.text(comboText.toString()));
        
        // Simple practice particles
        player.getWorld().spawnParticle(Particle.END_ROD, 
            player.getLocation().add(0, 1, 0), 5, 0.2, 0.3, 0.2, 0.02);
    }
    
    /**
     * Spawn combo particles with increasing intensity
     */
    private void spawnComboParticles(Player player, int combo, LivingEntity target) {
        Location playerLoc = player.getLocation().add(0, 1, 0);
        Location targetLoc = target.getLocation().add(0, 1, 0);
        
        switch (combo) {
            case 1:
                // First hit - small spark
                player.getWorld().spawnParticle(Particle.CRIT, targetLoc, 10, 0.2, 0.3, 0.2, 0.1);
                player.getWorld().spawnParticle(Particle.END_ROD, targetLoc, 5, 0.1, 0.2, 0.1, 0.02);
                break;
                
            case 2:
                // Second hit - medium explosion
                player.getWorld().spawnParticle(Particle.CRIT, targetLoc, 20, 0.3, 0.5, 0.3, 0.2);
                player.getWorld().spawnParticle(Particle.ENCHANT, targetLoc, 15, 0.3, 0.5, 0.3, 0.3);
                player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, targetLoc, 2, 0.3, 0.3, 0.3, 0);
                
                // Line connecting player and target
                for (double d = 0; d <= 1; d += 0.1) {
                    Location lineLoc = playerLoc.clone().add(targetLoc.clone().subtract(playerLoc).multiply(d));
                    player.getWorld().spawnParticle(Particle.END_ROD, lineLoc, 1, 0.1, 0.1, 0.1, 0.01);
                }
                break;
                
            case 3:
                // Third hit - massive explosion
                player.getWorld().spawnParticle(Particle.CRIT, targetLoc, 40, 0.5, 0.5, 0.5, 0.5);
                player.getWorld().spawnParticle(Particle.ENCHANT, targetLoc, 30, 0.5, 0.5, 0.5, 0.5);
                player.getWorld().spawnParticle(Particle.FLASH, targetLoc, 2);
                player.getWorld().spawnParticle(Particle.SONIC_BOOM, targetLoc, 3, 0.2, 0.2, 0.2, 0);
                
                // Shockwave ring
                for (int i = 0; i < 360; i += 30) {
                    double rad = Math.toRadians(i);
                    double x = targetLoc.getX() + 2 * Math.cos(rad);
                    double z = targetLoc.getZ() + 2 * Math.sin(rad);
                    Location ringLoc = new Location(player.getWorld(), x, targetLoc.getY(), z);
                    player.getWorld().spawnParticle(Particle.EXPLOSION, ringLoc, 1, 0, 0, 0, 0);
                }
                break;
        }
    }
    
    /**
     * Play combo sounds with increasing intensity
     */
    private void playComboSound(Player player, int combo) {
        switch (combo) {
            case 1:
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
                break;
            case 2:
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.3f);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.5f, 1.2f);
                break;
            case 3:
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f);
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 0.5f, 1.8f);
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
                break;
        }
    }
    
    /**
     * Show ability triggered effect with epic animation
     */
    private void showAbilityTriggered(Player player, Ability ability) {
        // Main success message
        player.sendMessage("§a§l╔════════════════════════════════════╗");
        player.sendMessage("§a§l║     §e✦ ABILITY TRIGGERED! ✦      §a§l║");
        player.sendMessage("§a§l║                                    §a§l║");
        player.sendMessage("§a§l║   §f" + ability.getDisplayName() + " §a§l║");
        player.sendMessage("§a§l╚════════════════════════════════════╝");
        
        // Epic particle effect
        Location loc = player.getLocation().add(0, 2, 0);
        
        // Spiral up
        new org.bukkit.scheduler.BukkitRunnable() {
            int tick = 0;
            
            @Override
            public void run() {
                if (tick >= 20) {
                    this.cancel();
                    return;
                }
                
                double angle = tick * 0.5;
                double radius = 1.5;
                double x = loc.getX() + radius * Math.cos(angle);
                double z = loc.getZ() + radius * Math.sin(angle);
                double y = loc.getY() + tick * 0.1;
                
                Location particleLoc = new Location(player.getWorld(), x, y, z);
                
                // Alternate particles
                if (tick % 2 == 0) {
                    player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, particleLoc, 5, 0.1, 0.1, 0.1, 0.5);
                } else {
                    player.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 3, 0.1, 0.1, 0.1, 0.02);
                }
                
                tick++;
            }
        }.runTaskTimer(plugin, 0, 1);
        
        // Burst effect
        player.getWorld().spawnParticle(Particle.FLASH, loc, 3);
        player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);
        
        // Sound
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1.2f);
    }
    }
