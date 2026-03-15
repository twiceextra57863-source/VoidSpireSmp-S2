package com.mythicabilities.listeners;

import com.mythicabilities.MythicAbilities;
import com.mythicabilities.abilities.Ability;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

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
            // Player has no ability - still track combos for debugging
            plugin.getAbilityManager().addCombo(player);
            int combo = plugin.getAbilityManager().getCombo(player);
            player.sendActionBar(Component.text("§7Combo: §e" + combo + " §7(No ability)"));
            return;
        }
        
        Ability ability = plugin.getAbilityManager().getAbility(abilityName);
        if (ability == null) return;
        
        // Add combo counter
        plugin.getAbilityManager().addCombo(player);
        int combo = plugin.getAbilityManager().getCombo(player);
        
        // Visual combo effect
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        player.spawnParticle(org.bukkit.Particle.CRIT, target.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.1);
        
        // Show combo in action bar
        player.sendActionBar(Component.text(
            "§6⚡ Combo: §e" + combo + "§6/3 §7" + 
            (target instanceof Player ? "vs " + ((Player) target).getName() : "vs Mob")
        ));
        
        // Check for 3 combos
        if (combo >= 3) {
            // Check cooldown
            if (!plugin.getCooldownManager().isOnCooldown(player, abilityName)) {
                // Trigger ability
                if (target instanceof Player) {
                    ability.onCombo(player, (Player) target);
                } else {
                    // For mobs, create a dummy player? Or just trigger without target
                    ability.onTrigger(player);
                }
                
                // Set cooldown
                plugin.getCooldownManager().setCooldown(player, abilityName, ability.getCooldown());
                
                // Reset combo
                plugin.getAbilityManager().resetCombo(player);
                
                // Success message
                player.sendMessage(Component.text("§a§l✦ ABILITY TRIGGERED! ✦"));
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
                
                // Particle effect
                player.getWorld().spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, 
                    player.getLocation().add(0, 2, 0), 50, 0.5, 0.5, 0.5, 0.5);
            } else {
                // On cooldown
                long remaining = plugin.getCooldownManager().getRemainingCooldown(player, abilityName);
                player.sendActionBar(Component.text("§cCooldown: " + remaining + "s"));
                plugin.getAbilityManager().resetCombo(player);
            }
        }
    }
    
    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        // Optional: Handle self-damage prevention
        if (event.getEntity() instanceof Player player) {
            // Check if damage is from ability (custom damage source)
            // You can implement this if needed
        }
    }
}
