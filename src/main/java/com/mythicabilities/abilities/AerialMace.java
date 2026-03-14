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
import java.util.*;

public class AerialMace extends Ability {
    
    private final MythicAbilities plugin;
    private final Map<UUID, Boolean> isAirborne = new HashMap<>();
    private final Map<UUID, Integer> smashCount = new HashMap<>();
    private final Map<UUID, Player> targetMap = new HashMap<>();
    private final Random random = new Random();
    
    public AerialMace(MythicAbilities plugin) {
        super("aerial_mace", "§e§lAerial Mace", 25, createIcon());
        this.plugin = plugin;
    }
    
    private static ItemStack createIcon() {
        ItemStack icon = new ItemStack(Material.MACE);
        ItemMeta meta = icon.getItemMeta();
        meta.displayName(Component.text("§e§lAerial Mace"));
        meta.lore(List.of(
            Component.text("§7Auto-triggers after 3 combos!"),
            Component.text("§7Launch into the air and smash!"),
            Component.text("§7• 5 consecutive ground pounds"),
            Component.text("§7• Weapon enhances damage:"),
            Component.text("§7  §fMace: §e3x damage + explosion"),
            Component.text("§7  §fSword/Axe: §e2x damage + crits"),
            Component.text("§7  §fBare hands: §e1.5x + ripple waves"),
            Component.text("§7• 10 seconds of aerial domination"),
            Component.text("§eCooldown: 25 seconds")
        ));
        icon.setItemMeta(meta);
        return icon;
    }
    
