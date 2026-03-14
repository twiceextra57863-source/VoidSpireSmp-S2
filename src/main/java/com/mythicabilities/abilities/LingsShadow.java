package com.mythicabilities.abilities;

import com.mythicabilities.MythicAbilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
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
import java.util.*;

public class LingsShadow extends Ability {
    
    private final MythicAbilities plugin;
    private final Map<UUID, Boolean> shadowMode = new HashMap<>();
    private final Map<UUID, Map<UUID, Long>> markedEnemies = new HashMap<>();
    private final Map<UUID, Long> lastDash = new HashMap<>();
    private final Map<UUID, Long> lastFinisher = new HashMap<>();
    private final Map<UUID, List<ArmorStand>> clones = new HashMap<>();
    private final Random random = new Random();
    
    public LingsShadow(MythicAbilities plugin) {
        super("lings_shadow", "§5§lLing's Shadow Step", 30, createIcon());
        this.plugin = plugin;
    }
    
    private static ItemStack createIcon() {
        ItemStack icon = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = icon.getItemMeta();
        meta.displayName(Component.text("§5§lLing's Shadow Step"));
        meta.lore(List.of(
            Component.text("§7Auto-triggers after 3 combos!"),
            Component.text("§7Become the ultimate shadow assassin!"),
            Component.text("§7• Left Click: Shadow Dash through walls"),
            Component.text("§7  §f- 10 blocks range, 2 sec cooldown"),
            Component.text("§7  §f- Creates shadow clones"),
            Component.text("§7• Right Click: Finisher Strike"),
            Component.text("§7  §f- Teleport + critical hit"),
            Component.text("§7  §f- 50% bonus if enemy below 50% HP"),
            Component.text("§7• Cloud Walker: Pass through terrain"),
            Component.text("§7• Shadow Mark: +15% damage for 3 sec"),
            Component.text("§5Cooldown: 30 seconds")
        ));
        icon.setItemMeta(meta);
        return icon;
    }
    
    @Override
    public void onTrigger(Player player) {
        activateShadowMode(player);
    }
    
    @Override
    public void onCombo(Player player, Player target) {
        activateShadowMode(player);
    }
    
    private void activateShadowMode(Player player) {
        UUID playerId = player.getUniqueId();
        shadowMode.put(playerId, true);
        markedEnemies.put(playerId, new HashMap<>());
        clones.put(playerId, new ArrayList<>());
        
        // Broadcast
        player.sendMessage(Component.text("§5§l✦ SHADOW MODE ACTIVATED ✦"));
        player.sendMessage(Component.text("§dYou are one with the shadows..."));
        
        // Title
        player.showTitle(Title.title(
            Component.text("§5§lSHADOW STEP"),
            Component.text("§d10 seconds of assassination"),
            Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(1), Duration.ofMillis(500))
        ));
        
