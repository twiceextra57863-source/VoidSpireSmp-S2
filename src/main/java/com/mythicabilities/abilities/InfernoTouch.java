package com.mythicabilities.abilities;

import com.mythicabilities.MythicAbilities;
import com.mythicabilities.utils.ParticleEffects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InfernoTouch extends Ability {
    
    private final MythicAbilities plugin;
    private final List<UUID> inDomain = new ArrayList<>();
    private Location domainCenter;
    private List<ArmorStand> domainBlocks = new ArrayList<>();
    
    public InfernoTouch(MythicAbilities plugin) {
        super("inferno_touch", "§c§lInferno Touch", 30, createIcon());
        this.plugin = plugin;
    }
    
    private static ItemStack createIcon() {
        ItemStack icon = new ItemStack(Material.BLAZE_POWDER);
        ItemMeta meta = icon.getItemMeta();
        meta.displayName(Component.text("§c§lInferno Touch"));
        meta.lore(List.of(
            Component.text("§7Right-click to use domain after combo"),
            Component.text("§7Left-click to shoot fire projectiles"),
            Component.text("§cCooldown: 30 seconds")
        ));
        icon.setItemMeta(meta);
        return icon;
    }
    
    @Override
    public void onRightClick(Player player) {
        if (plugin.getAbilityManager().getCombo(player) >= 3) {
            createDomain(player);
            plugin.getAbilityManager().resetCombo(player);
        } else {
            player.sendMessage(Component.text("§cYou need 3 combos first! Current: " + 
                plugin.getAbilityManager().getCombo(player)));
        }
    }
    
    @Override
    public void onLeftClick(Player player) {
        if (inDomain.contains(player.getUniqueId())) {
            shootFireProjectile(player);
        }
    }
    
    @Override
    public void onCombo(Player player, Player target) {
        plugin.getAbilityManager().addCombo(player);
        
        // Visual combo effect
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        player.spawnParticle(Particle.FLAME, player.getLocation().add(0, 2, 0), 20, 0.5, 0.5, 0.5, 0.1);
        
        player.sendActionBar(Component.text("§cCombo: " + 
            plugin.getAbilityManager().getCombo(player) + "/3"));
    }
    
    private void createDomain(Player player) {
        domainCenter = player.getLocation().clone();
        inDomain.clear();
        domainBlocks.clear();
        
        // Get all nearby players
        for (Entity entity : player.getWorld().getNearbyEntities(domainCenter, 10, 5, 10)) {
            if (entity instanceof Player target && !target.equals(player)) {
                inDomain.add(target.getUniqueId());
            }
        }
        inDomain.add(player.getUniqueId());
        
        // Create domain walls
        createDomainWalls(player);
        
        // Apply effects to enemies
        for (UUID uuid : inDomain) {
            Player target = Bukkit.getPlayer(uuid);
            if (target != null && !target.equals(player)) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 200, 1));
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 0));
                
                // Particle effect on enemies
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        if (ticks >= 200 || !inDomain.contains(target.getUniqueId())) {
                            this.cancel();
                            return;
                        }
                        target.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, 
                            target.getLocation().add(0, 1, 0), 5, 0.3, 0.5, 0.3, 0.05);
                        ticks += 5;
                    }
                }.runTaskTimer(plugin, 0, 5);
            }
        }
        
        // Power-up effect for user
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 200, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 1));
        
        // Visual effects
        for (int i = 0; i < 360; i += 10) {
            double rad = Math.toRadians(i);
            double x = domainCenter.getX() + 10 * Math.cos(rad);
            double z = domainCenter.getZ() + 10 * Math.sin(rad);
            Location loc = new Location(player.getWorld(), x, domainCenter.getY(), z);
            player.getWorld().spawnParticle(Particle.FLAME, loc, 10, 0, 1, 0, 0.1);
        }
        
        player.sendMessage(Component.text("§c§lDOMAIN EXPANSION: INFERNO REALM!"));
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1, 0.5f);
        
        // Remove domain after 10 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                removeDomain(player);
            }
        }.runTaskLater(plugin, 200); // 10 seconds = 200 ticks
    }
    
    private void createDomainWalls(Player player) {
        for (int y = 0; y < 5; y++) {
            for (int angle = 0; angle < 360; angle += 30) {
                double rad = Math.toRadians(angle);
                double x = domainCenter.getX() + 10 * Math.cos(rad);
                double z = domainCenter.getZ() + 10 * Math.sin(rad);
                Location loc = new Location(player.getWorld(), x, domainCenter.getY() + y, z);
                
                ArmorStand stand = player.getWorld().spawn(loc, ArmorStand.class);
                stand.setInvisible(true);
                stand.setMarker(true);
                stand.setGravity(false);
                stand.setSmall(true);
                
                // Store block data for removal
                domainBlocks.add(stand);
                
                // Particle wall
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!inDomain.contains(player.getUniqueId())) {
                            this.cancel();
                            return;
                        }
                        player.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(0, 1, 0), 
                            3, 0.3, 0.3, 0.3, 0.02);
                    }
                }.runTaskTimer(plugin, 0, 5);
            }
        }
    }
    
    private void shootFireProjectile(Player player) {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize().multiply(1.5);
        
        // Spawn projectile effect
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 1);
        
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
                
                // Particle trail
                player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 10, 0.2, 0.2, 0.2, 0.05);
                player.getWorld().spawnParticle(Particle.LAVA, loc, 3, 0.1, 0.1, 0.1, 0);
                
                // Check for hits
                for (Entity entity : player.getWorld().getNearbyEntities(loc, 1, 1, 1)) {
                    if (entity instanceof LivingEntity target && !entity.equals(player)) {
                        // Explosion effect
                        target.getWorld().createExplosion(target.getLocation(), 2, false, false);
                        target.setVelocity(target.getLocation().getDirection().multiply(-2).setY(0.5));
                        
                        // Damage
                        target.damage(6, player);
                        
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
        inDomain.clear();
        
        // Remove wall blocks
        for (ArmorStand stand : domainBlocks) {
            stand.remove();
        }
        domainBlocks.clear();
        
        // Return all players to original positions
        for (UUID uuid : inDomain) {
            Player target = Bukkit.getPlayer(uuid);
            if (target != null) {
                target.teleport(domainCenter);
            }
        }
        
        // Clear effects
        player.removePotionEffect(PotionEffectType.STRENGTH);
        player.removePotionEffect(PotionEffectType.SPEED);
        
        player.sendMessage(Component.text("§cDomain has dissipated!"));
    }
                  }
