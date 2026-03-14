package com.mythicabilities.abilities;

import com.mythicabilities.MythicAbilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Block;
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

public class FirstOfStone extends Ability {
    
    private final MythicAbilities plugin;
    private final Map<UUID, Boolean> earthMode = new HashMap<>();
    private final Map<UUID, Integer> stompCooldown = new HashMap<>();
    private final Random random = new Random();
    
    public FirstOfStone(MythicAbilities plugin) {
        super("first_of_stone", "§7§lFirst of Stone", 35, createIcon());
        this.plugin = plugin;
    }
    
    private static ItemStack createIcon() {
        ItemStack icon = new ItemStack(Material.STONE_BRICKS);
        ItemMeta meta = icon.getItemMeta();
        meta.displayName(Component.text("§7§lFirst of Stone"));
        meta.lore(List.of(
            Component.text("§7Auto-triggers after 3 combos!"),
            Component.text("§7Become a living siege engine!"),
            Component.text("§7• Left Click: Throw massive boulders"),
            Component.text("§7  §f- 8 damage to players"),
            Component.text("§7  §f- 24 damage to structures"),
            Component.text("§7• Right Click: Earth Quake Stomp"),
            Component.text("§7  §f- Damage + Slowness to enemies"),
            Component.text("§7• Stone Skin: 40% damage reduction"),
            Component.text("§7• Finale: Landslide explosion"),
            Component.text("§7Cooldown: 35 seconds")
        ));
        icon.setItemMeta(meta);
        return icon;
    }
    
    @Override
    public void onTrigger(Player player) {
        activateEarthMode(player);
    }
    
    @Override
    public void onCombo(Player player, Player target) {
        activateEarthMode(player);
    }
    
    private void activateEarthMode(Player player) {
        UUID playerId = player.getUniqueId();
        earthMode.put(playerId, true);
        
        // Broadcast
        Bukkit.broadcast(Component.text("§7§l✦✦✦ FIRST OF STONE AWAKENS! ✦✦✦"));
        Bukkit.broadcast(Component.text("§fThe earth trembles..."));
        
        // Title
        player.showTitle(Title.title(
            Component.text("§7§lEARTH SHATTERER"),
            Component.text("§fYou are the First of Stone"),
            Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(1))
        ));
        