    @Override
    public void onTrigger(Player player) {
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
            activateAerialMace(player, nearestEnemy);
        } else {
            player.sendMessage(Component.text("§eNo enemies nearby to smash!"));
        }
    }
    
    @Override
    public void onCombo(Player player, Player target) {
        activateAerialMace(player, target);
    }
    
    private void activateAerialMace(Player player, Player target) {
        UUID playerId = player.getUniqueId();
        
        // Launch player 10 blocks up
        Location launchLoc = player.getLocation().clone();
        launchLoc.setY(launchLoc.getY() + 12);
        
        player.teleport(launchLoc);
        player.setAllowFlight(true);
        player.setFlying(true);
        
        isAirborne.put(playerId, true);
        smashCount.put(playerId, 0);
        targetMap.put(playerId, target);
        
        // Effects
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1, 0.5f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 1, 0.8f);
        
        // Title
        player.showTitle(Title.title(
            Component.text("§e§lAERIAL MACE"),
            Component.text("§6Get ready to smash!"),
            Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(1), Duration.ofMillis(500))
        ));
        
        target.showTitle(Title.title(
            Component.text("§c§lLOOK UP!"),
            Component.text("§eYou're about to get smashed"),
            Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(1), Duration.ofMillis(500))
        ));
        
        // Particle trail above
        startAirTrail(player);
        
        // Start smashes
        startSmashes(player, target);
        
        // End after 10 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                if (isAirborne.getOrDefault(playerId, false)) {
                    endAerialMace(player);
                }
            }
        }.runTaskLater(plugin, 200); // 10 seconds
    }
    
    private void startAirTrail(Player player) {
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 200 || !isAirborne.getOrDefault(player.getUniqueId(), false)) {
                    this.cancel();
                    return;
                }
                
                // Cloud trail
                Location loc = player.getLocation();
                player.getWorld().spawnParticle(Particle.CLOUD, loc.clone().add(0, -1, 0), 5, 0.3, 0.1, 0.3, 0.02);
                player.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, -1, 0), 3, 0.2, 0.1, 0.2, 0.01);
                
                ticks += 5;
            }
        }.runTaskTimer(plugin, 0, 5);
    }
    
    private void startSmashes(Player player, Player target) {
        new BukkitRunnable() {
            int smashNumber = 0;
            
            @Override
            public void run() {
                if (smashNumber >= 5 || !isAirborne.getOrDefault(player.getUniqueId(), false)) {
                    this.cancel();
                    return;
                }
                
                // Perform smash
                performSmash(player, target, smashNumber + 1);
                smashNumber++;
                
                // Update count
                smashCount.put(player.getUniqueId(), smashNumber);
                
                // Wait 1.5 seconds between smashes
                if (smashNumber < 5) {
                    // Don't cancel, just wait for next
                } else {
                    // All smashes done
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            endAerialMace(player);
                        }
                    }.runTaskLater(plugin, 20); // 1 second delay after last smash
                }
            }
        }.runTaskTimer(plugin, 10, 30); // First smash after 0.5 sec, then every 1.5 sec
    }
    
    private void performSmash(Player player, Player target, int smashNum) {
        // Get weapon type
        ItemStack weapon = player.getInventory().getItemInMainHand();
        WeaponType weaponType = getWeaponType(weapon);
        
        // Calculate damage
        double baseDamage = 4;
        double multiplier = getDamageMultiplier(weaponType);
        double finalDamage = baseDamage * multiplier;
        
        // Get target location
        Location targetLoc = target.getLocation().clone();
        
        // Teleport player above target
        Location smashLoc = targetLoc.clone().add(0, 7, 0);
        player.teleport(smashLoc);
        
        // Smash down effect
        player.setVelocity(new Vector(0, -1.5, 0));
        
        // Play sound
        player.getWorld().playSound(targetLoc, Sound.ENTITY_GENERIC_EXPLODE, 1, 0.8f);
        
        // Damage target
        target.damage(finalDamage, player);
        
        // Apply slowness to nearby enemies
        for (Entity entity : player.getWorld().getNearbyEntities(targetLoc, 5, 3, 5)) {
            if (entity instanceof LivingEntity enemy && !enemy.equals(player)) {
                enemy.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));
            }
        }
        
        // Particle effects based on weapon
        spawnSmashParticles(player, targetLoc, weaponType, smashNum);
        
        // Screen shake for target
        target.showTitle(Title.title(
            Component.text("§c§lSMASH " + smashNum + "!"),
            Component.text("§7" + Math.round(finalDamage) + " damage"),
            Title.Times.times(Duration.ZERO, Duration.ofMillis(500), Duration.ZERO)
        ));
        
        // Message
        player.sendMessage(Component.text("§eSmash " + smashNum + "/5 - §6" + Math.round(finalDamage) + " damage"));
    }
    
    private enum WeaponType {
        MACE, SWORD, AXE, BARE, OTHER
    }
    
    private WeaponType getWeaponType(ItemStack weapon) {
        if (weapon == null || weapon.getType() == Material.AIR) {
            return WeaponType.BARE;
        }
        
        Material type = weapon.getType();
        String name = type.toString();
        
        if (name.contains("MACE")) {
            return WeaponType.MACE;
        } else if (name.contains("SWORD")) {
            return WeaponType.SWORD;
        } else if (name.contains("AXE") && !name.contains("PICKAXE")) {
            return WeaponType.AXE;
        } else {
            return WeaponType.OTHER;
        }
    }
    
    private double getDamageMultiplier(WeaponType type) {
        switch (type) {
            case MACE:
                return 3.0;
            case SWORD:
            case AXE:
                return 2.0;
            case BARE:
                return 1.5;
            default:
                return 1.2;
        }
    }
    
    private void spawnSmashParticles(Player player, Location loc, WeaponType type, int smashNum) {
        World world = player.getWorld();
        
        switch (type) {
            case MACE:
                // Golden explosion
                world.spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);
                world.spawnParticle(Particle.FLASH, loc, 1);
                world.spawnParticle(Particle.ELECTRIC_SPARK, loc, 30, 1, 1, 1, 0.2);
                world.spawnParticle(Particle.GLOW, loc, 50, 1, 1, 1, 0.1);
                
                // Lightning effect (visual only)
                if (smashNum % 2 == 0) {
                    world.strikeLightningEffect(loc);
                }
                break;
                
            case SWORD:
                // Critical hit particles
                world.spawnParticle(Particle.CRIT, loc, 50, 1.5, 1, 1.5, 0.5);
                world.spawnParticle(Particle.SWEEP_ATTACK, loc, 3, 1, 0.5, 1, 0);
                world.spawnParticle(Particle.DAMAGE_INDICATOR, loc, 20, 1, 1, 1, 0);
                break;
                
            case AXE:
                // Heavy hit particles
                world.spawnParticle(Particle.BLOCK, loc, 40, 1.2, 0.8, 1.2, Material.OAK_PLANKS.createBlockData());
                world.spawnParticle(Particle.ITEM, loc, 30, 1, 1, 1, 0.1, Material.OAK_LOG);
                break;
                
            case BARE:
                // Ripple/shockwave effect
                for (int i = 0; i < 3; i++) {
                    double radius = 2 + i * 1.5;
                    for (int angle = 0; angle < 360; angle += 20) {
                        double rad = Math.toRadians(angle);
                        double x = loc.getX() + radius * Math.cos(rad);
                        double z = loc.getZ() + radius * Math.sin(rad);
                        
                        Location ringLoc = new Location(world, x, loc.getY() + 0.2, z);
                        world.spawnParticle(Particle.RIPPLE, ringLoc, 1, 0, 0, 0, 0.1);
                        world.spawnParticle(Particle.BUBBLE, ringLoc, 2, 0.2, 0.2, 0.2, 0);
                    }
                }
                
                // Shockwave rings
                new BukkitRunnable() {
                    int wave = 0;
                    
                    @Override
                    public void run() {
                        if (wave >= 3) {
                            this.cancel();
                            return;
                        }
                        
                        double radius = 2 + wave * 2;
                        for (int angle = 0; angle < 360; angle += 15) {
                            double rad = Math.toRadians(angle);
                            double x = loc.getX() + radius * Math.cos(rad);
                            double z = loc.getZ() + radius * Math.sin(rad);
                            
                            Location waveLoc = new Location(world, x, loc.getY() + 0.3, z);
                            world.spawnParticle(Particle.CLOUD, waveLoc, 2, 0.1, 0.1, 0.1, 0.02);
                        }
                        
                        wave++;
                    }
                }.runTaskTimer(plugin, 0, 5);
                break;
                
            default:
                // Generic particles
                world.spawnParticle(Particle.CLOUD, loc, 30, 1.5, 1, 1.5, 0.1);
                world.spawnParticle(Particle.END_ROD, loc, 20, 1, 1, 1, 0.1);
                break;
        }
        
        // Ground crack effect (common for all)
        for (int i = 0; i < 8; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double x = loc.getX() + 2 * Math.cos(angle);
            double z = loc.getZ() + 2 * Math.sin(angle);
            
            Location crackLoc = new Location(world, x, loc.getY(), z);
            world.spawnParticle(Particle.BLOCK, crackLoc, 5, 0.2, 0.1, 0.2, Material.COBBLESTONE.createBlockData());
        }
    }
    
    private void endAerialMace(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Bring player down safely
        Location landing = player.getLocation().clone();
        landing.setY(player.getWorld().getHighestBlockYAt(landing) + 1);
        player.teleport(landing);
        
        player.setAllowFlight(false);
        player.setFlying(false);
        
        isAirborne.remove(playerId);
        smashCount.remove(playerId);
        targetMap.remove(playerId);
        
        // Final effects
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 0.8f);
        player.getWorld().spawnParticle(Particle.FIREWORK, player.getLocation(), 30, 1, 1, 1, 0.1);
        
        player.sendMessage(Component.text("§eAerial Mace completed!"));
    }
    
    @Override
    public void onLeftClick(Player player) {
        // Not used
    }
    
    @Override
    public void onRightClick(Player player) {
        // Manual trigger
        onTrigger(player);
    }
  }
