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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InfernoTouch extends Ability {
    
    private final MythicAbilities plugin;
    private final List<UUID> inDomain = new ArrayList<>();
    private Location domainCenter;
    private List<Block> domainBlocks = new ArrayList<>(); // ArmorStand ki jagah Block use karenge
    private boolean domainActive = false;
    
    public InfernoTouch(MythicAbilities plugin) {
        super("inferno_touch", "§c§lInferno Touch", 30, createIcon());
        this.plugin = plugin;
    }
    
    private static ItemStack createIcon() {
        ItemStack icon = new ItemStack(Material.BLAZE_POWDER);
        ItemMeta meta = icon.getItemMeta();
        meta.displayName(Component.text("§c§lInferno Touch"));
        meta.lore(List.of(
            Component.text("§7Auto-triggers after 3 combos!"),
            Component.text("§7Creates a fiery domain"),
            Component.text("§7Enemies get weakness, you get strength"),
            Component.text("§cCooldown: 30 seconds")
        ));
        icon.setItemMeta(meta);
        return icon;
    }
    
    @Override
    public void onTrigger(Player player) {
        // Direct trigger without right-click
        createDomain(player);
    }
    
    @Override
    public void onRightClick(Player player) {
        // Optional - can also trigger manually
        if (!domainActive) {
            createDomain(player);
        }
    }
    
    @Override
    public void onLeftClick(Player player) {
        if (domainActive && inDomain.contains(player.getUniqueId())) {
            shootFireProjectile(player);
        }
    }
    
    @Override
    public void onCombo(Player player, Player target) {
        // Yeh method tab call hoga jab player 3 baar combo karega
        // Target ko damage nahi hoga, sirf trigger hoga
        if (!domainActive) {
            onTrigger(player);
        }
    }
    
    private void createDomain(Player player) {
        if (domainActive) return;
        
        domainActive = true;
        domainCenter = player.getLocation().clone();
        inDomain.clear();
        domainBlocks.clear();
        
        // Get all nearby players (enemies)
        for (Entity entity : player.getWorld().getNearbyEntities(domainCenter, 10, 5, 10)) {
            if (entity instanceof Player target && !target.equals(player)) {
                inDomain.add(target.getUniqueId());
            }
        }
        inDomain.add(player.getUniqueId()); // User bhi domain me hai
        
        // Create glass dome
        createGlassDome(player);
        
        // Apply effects to enemies
        for (UUID uuid : inDomain) {
            Player target = Bukkit.getPlayer(uuid);
            if (target != null && !target.equals(player)) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 200, 1));
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 0));
                target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 200, 0));
                
                // Fire particles on enemies
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        if (ticks >= 200 || !inDomain.contains(target.getUniqueId()) || !domainActive) {
                            this.cancel();
                            return;
                        }
                        target.getWorld().spawnParticle(Particle.FLAME, 
                            target.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.05);
                        target.getWorld().spawnParticle(Particle.LAVA, 
                            target.getLocation().add(0, 1, 0), 3, 0.3, 0.5, 0.3, 0);
                        ticks += 5;
                    }
                }.runTaskTimer(plugin, 0, 5);
            }
        }
        
        // Power-up effects for user (no damage)
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 200, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 200, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 0));
        
        // Visual effects
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1, 0.5f);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 1, 0.8f);
        
        // Title message
        player.showTitle(Title.title(
            Component.text("§c§lDOMAIN EXPANSION"),
            Component.text("§6§lINFERNO REALM"),
            Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(1))
        ));
        
        for (UUID uuid : inDomain) {
            Player target = Bukkit.getPlayer(uuid);
            if (target != null && !target.equals(player)) {
                target.showTitle(Title.title(
                    Component.text("§c§lDOMAIN"),
                    Component.text("§6§lYOU'RE TRAPPED!"),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(1), Duration.ofMillis(500))
                ));
            }
        }
        
        // Remove domain after 10 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                removeDomain(player);
            }
        }.runTaskLater(plugin, 200); // 200 ticks = 10 seconds
    }
    
    private void createGlassDome(Player player) {
        World world = player.getWorld();
        int radius = 8;
        
        // Create a hemisphere of glass
        for (int x = -radius; x <= radius; x++) {
            for (int y = 0; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    double distance = Math.sqrt(x*x + y*y + z*z);
                    
                    // Create hollow dome (only shell)
                    if (distance > radius - 0.5 && distance < radius + 0.5 && y >= 0) {
                        Block block = world.getBlockAt(
                            domainCenter.getBlockX() + x,
                            domainCenter.getBlockY() + y,
                            domainCenter.getBlockZ() + z
                        );
                        
                        // Only replace air blocks
                        if (block.getType() == Material.AIR) {
                            block.setType(Material.RED_STAINED_GLASS);
                            domainBlocks.add(block);
                        }
                    }
                }
            }
        }
        
        // Add floor
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double distance = Math.sqrt(x*x + z*z);
                if (distance <= radius) {
                    Block block = world.getBlockAt(
                        domainCenter.getBlockX() + x,
                        domainCenter.getBlockY() - 1,
                        domainCenter.getBlockZ() + z
                    );
                    
                    if (block.getType() == Material.AIR || block.getType() == Material.WATER) {
                        block.setType(Material.NETHER_BRICKS);
                        domainBlocks.add(block);
                    }
                }
            }
        }
        
        // Add fire particles around dome
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 200 || !domainActive) {
                    this.cancel();
                    return;
                }
                
                for (int i = 0; i < 10; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double x = domainCenter.getX() + radius * Math.cos(angle);
                    double z = domainCenter.getZ() + radius * Math.sin(angle);
                    double y = domainCenter.getY() + Math.random() * radius;
                    
                    Location loc = new Location(world, x, y, z);
                    world.spawnParticle(Particle.FLAME, loc, 5, 0.2, 0.2, 0.2, 0.02);
                    world.spawnParticle(Particle.LAVA, loc, 2, 0.2, 0.2, 0.2, 0);
                }
                
                ticks += 5;
            }
        }.runTaskTimer(plugin, 0, 5);
    }
    
    private void shootFireProjectile(Player player) {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize().multiply(1.5);
        
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 1);
        
        new BukkitRunnable() {
            Location loc = eye.clone();
            int distance = 0;
            
            @Override
            public void run() {
                if (distance > 30 || !domainActive) {
                    this.cancel();
                    return;
                }
                
                loc.add(direction);
                distance++;
                
                // Particle trail
                player.getWorld().spawnParticle(Particle.FLAME, loc, 15, 0.2, 0.2, 0.2, 0.05);
                player.getWorld().spawnParticle(Particle.LAVA, loc, 5, 0.1, 0.1, 0.1, 0);
                player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 8, 0.2, 0.2, 0.2, 0.03);
                
                // Check for hits (only enemies, not user)
                for (Entity entity : player.getWorld().getNearbyEntities(loc, 1.5, 1.5, 1.5)) {
                    if (entity instanceof LivingEntity target && !entity.equals(player) && inDomain.contains(target.getUniqueId())) {
                        // Explosion effect (no block damage)
                        target.getWorld().createExplosion(target.getLocation(), 0f, false, false);
                        target.setVelocity(direction.clone().multiply(2).setY(0.5));
                        
                        // Damage (6 = 3 hearts)
                        target.damage(6, player);
                        
                        // Fire effect
                        target.setFireTicks(60);
                        
                        // Particle explosion
                        target.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, 
                            target.getLocation(), 1);
                        
                        this.cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    private void removeDomain(Player player) {
        // Remove all glass blocks
        for (Block block : domainBlocks) {
            if (block.getType() == Material.RED_STAINED_GLASS || 
                block.getType() == Material.NETHER_BRICKS) {
                block.setType(Material.AIR);
            }
        }
        
        domainBlocks.clear();
        domainActive = false;
        
        // Clear effects
        player.removePotionEffect(PotionEffectType.STRENGTH);
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
        player.removePotionEffect(PotionEffectType.REGENERATION);
        
        for (UUID uuid : inDomain) {
            Player target = Bukkit.getPlayer(uuid);
            if (target != null) {
                target.removePotionEffect(PotionEffectType.WEAKNESS);
                target.removePotionEffect(PotionEffectType.SLOWNESS);
                target.removePotionEffect(PotionEffectType.WITHER);
            }
        }
        
        inDomain.clear();
        
        player.sendMessage(Component.text("§cDomain has dissipated!"));
        player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 1, 0.5f);
    }
                                                }
