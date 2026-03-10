package com.mythicabilities.abilities;

import com.mythicabilities.MythicAbilities;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;

public class FrostWalker extends Ability {
    
    private final MythicAbilities plugin;
    
    public FrostWalker(MythicAbilities plugin) {
        super("frost_walker", "§b§lFrost Walker", 20, createIcon());
        this.plugin = plugin;
    }
    
    private static ItemStack createIcon() {
        ItemStack icon = new ItemStack(Material.ICE);
        ItemMeta meta = icon.getItemMeta();
        meta.displayName(Component.text("§b§lFrost Walker"));
        meta.lore(List.of(
            Component.text("§7Right-click to launch ice projectiles"),
            Component.text("§7Freezes and knocks back enemies"),
            Component.text("§bCooldown: 20 seconds")
        ));
        icon.setItemMeta(meta);
        return icon;
    }
    
    @Override
    public void onRightClick(Player player) {
        shootIceLane(player);
    }
    
    @Override
    public void onLeftClick(Player player) {
        // Not used for Frost Walker
    }
    
    @Override
    public void onCombo(Player player, Player target) {
        // Not used for Frost Walker
    }
    
    private void shootIceLane(Player player) {
        Location startLoc = player.getEyeLocation();
        Vector direction = startLoc.getDirection().normalize();
        
        player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1, 1);
        
        new BukkitRunnable() {
            int distance = 0;
            Location currentLoc = startLoc.clone();
            
            @Override
            public void run() {
                if (distance >= 10) {
                    this.cancel();
                    return;
                }
                
                // Move forward
                currentLoc.add(direction.clone().multiply(1.5));
                distance++;
                
                // Create ice block falling effect
                Location iceLoc = currentLoc.clone().add(0, 2, 0);
                player.getWorld().spawnParticle(Particle.BLOCK, iceLoc, 20, 0.3, 0.3, 0.3, 
                    Material.ICE.createBlockData());
                
                player.getWorld().spawnParticle(Particle.SNOWFLAKE, iceLoc, 10, 0.5, 0.5, 0.5, 0.1);
                player.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, iceLoc, 5, 0.2, 0.2, 0.2, 0.1);
                
                // Create frost effects
                player.getWorld().spawnParticle(Particle.SNOWFLAKE, 
                    currentLoc.clone().add(0, 1, 0), 15, 1, 1, 1, 0.05);
                
                // Hit detection
                for (Entity entity : player.getWorld().getNearbyEntities(currentLoc, 2, 2, 2)) {
                    if (entity instanceof LivingEntity target && !entity.equals(player)) {
                        // Freeze effect
                        target.setFreezeTicks(100);
                        target.setVelocity(direction.clone().multiply(2).setY(0.8));
                        
                        // Damage
                        target.damage(4, player);
                        
                        // Ice particle explosion
                        target.getWorld().spawnParticle(Particle.BLOCK, target.getLocation().add(0, 1, 0),
                            30, 0.5, 0.5, 0.5, Material.ICE.createBlockData());
                        
                        player.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 1, 1);
                        
                        this.cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 2);
    }
}
