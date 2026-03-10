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
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        String abilityName = plugin.getAbilityManager().getPlayerAbility(player);
        
        if (abilityName == null) return;
        
        Ability ability = plugin.getAbilityManager().getAbility(abilityName);
        if (ability == null) return;
        
        // Check cooldown
        if (plugin.getCooldownManager().isOnCooldown(player, abilityName)) {
            long remaining = plugin.getCooldownManager().getRemainingCooldown(player, abilityName);
            player.sendActionBar(Component.text("§cCooldown: " + remaining + "s"));
            return;
        }
        
        if (event.getAction() == Action.RIGHT_CLICK_AIR || 
            event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ability.onRightClick(player);
            plugin.getCooldownManager().setCooldown(player, abilityName, ability.getCooldown());
        } else if (event.getAction() == Action.LEFT_CLICK_AIR || 
                   event.getAction() == Action.LEFT_CLICK_BLOCK) {
            ability.onLeftClick(player);
        }
    }
    
    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof Player target)) return;
        
        String abilityName = plugin.getAbilityManager().getPlayerAbility(player);
        if (abilityName == null) return;
        
        Ability ability = plugin.getAbilityManager().getAbility(abilityName);
        if (ability == null) return;
        
        // Handle combo for abilities
        ability.onCombo(player, target);
    }
}
