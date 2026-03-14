package com.mythicabilities.abilities;

import com.mythicabilities.MythicAbilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VoidWalker extends Ability {
    
    private final MythicAbilities plugin;
    private final List<UUID> invisiblePlayers = new ArrayList<>();
    private final List<UUID> backstabReady = new ArrayList<>();
    
    public VoidWalker(MythicAbilities plugin) {
        super("void_walker", "§5§lVoid Walker", 25, createIcon());
        this.plugin = plugin;
    }
    
    private static ItemStack createIcon() {
        ItemStack icon = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = icon.getItemMeta();
        meta.displayName(Component.text("§5§lVoid Walker"));
        meta.lore(List.of(
            Component.text("§7Auto-triggers after 3 combos!"),
            Component.text("§7Become one with the shadows"),
            Component.text("§7• Shadow Step behind enemy"),
            Component.text("§7• Invisibility for 10 seconds"),
            Component.text("§7• Backstab does 3x damage"),
            Component.text("§7• Shadow explosion on reveal"),
            Component.text("§5Cooldown: 25 seconds")
        ));
        icon.setItemMeta(meta);
        return icon;
    }
    
    @Override
    public void onTrigger(Player player) {
        activateVoidWalker(player);
    }
    
    @Override
    public void onCombo(Player player, Player target) {
        // Target is the last hit enemy
        activateVoidWalkerWithTarget(player, target);
    }
    
    private void activateVoidWalker(Player player) {
        // Find nearest enemy
        Player nearestEnemy = null;
        double nearestDistance = Double.MAX_VALUE;
        
        for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), 15, 15, 15)) {
            if (entity instanceof Player target && !target.equals(player)) {
                double distance = player.getLocation().distance(target.getLocation());
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestEnemy = target;
                }
            }
        }
        
        activateVoidWalkerWithTarget(player, nearestEnemy);
    }
    
    private void activateVoidWalkerWithTarget(Player player, Player target) {
        if (target == null) {
            player.sendMessage(Component.text("§5No enemies nearby to shadow step!"));
            return;
        }
        
        UUID playerId = player.getUniqueId();
        invisiblePlayers.add(playerId);
        
        // Store original location
        Location originalLoc = player.getLocation().clone();
        
        // Shadow step behind target
        Location behindTarget = target.getLocation().clone();
        Vector direction = target.getLocation().getDirection().normalize();
        behindTarget.subtract(direction.multiply(2)); // 2 blocks behind
        
        // Ensure safe location (not in walls)
        behindTarget.setY(target.getLocation().getY());
        
        // Teleport with effect
        player.teleport(behindTarget);
        
        // Visual and sound effects
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 0.5f);
        player.getWorld().playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, 1, 0.8f);
        
        // Particle effect at both locations
        player.getWorld().spawnParticle(Particle.PORTAL, originalLoc, 50, 0.5, 1, 0.5, 2);
        player.getWorld().spawnParticle(Particle.PORTAL, behindTarget, 50, 0.5, 1, 0.5, 2);
        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, behindTarget, 30, 0.5, 1, 0.5, 0.1);
        
        // Make player invisible
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 200, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 200, 0));
        
        // Set backstab ready
        backstabReady.add(playerId);
        
        // Title message
        player.showTitle(Title.title(
            Component.text("§5§lSHADOW STEP"),
            Component.text("§dYou are one with the void"),
            Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(2), Duration.ofMillis(500))
        ));
        
        target.showTitle(Title.title(
            Component.text("§5§lWHERE DID HE GO?!"),
            Component.text("§dWatch your back..."),
            Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(1), Duration.ofMillis(500))
        ));
        
        // Start shadow effect
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 200 || !invisiblePlayers.contains(playerId)) {
                    this.cancel();
                    return;
                }
                
                // Shadow particles around player
                Location loc = player.getLocation();
                player.getWorld().spawnParticle(Particle.SMOKE, loc.clone().add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0.02);
                player.getWorld().spawnParticle(Particle.SPELL_WITCH, loc.clone().add(0, 1, 0), 3, 0.3, 0.5, 0.3, 0);
                
                // Occasionally show shadow figure
                if (ticks % 20 == 0) {
                    player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, 1, 0), 10, 0.5, 1, 0.5, 0.05);
                }
                
                ticks += 5;
            }
        }.runTaskTimer(plugin, 0, 5);
        
        // Remove after 10 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                if (invisiblePlayers.contains(playerId)) {
                    invisiblePlayers.remove(playerId);
                    backstabReady.remove(playerId);
                    
                    // Remove effects
                    player.removePotionEffect(PotionEffectType.INVISIBILITY);
                    player.removePotionEffect(PotionEffectType.SPEED);
                    player.removePotionEffect(PotionEffectType.NIGHT_VISION);
                    
                    // Shadow explosion
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1, 0.5f);
                    player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, player.getLocation(), 1);
                    player.getWorld().spawnParticle(Particle.SOUL, player.getLocation(), 100, 3, 1, 3, 0.2);
                    
                    // Affect nearby enemies
                    for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), 5, 5, 5)) {
                        if (entity instanceof Player enemy && !enemy.equals(player)) {
                            enemy.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 1));
                            enemy.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 0));
                            enemy.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
                            
                            enemy.sendMessage(Component.text("§5The void consumes you..."));
                        }
                    }
                    
                    player.sendMessage(Component.text("§5Your invisibility fades away..."));
                }
            }
        }.runTaskLater(plugin, 200); // 10 seconds
    }
    
    @Override
    public void onLeftClick(Player player) {
        // Not used
    }
    
    @Override
    public void onRightClick(Player player) {
        // Manual activate
        if (!invisiblePlayers.contains(player.getUniqueId())) {
            onTrigger(player);
        }
    }
    
    // Called from listener when player attacks while invisible
    public boolean tryBackstab(Player player, Player target) {
        UUID playerId = player.getUniqueId();
        
        if (backstabReady.contains(playerId)) {
            // Backstab successful
            backstabReady.remove(playerId);
            
            // Visual effect
            player.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1, 0.5f);
            player.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.5);
            player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0, 1, 0), 1);
            
            // Damage (3x normal - will be applied in listener)
            return true;
        }
        
        return false;
    }
    
    public boolean isInvisible(Player player) {
        return invisiblePlayers.contains(player.getUniqueId());
    }
                  }