        // Play awakening sound
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1, 0.7f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1, 0.5f);
        
        // Stone Skin effects
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 300, 2)); // 40% reduction
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 300, 0)); // Slightly slow but worth it
        
        // Visual transformation
        player.getWorld().spawnParticle(Particle.BLOCK, player.getLocation().add(0, 1, 0), 
            100, 0.5, 1, 0.5, Material.STONE.createBlockData());
        player.getWorld().spawnParticle(Particle.BLOCK, player.getLocation(), 
            50, 0.5, 0.5, 0.5, Material.COBBLESTONE.createBlockData());
        
        // Start stone particles
        startStoneParticles(player);
        
        // End after 15 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                endEarthMode(player);
            }
        }.runTaskLater(plugin, 300); // 15 seconds
    }
    
    private void startStoneParticles(Player player) {
        UUID playerId = player.getUniqueId();
        
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 300 || !earthMode.getOrDefault(playerId, false)) {
                    this.cancel();
                    return;
                }
                
                Location loc = player.getLocation();
                
                // Stone aura particles
                player.getWorld().spawnParticle(Particle.BLOCK, loc.clone().add(0, 1, 0), 
                    5, 0.5, 0.5, 0.5, Material.STONE.createBlockData());
                player.getWorld().spawnParticle(Particle.FALLING_LAVA, loc.clone().add(0, 1, 0), 
                    2, 0.3, 0.3, 0.3, 0);
                
                // Ground cracks
                if (ticks % 10 == 0) {
                    for (int i = 0; i < 4; i++) {
                        double angle = random.nextDouble() * 2 * Math.PI;
                        double x = loc.getX() + 2 * Math.cos(angle);
                        double z = loc.getZ() + 2 * Math.sin(angle);
                        
                        Location crackLoc = new Location(player.getWorld(), x, loc.getY(), z);
                        player.getWorld().spawnParticle(Particle.BLOCK, crackLoc, 
                            10, 0.2, 0.1, 0.2, Material.COBBLESTONE.createBlockData());
                    }
                }
                
                ticks += 5;
            }
        }.runTaskTimer(plugin, 0, 5);
    }
    
    @Override
    public void onLeftClick(Player player) {
        if (earthMode.getOrDefault(player.getUniqueId(), false)) {
            throwBoulder(player);
        }
    }
    
    private void throwBoulder(Player player) {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize().multiply(2);
        
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1, 0.6f);
        
        new BukkitRunnable() {
            Location loc = eye.clone();
            int distance = 0;
            
            @Override
            public void run() {
                if (distance > 30) {
                    this.cancel();
                    return;
                }
                
                loc.add(direction);
                distance++;
                
                // Boulder trail
                player.getWorld().spawnParticle(Particle.BLOCK, loc, 10, 0.3, 0.3, 0.3, 
                    Material.STONE.createBlockData());
                player.getWorld().spawnParticle(Particle.SMOKE, loc, 5, 0.2, 0.2, 0.2, 0.02);
                player.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc, 2, 0.1, 0.1, 0.1, 0.01);
                
                // Periodic sound
                if (distance % 5 == 0) {
                    player.getWorld().playSound(loc, Sound.ENTITY_WITHER_AMBIENT, 0.3f, 1.2f);
                }
                
                // Check for hits
                for (Entity entity : player.getWorld().getNearbyEntities(loc, 2, 2, 2)) {
                    if (entity instanceof LivingEntity target && !entity.equals(player)) {
                        target.damage(8, player); // 4 hearts
                        
                        // Explosion effect
                        target.getWorld().createExplosion(target.getLocation(), 0f, false, false);
                        target.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, target.getLocation(), 1);
                        target.getWorld().spawnParticle(Particle.BLOCK, target.getLocation().add(0, 1, 0), 
                            50, 1, 1, 1, Material.STONE.createBlockData());
                        
                        // Knockback
                        target.setVelocity(direction.clone().multiply(1.5).setY(0.5));
                        
                        this.cancel();
                        return;
                    }
                }
                
                // Check for block hits
                if (loc.getBlock().getType().isSolid()) {
                    // Boulder hit structure
                    loc.getBlock().getWorld().createExplosion(loc, 0f, false, false);
                    loc.getBlock().getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);
                    loc.getBlock().getWorld().spawnParticle(Particle.BLOCK, loc, 
                        80, 1, 1, 1, Material.STONE.createBlockData());
                    
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    @Override
    public void onRightClick(Player player) {
        if (earthMode.getOrDefault(player.getUniqueId(), false)) {
            performEarthStomp(player);
        }
    }
    
    private void performEarthStomp(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Check cooldown
        int lastStomp = stompCooldown.getOrDefault(playerId, 0);
        int currentTime = (int) (System.currentTimeMillis() / 1000);
        
        if (currentTime - lastStomp < 5) {
            player.sendActionBar(Component.text("§7Stomp on cooldown!"));
            return;
        }
        
        stompCooldown.put(playerId, currentTime);
        
        Location loc = player.getLocation();
        World world = player.getWorld();
        
        // Sound
        world.playSound(loc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1, 0.5f);
        world.playSound(loc, Sound.BLOCK_ANVIL_LAND, 1, 0.3f);
        
        // Particle ring
        for (int angle = 0; angle < 360; angle += 10) {
            double rad = Math.toRadians(angle);
            double x = loc.getX() + 4 * Math.cos(rad);
            double z = loc.getZ() + 4 * Math.sin(rad);
            
            Location ringLoc = new Location(world, x, loc.getY(), z);
            world.spawnParticle(Particle.BLOCK, ringLoc, 5, 0.2, 0.1, 0.2, 
                Material.STONE.createBlockData());
            world.spawnParticle(Particle.CLOUD, ringLoc, 3, 0.1, 0.1, 0.1, 0.02);
        }
        
        // Damage and slow enemies
        for (Entity entity : world.getNearbyEntities(loc, 5, 3, 5)) {
            if (entity instanceof LivingEntity target && !entity.equals(player)) {
                target.damage(4, player); // 2 hearts
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 2));
                
                // Hit effect
                target.getWorld().spawnParticle(Particle.BLOCK, target.getLocation().add(0, 1, 0), 
                    20, 0.5, 0.5, 0.5, Material.COBBLESTONE.createBlockData());
            }
        }
    }
    
    private void endEarthMode(Player player) {
        UUID playerId = player.getUniqueId();
        earthMode.put(playerId, false);
        
        // Grand Finale - Landslide
        World world = player.getWorld();
        
        for (int i = 0; i < 10; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double x = player.getLocation().getX() + 8 * Math.cos(angle);
            double z = player.getLocation().getZ() + 8 * Math.sin(angle);
            double y = player.getLocation().getY() + 5;
            
            Location boulderLoc = new Location(world, x, y, z);
            Vector dir = player.getLocation().toVector().subtract(boulderLoc.toVector()).normalize();
            
            world.spawnParticle(Particle.BLOCK, boulderLoc, 30, 0.5, 0.5, 0.5, Material.STONE.createBlockData());
            world.spawnParticle(Particle.EXPLOSION_EMITTER, boulderLoc, 1);
        }
        
        world.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 2, 0.5f);
        
        // Remove effects
        player.removePotionEffect(PotionEffectType.RESISTANCE);
        player.removePotionEffect(PotionEffectType.SLOWNESS);
        
        player.sendMessage(Component.text("§7First of Stone returns to slumber..."));
    }
                    }
