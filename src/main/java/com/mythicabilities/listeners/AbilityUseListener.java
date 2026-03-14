package com.mythicabilities.listeners;

import com.mythicabilities.MythicAbilities;
import com.mythicabilities.abilities.Ability;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class AbilityUseListener implements Listener {
    
    private final MythicAbilities plugin;
    
    public AbilityUseListener(MythicAbilities plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof Player target)) return;
        
        String abilityName = plugin.getAbilityManager().getPlayerAbility(player);
        if (abilityName == null) return;
        
        Ability ability = plugin.getAbilityManager().getAbility(abilityName);
        if (ability == null) return;
        
        // Cancel damage from abilities
        if (event.getDamage() > 0) {
            // Check if it's ability damage
            if (event.getCause() == EntityDamageByEntityEvent.DamageCause.CUSTOM ||
                event.getCause() == EntityDamageByEntityEvent.DamageCause.ENTITY_ATTACK) {
                
                // Add combo
                plugin.getAbilityManager().addCombo(player);
                int combo = plugin.getAbilityManager().getCombo(player);
                
                // Visual combo effect
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                player.spawnParticle(org.bukkit.Particle.ENCHANT, player.getLocation().add(0, 2, 0), 20, 0.5, 0.5, 0.5, 0);
                
                player.sendActionBar(Component.text("§6Combo: §e" + combo + "§6/3"));
                
                // Check for 3 combos
                if (combo >= 3) {
                    // Trigger ability
                    if (!plugin.getCooldownManager().isOnCooldown(player, abilityName)) {
                        ability.onCombo(player, target);
                        plugin.getCooldownManager().setCooldown(player, abilityName, ability.getCooldown());
                        plugin.getAbilityManager().resetCombo(player);
                        
                        // Success message
                        player.sendMessage(Component.text("§a§l✦ ABILITY TRIGGERED! ✦"));
                        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
                    } else {
                        long remaining = plugin.getCooldownManager().getRemainingCooldown(player, abilityName);
                        player.sendActionBar(Component.text("§cCooldown: " + remaining + "s"));
                        plugin.getAbilityManager().resetCombo(player);
                    }
                }
            }
        }
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        String abilityName = plugin.getAbilityManager().getPlayerAbility(player);
        
        if (abilityName == null) return;
        
        Ability ability = plugin.getAbilityManager().getAbility(abilityName);
        if (ability == null) return;
        
        // Manual triggers (optional)
        if (event.getAction() == Action.RIGHT_CLICK_AIR || 
            event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            
            if (!plugin.getCooldownManager().isOnCooldown(player, abilityName)) {
                ability.onRightClick(player);
                plugin.getCooldownManager().setCooldown(player, abilityName, ability.getCooldown());
            } else {
                long remaining = plugin.getCooldownManager().getRemainingCooldown(player, abilityName);
                player.sendActionBar(Component.text("§cCooldown: " + remaining + "s"));
            }
        } else if (event.getAction() == Action.LEFT_CLICK_AIR || 
                   event.getAction() == Action.LEFT_CLICK_BLOCK) {
            ability.onLeftClick(player);
        }
    }
}
