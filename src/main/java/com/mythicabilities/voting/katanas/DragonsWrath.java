package com.mythicabilities.katana.katanas;

import com.mythicabilities.MythicAbilities;
import com.mythicabilities.katana.KatanaAbility;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class DragonsWrath implements KatanaAbility {
    
    private final MythicAbilities plugin;
    
    public DragonsWrath(MythicAbilities plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void onLeftClick(Player player) {
        // Flame Slash
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_SHOOT, 1, 1);
        
        for (double d = 0; d < 8; d += 0.5) {
            Location loc = eye.clone().add(direction.clone().multiply(d));
            player.getWorld().spawnParticle(Particle.FLAME, loc, 5, 0.2, 0.2, 0.2, 0);
            
            // Damage enemies
            player.getWorld().getNearbyEntities(loc, 1.5, 1.5, 1.5).stream()
                .filter(e -> e instanceof Player && !e.equals(player))
                .forEach(e -> {
                    e.setFireTicks(60);
                    ((Player) e).damage(8, player);
                });
        }
    }
    
    @Override
    public void onRightClick(Player player) {
        // Dragon Flight
        player.setVelocity(new Vector(0, 1.5, 0));
        player.setAllowFlight(true);
        player.setFlying(true);
        
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1, 1);
    }
    
    @Override
    public void onSneakLeft(Player player) {
        // Dragon Breath
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1, 0.8f);
        
        for (double d = 0; d < 10; d += 0.3) {
            for (int i = -2; i <= 2; i++) {
                Vector spread = direction.clone().rotateAroundY(Math.toRadians(i * 5));
                Location loc = eye.clone().add(spread.multiply(d));
                
                player.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc, 3, 0.2, 0.2, 0.2, 0);
                
                player.getWorld().getNearbyEntities(loc, 1, 1, 1).stream()
                    .filter(e -> e instanceof Player && !e.equals(player))
                    .forEach(e -> ((Player) e).damage(4, player));
            }
        }
    }
    
    @Override
    public void onSneakRight(Player player) {
        // Ultimate - Dragon's Wrath
        player.getWorld().strikeLightningEffect(player.getLocation());
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 2, 0.5f);
        
        for (int i = 0; i < 10; i++) {
            player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, 
                player.getLocation().add(Math.random() * 10 - 5, 0, Math.random() * 10 - 5), 1);
        }
        
        player.getWorld().getNearbyEntities(player.getLocation(), 10, 5, 10).stream()
            .filter(e -> e instanceof Player && !e.equals(player))
            .forEach(e -> ((Player) e).damage(15, player));
    }
    
    @Override
    public void onPassive(Player player) {
        // Dragon's Roar - periodic effect
        if (Math.random() < 0.1) {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_AMBIENT, 0.3f, 1);
        }
    }
    
    @Override
    public int getCooldown(String abilityType) {
        switch (abilityType.toLowerCase()) {
            case "left": return 3;
            case "right": return 15;
            case "sneakleft": return 10;
            case "sneakright": return 60;
            default: return 5;
        }
    }
    
    @Override
    public String getDescription() {
        return "§c§lDRAGON'S WRATH\n§7Unleash the power of ancient dragons!";
    }
}