        // Play awakening sound
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 0.5f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1, 0.8f);
        
        // Cloud Walker effects
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 1)); // 20% speed
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 200, 0));
        player.setFallDistance(0); // No fall damage
        
        // Visual transformation
        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 100, 0.5, 1, 0.5, 2);
        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation().add(0, 1, 0), 50, 0.5, 1, 0.5, 0.05);
        
        // Start shadow particles
        startShadowParticles(player);
        
        // End after 10 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                endShadowMode(player);
            }
        }.runTaskLater(plugin, 200); // 10 seconds
    }
    
    private void startShadowParticles(Player player) {
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 200 || !shadowMode.getOrDefault(player.getUniqueId(), false)) {
                    this.cancel();
                    return;
                }
                
                Location loc = player.getLocation();
                
                // Shadow aura
                player.getWorld().spawnParticle(Particle.PORTAL, loc.clone().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.5);
                player.getWorld().spawnParticle(Particle.SMOKE, loc.clone().add(0, 1, 0), 5, 0.2, 0.3, 0.2, 0.02);
                
                // Translucent effect (via invisibility + particles)
                if (ticks % 20 == 0) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 30, 0, false, false));
                }
                
                // Shadow trail
                if (player.isSprinting()) {
                    player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, 0.5, 0), 
                        3, 0.1, 0.1, 0.1, 0.02);
                }
                
                ticks += 5;
            }
        }.runTaskTimer(plugin, 0, 5);
    }
    
    @Override
    public void onLeftClick(Player player) {
        if (shadowMode.getOrDefault(player.getUniqueId(), false)) {
            UUID playerId = player.getUniqueId();
            
            // Check cooldown
            long now = System.currentTimeMillis();
            long lastDashTime = lastDash.getOrDefault(playerId, 0L);
            
            if (now - lastDashTime < 2000) { // 2 second cooldown
                long remaining = (2000 - (now - lastDashTime)) / 1000;
                player.sendActionBar(Component.text("§7Dash cooldown: §5" + remaining + "s"));
                return;
            }
            
            lastDash.put(playerId, now);
            performShadowDash(player);
        }
    }
    
    private void performShadowDash(Player player) {
        Location start = player.getLocation();
        Vector direction = player.getLocation().getDirection().normalize();
        double dashDistance = 10;
        
        // Calculate end point
        Location end = start.clone().add(direction.clone().multiply(dashDistance));
        
        // Phase through blocks (temporary)
        for (double i = 0; i < dashDistance; i += 0.5) {
            Location check = start.clone().add(direction.clone().multiply(i));
            Block block = check.getBlock();
            
            if (block.getType().isSolid()) {
                // Temporarily set block to air (visual only - we don't actually remove blocks)
                // Instead, we just let player pass through via teleport
            }
        }
        
        // Teleport
        player.teleport(end);
        
        // Effects
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 0.8f);
        player.getWorld().playSound(end, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 0.6f);
        
        // Trail of clones (after-images)
        for (double i = 0; i < dashDistance; i += 1.5) {
            Location cloneLoc = start.clone().add(direction.clone().multiply(i));
            
            // Particle after-image
            player.getWorld().spawnParticle(Particle.PORTAL, cloneLoc.clone().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.5);
            player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, cloneLoc.clone().add(0, 1, 0), 10, 0.2, 0.3, 0.2, 0.02);
            
            // Create temporary clone (using armor stand)
            if (i % 3 == 0) {
                createShadowClone(player, cloneLoc);
            }
        }
        
        // Dash complete message
        player.sendActionBar(Component.text("§5Shadow Dash!"));
    }
    
    private void createShadowClone(Player player, Location loc) {
        ArmorStand clone = player.getWorld().spawn(loc, ArmorStand.class);
        clone.setInvisible(true);
        clone.setMarker(true);
        clone.setGravity(false);
        
        // Clone particles
        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.05);
        player.getWorld().spawnParticle(Particle.PORTAL, loc.clone().add(0, 1, 0), 20, 0.2, 0.3, 0.2, 0.3);
        
        // Clone attack nearby enemies for 2 seconds
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 40) { // 2 seconds
                    clone.remove();
                    this.cancel();
                    return;
                }
                
                // Check for nearby enemies
                for (Entity entity : player.getWorld().getNearbyEntities(loc, 3, 2, 3)) {
                    if (entity instanceof LivingEntity target && !entity.equals(player)) {
                        // Clone attacks
                        target.damage(2, player); // 1 heart damage
                        target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1, 0), 5, 0.2, 0.3, 0.2, 0);
                        
                        // Knockback
                        Vector knockback = target.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(0.5);
                        target.setVelocity(knockback);
                    }
                }
                
                ticks += 5;
            }
        }.runTaskTimer(plugin, 0, 5);
        
        // Store clone to remove later
        clones.get(player.getUniqueId()).add(clone);
    }
    
    @Override
    public void onRightClick(Player player) {
        if (shadowMode.getOrDefault(player.getUniqueId(), false)) {
            UUID playerId = player.getUniqueId();
            
            // Check cooldown
            long now = System.currentTimeMillis();
            long lastFinisherTime = lastFinisher.getOrDefault(playerId, 0L);
            
            if (now - lastFinisherTime < 4000) { // 4 second cooldown
                long remaining = (4000 - (now - lastFinisherTime)) / 1000;
                player.sendActionBar(Component.text("§7Finisher cooldown: §5" + remaining + "s"));
                return;
            }
            
            // Find nearest enemy
            Player nearestEnemy = null;
            double nearestDistance = Double.MAX_VALUE;
            
            for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), 15, 10, 15)) {
                if (entity instanceof Player target && !target.equals(player)) {
                    double distance = player.getLocation().distance(target.getLocation());
                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearestEnemy = target;
                    }
                }
            }
            
            if (nearestEnemy != null) {
                lastFinisher.put(playerId, now);
                performFinisherStrike(player, nearestEnemy);
            } else {
                player.sendActionBar(Component.text("§7No enemies nearby!"));
            }
        }
    }
    
    private void performFinisherStrike(Player player, Player target) {
        Location originalLoc = player.getLocation();
        Location targetLoc = target.getLocation();
        
        // Teleport behind target
        Vector direction = target.getLocation().getDirection().normalize();
        Location behindTarget = targetLoc.clone().subtract(direction.multiply(2));
        
        player.teleport(behindTarget);
        
        // Calculate damage
        double healthPercent = target.getHealth() / target.getMaxHealth();
        double baseDamage = 10; // 5 hearts
        boolean lowHpBonus = false;
        
        if (healthPercent < 0.5) {
            baseDamage *= 1.5; // 50% bonus
            lowHpBonus = true;
        }
        
        // Apply damage
        target.damage(baseDamage, player);
        
        // Apply shadow mark
        markedEnemies.get(player.getUniqueId()).put(target.getUniqueId(), System.currentTimeMillis());
        
        // Effects
        player.getWorld().playSound(targetLoc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1, 0.5f);
        player.getWorld().playSound(targetLoc, Sound.ENTITY_ENDERMAN_HURT, 1, 0.8f);
        
        // Particle explosion
        target.getWorld().spawnParticle(Particle.CRIT, targetLoc.clone().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.5);
        target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, targetLoc.clone().add(0, 1, 0), 3, 0.3, 0.3, 0.3, 0);
        
        if (lowHpBonus) {
            target.getWorld().spawnParticle(Particle.FLASH, targetLoc.clone().add(0, 1, 0), 1);
            target.sendMessage(Component.text("§c§lEXECUTE!"));
        }
        
        // Mark visual
        target.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, targetLoc.clone().add(0, 2, 0), 20, 0.3, 0.3, 0.3, 0.02);
        
        player.sendMessage(Component.text("§5Finisher Strike! §7(" + Math.round(baseDamage) + " damage)"));
        
        if (lowHpBonus) {
            player.sendMessage(Component.text("§d§lEXECUTION BONUS!"));
        }
    }
    
    private void endShadowMode(Player player) {
        UUID playerId = player.getUniqueId();
        shadowMode.put(playerId, false);
        
        // Remove all clones
        List<ArmorStand> playerClones = clones.get(playerId);
        if (playerClones != null) {
            for (ArmorStand clone : playerClones) {
                clone.remove();
            }
        }
        
        // Grand Finale - all marks explode
        Map<UUID, Long> marks = markedEnemies.get(playerId);
        if (marks != null) {
            for (UUID enemyId : marks.keySet()) {
                Player enemy = Bukkit.getPlayer(enemyId);
                if (enemy != null && enemy.isOnline()) {
                    // Explode mark
                    enemy.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, enemy.getLocation().add(0, 1, 0), 1);
                    enemy.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, enemy.getLocation().add(0, 1, 0), 100, 1, 1, 1, 0.1);
                    
                    // Darken screen (via blindness)
                    enemy.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
                    
                    enemy.sendMessage(Component.text("§5The shadows consume you..."));
                }
            }
        }
        
        // Remove effects
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        
        // Final effect
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1, 0.8f);
        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 200, 2, 2, 2, 2);
        
        // Clean up
        markedEnemies.remove(playerId);
        clones.remove(playerId);
        
        player.sendMessage(Component.text("§5Shadow mode ends..."));
    }
    
    // Called from listener for mark bonus damage
    public boolean hasMark(Player player, Player target) {
        Map<UUID, Long> marks = markedEnemies.get(player.getUniqueId());
        if (marks == null) return false;
        
        Long markTime = marks.get(target.getUniqueId());
        if (markTime == null) return false;
        
        // Mark lasts 3 seconds
        if (System.currentTimeMillis() - markTime < 3000) {
            return true;
        } else {
            marks.remove(target.getUniqueId());
            return false;
        }
    }
    
    // Called from listener for mark damage bonus
    public double getMarkBonus(Player player, Player target) {
        return hasMark(player, target) ? 1.15 : 1.0; // 15% bonus damage
    }
          }
