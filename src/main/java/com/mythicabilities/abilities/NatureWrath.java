package com.mythicabilities.abilities;

import com.mythicabilities.MythicAbilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
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

public class NaturesWrath extends Ability {
    
    private final MythicAbilities plugin;
    private final Map<UUID, Boolean> natureActive = new HashMap<>();
    private final Map<UUID, List<Block>> natureBlocks = new HashMap<>();
    private final Map<UUID, List<Player>> healedPlayers = new HashMap<>();
    private final Random random = new Random();
    
    // Particle animation variables
    private final double[][] vineOffsets = {
        {0.5, 0, 0.5}, { -0.5, 0, -0.5}, {0.5, 0, -0.5}, { -0.5, 0, 0.5},
        {0.3, 1, 0.3}, { -0.3, 1, -0.3}, {0.3, 1, -0.3}, { -0.3, 1, 0.3}
    };
    
    public NaturesWrath(MythicAbilities plugin) {
        super("natures_wrath", "§2§lNature's Wrath", 30, createIcon());
        this.plugin = plugin;
    }
    
    private static ItemStack createIcon() {
        ItemStack icon = new ItemStack(Material.OAK_SAPLING);
        ItemMeta meta = icon.getItemMeta();
        meta.displayName(Component.text("§2§lNature's Wrath"));
        meta.lore(List.of(
            Component.text("§7Auto-triggers after 3 combos!"),
            Component.text("§7Harness the power of nature"),
            Component.text("§7• Thorn Cage around enemy"),
            Component.text("§7• Vine Whip to pull enemies"),
            Component.text("§7• Healing Aura for allies"),
            Component.text("§7• Poison Spores in cage"),
            Component.text("§7• Nature's Blessing buffs"),
            Component.text("§2Cooldown: 30 seconds")
        ));
        icon.setItemMeta(meta);
        return icon;
    }
    
    @Override
    public void onTrigger(Player player) {
        activateNaturesWrath(player);
    }
    
    @Override
    public void onCombo(Player player, Player target) {
        activateNaturesWrathWithTarget(player, target);
    }
    
    private void activateNaturesWrath(Player player) {
        // Find nearest enemy
        Player nearestEnemy = null;
        double nearestDistance = Double.MAX_VALUE;
        
        for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), 20, 10, 20)) {
            if (entity instanceof Player target && !target.equals(player)) {
                double distance = player.getLocation().distance(target.getLocation());
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestEnemy = target;
                }
            }
        }
        
        activateNaturesWrathWithTarget(player, nearestEnemy);
    }
    
    private void activateNaturesWrathWithTarget(Player player, Player target) {
        if (target == null) {
            player.sendMessage(Component.text("§2No enemies nearby for Nature's Wrath!"));
            return;
        }
        
        UUID playerId = player.getUniqueId();
        natureActive.put(playerId, true);
        natureBlocks.put(playerId, new ArrayList<>());
        healedPlayers.put(playerId, new ArrayList<>());
        
        // Broadcast message
        Bukkit.broadcast(Component.text("§2§l✦✦✦ NATURE'S WRATH ✦✦✦"));
        Bukkit.broadcast(Component.text("§aThe forest awakens..."));
        
        // Title effects
        player.showTitle(Title.title(
            Component.text("§2§lNATURE'S WRATH"),
            Component.text("§aThe forest obeys you"),
            Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(1))
        ));
        
        target.showTitle(Title.title(
            Component.text("§2§lYOU'RE TRAPPED!"),
            Component.text("§aVines rise from the ground"),
            Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(1))
        ));
        
        // Play awakening sound
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GROWING_PLANT_CROP, 1, 0.8f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1, 1.2f);
        
        // Apply nature's blessing to user
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 160, 1)); // 8 seconds
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 160, 2)); // 8 absorption hearts
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 160, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 160, 0));
        
        // Create thorn cage around target
        createThornCage(player, target);
        
        // Start healing aura
        startHealingAura(player);
        
        // Start nature particles
        startNatureParticles(player, target);
        
        // End after 8 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                endNaturesWrath(player);
            }
        }.runTaskLater(plugin, 160); // 8 seconds
    }
    
    private void createThornCage(Player player, Player target) {
        Location center = target.getLocation().clone();
        World world = player.getWorld();
        List<Block> blocks = natureBlocks.get(player.getUniqueId());
        
        // Create circular cage
        int radius = 4;
        int height = 5;
        
        for (int y = 0; y < height; y++) {
            for (int angle = 0; angle < 360; angle += 15) {
                double rad = Math.toRadians(angle);
                double x = center.getX() + radius * Math.cos(rad);
                double z = center.getZ() + radius * Math.sin(rad);
                
                Block block = world.getBlockAt(new Location(world, x, center.getY() + y, z));
                
                // Place vine/thorn blocks
                if (block.getType() == Material.AIR) {
                    if (y == 0) {
                        // Base - thorns
                        block.setType(Material.SWEET_BERRY_BUSH);
                        blocks.add(block);
                    } else if (y == height - 1) {
                        // Top - leaves
                        block.setType(Material.VINE);
                        blocks.add(block);
                    } else {
                        // Middle - mixed
                        if (random.nextBoolean()) {
                            block.setType(Material.SWEET_BERRY_BUSH);
                        } else {
                            block.setType(Material.VINE);
                        }
                        blocks.add(block);
                    }
                }
            }
        }
        
        // Add floor thorns
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double distance = Math.sqrt(x*x + z*z);
                if (distance <= radius) {
                    Block block = world.getBlockAt(
                        center.getBlockX() + x,
                        center.getBlockY() - 1,
                        center.getBlockZ() + z
                    );
                    
                    if (block.getType() == Material.AIR || block.getType() == Material.GRASS_BLOCK) {
                        block.setType(Material.SWEET_BERRY_BUSH);
                        blocks.add(block);
                    }
                }
            }
        }
        
        // Start cage effects
        startCageEffects(player, target, center);
    }
    
    private void startCageEffects(Player player, Player target, Location cageCenter) {
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 160 || !natureActive.getOrDefault(player.getUniqueId(), false)) {
                    this.cancel();
                    return;
                }
                
                // Damage and poison enemies in cage
                for (Entity entity : player.getWorld().getNearbyEntities(cageCenter, 5, 5, 5)) {
                    if (entity instanceof Player enemy && !enemy.equals(player)) {
                        // Thorn damage
                        if (ticks % 20 == 0) { // Every second
                            enemy.damage(2, player); // 1 heart per second
                            enemy.getWorld().playSound(enemy.getLocation(), Sound.ENTITY_SWEET_BERRY_BUSH_HURT, 1, 0.8f);
                        }
                        
                        // Poison effect
                        if (random.nextInt(10) < 3) {
                            enemy.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0));
                        }
                        
                        // Slowness
                        enemy.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));
                        
                        // Thorn particles
                        enemy.getWorld().spawnParticle(Particle.BLOCK, enemy.getLocation().add(0, 1, 0),
                            10, 0.3, 0.5, 0.3, Material.SWEET_BERRY_BUSH.createBlockData());
                        enemy.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, enemy.getLocation().add(0, 1, 0),
                            5, 0.3, 0.5, 0.3, 0);
                    }
                }
                
                // Particle effects around cage
                for (int i = 0; i < 8; i++) {
                    double angle = (ticks * 5 + i * 45) % 360;
                    double rad = Math.toRadians(angle);
                    
                    for (int y = 0; y < 5; y++) {
                        double x = cageCenter.getX() + 4 * Math.cos(rad);
                        double z = cageCenter.getZ() + 4 * Math.sin(rad);
                        Location loc = new Location(player.getWorld(), x, cageCenter.getY() + y, z);
                        
                        // Vine particles
                        player.getWorld().spawnParticle(Particle.COMPOSTER, loc, 3, 0.2, 0.2, 0.2, 0);
                        
                        if (y % 2 == 0) {
                            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 2, 0.1, 0.1, 0.1, 0);
                        }
                    }
                }
                
                ticks += 5;
            }
        }.runTaskTimer(plugin, 0, 5);
    }
    
    private void startHealingAura(Player player) {
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 160 || !natureActive.getOrDefault(player.getUniqueId(), false)) {
                    this.cancel();
                    return;
                }
                
                // Heal player and nearby allies
                List<Player> currentHealed = new ArrayList<>();
                
                for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), 10, 5, 10)) {
                    if (entity instanceof Player ally && !ally.equals(player)) {
                        currentHealed.add(ally);
                        
                        // Healing every 2 seconds
                        if (ticks % 40 == 0) {
                            double newHealth = Math.min(ally.getHealth() + 2, ally.getMaxHealth());
                            ally.setHealth(newHealth);
                            
                            // Healing particles
                            ally.getWorld().spawnParticle(Particle.HEART, ally.getLocation().add(0, 2, 0),
                                5, 0.3, 0.3, 0.3, 0);
                            ally.getWorld().playSound(ally.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
                            
                            ally.sendMessage(Component.text("§aNature heals you!"));
                        }
                    }
                }
                
                // Always heal the user
                if (ticks % 40 == 0) {
                    double newHealth = Math.min(player.getHealth() + 1, player.getMaxHealth());
                    player.setHealth(newHealth);
                    
                    // User healing particles
                    player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 2, 0),
                        8, 0.5, 0.5, 0.5, 0);
                }
                
                // Healing aura particles
                for (int i = 0; i < 360; i += 30) {
                    double rad = Math.toRadians(i);
                    double x = player.getLocation().getX() + 5 * Math.cos(rad);
                    double z = player.getLocation().getZ() + 5 * Math.sin(rad);
                    double y = player.getLocation().getY() + 1 + Math.sin(ticks * 0.1 + i) * 0.5;
                    
                    Location loc = new Location(player.getWorld(), x, y, z);
                    player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 2, 0.1, 0.1, 0.1, 0);
                    player.getWorld().spawnParticle(Particle.COMPOSTER, loc, 1, 0, 0, 0, 0);
                }
                
                healedPlayers.put(player.getUniqueId(), currentHealed);
                ticks += 5;
            }
        }.runTaskTimer(plugin, 0, 5);
    }
    
    private void startNatureParticles(Player player, Player target) {
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 160 || !natureActive.getOrDefault(player.getUniqueId(), false)) {
                    this.cancel();
                    return;
                }
                
                // Nature particles around player
                for (int i = 0; i < 5; i++) {
                    double offsetX = (random.nextDouble() - 0.5) * 4;
                    double offsetY = random.nextDouble() * 3;
                    double offsetZ = (random.nextDouble() - 0.5) * 4;
                    
                    Location particleLoc = player.getLocation().clone().add(offsetX, offsetY, offsetZ);
                    
                    // Random nature particles
                    int particleType = random.nextInt(5);
                    switch (particleType) {
                        case 0:
                            player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, particleLoc, 1, 0, 0, 0, 0);
                            break;
                        case 1:
                            player.getWorld().spawnParticle(Particle.COMPOSTER, particleLoc, 2, 0.2, 0.2, 0.2, 0);
                            break;
                        case 2:
                            player.getWorld().spawnParticle(Particle.FALLING_SPORE_BLOSSOM, particleLoc, 1, 0, 0, 0, 0);
                            break;
                        case 3:
                            player.getWorld().spawnParticle(Particle.SPORE_BLOSSOM_AIR, particleLoc, 3, 0.3, 0.3, 0.3, 0);
                            break;
                        case 4:
                            player.getWorld().spawnParticle(Particle.WAX_ON, particleLoc, 2, 0.2, 0.2, 0.2, 0);
                            break;
                    }
                }
                
                // Vine whip effect around target
                if (target != null && target.isOnline()) {
                    for (double[] offset : vineOffsets) {
                        Location vineLoc = target.getLocation().clone().add(
                            offset[0] * 2,
                            offset[1],
                            offset[2] * 2
                        );
                        
                        // Vine particle trail
                        player.getWorld().spawnParticle(Particle.WAX_OFF, vineLoc, 3, 0.1, 0.1, 0.1, 0);
                        
                        // Connect to target with particles
                        Vector toTarget = target.getLocation().toVector().subtract(vineLoc.toVector()).normalize();
                        for (double d = 0; d < 2; d += 0.3) {
                            Location connectLoc = vineLoc.clone().add(toTarget.clone().multiply(d));
                            player.getWorld().spawnParticle(Particle.COMPOSTER, connectLoc, 1, 0, 0, 0, 0);
                        }
                    }
                }
                
                ticks += 3;
            }
        }.runTaskTimer(plugin, 0, 3);
    }
    
    @Override
    public void onLeftClick(Player player) {
        if (natureActive.getOrDefault(player.getUniqueId(), false)) {
            shootVineWhip(player);
        }
    }
    
    private void shootVineWhip(Player player) {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize().multiply(1.5);
        
        player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1, 0.8f);
        
        new BukkitRunnable() {
            Location loc = eye.clone();
            int distance = 0;
            Entity grabbedEntity = null;
            
            @Override
            public void run() {
                if (distance > 15) {
                    this.cancel();
                    return;
                }
                
                loc.add(direction);
                distance++;
                
                // Vine particle trail
                player.getWorld().spawnParticle(Particle.WAX_OFF, loc, 8, 0.1, 0.1, 0.1, 0.02);
                player.getWorld().spawnParticle(Particle.COMPOSTER, loc, 5, 0.1, 0.1, 0.1, 0);
                
                // Periodic sound
                if (distance % 5 == 0) {
                    player.getWorld().playSound(loc, Sound.BLOCK_GROWING_PLANT_CROP, 0.3f, 1.2f);
                }
                
                // Hit detection
                for (Entity entity : player.getWorld().getNearbyEntities(loc, 1.5, 1.5, 1.5)) {
                    if (entity instanceof LivingEntity target && !entity.equals(player)) {
                        grabbedEntity = target;
                        
                        // Pull effect
                        Vector pull = player.getLocation().toVector().subtract(target.getLocation().toVector()).normalize().multiply(1.5);
                        target.setVelocity(pull);
                        
                        // Damage
                        target.damage(4, player);
                        
                        // Vine wrap effect
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 3));
                        
                        // Particle explosion
                        target.getWorld().spawnParticle(Particle.BLOCK, target.getLocation().add(0, 1, 0),
                            30, 0.5, 0.5, 0.5, Material.VINE.createBlockData());
                        target.getWorld().spawnParticle(Particle.ENCHANT, target.getLocation().add(0, 1, 0),
                            20, 0.5, 0.5, 0.5, 0.5);
                        
                        player.playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 1, 1);
                        
                        this.cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    @Override
    public void onRightClick(Player player) {
        if (!natureActive.getOrDefault(player.getUniqueId(), false)) {
            onTrigger(player);
        } else {
            // Bonus - right click while active gives temporary buff
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 60, 1));
            player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 2, 0), 10, 0.5, 0.5, 0.5, 0);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1.2f);
        }
    }
    
    private void endNaturesWrath(Player player) {
        UUID playerId = player.getUniqueId();
        natureActive.put(playerId, false);
        
        // Remove all nature blocks
        List<Block> blocks = natureBlocks.get(playerId);
        if (blocks != null) {
            for (Block block : blocks) {
                if (block.getType() == Material.SWEET_BERRY_BUSH || 
                    block.getType() == Material.VINE ||
                    block.getType() == Material.GRASS_BLOCK) {
                    block.setType(Material.AIR);
                }
            }
        }
        
        // Remove all effects from user
        player.removePotionEffect(PotionEffectType.REGENERATION);
        player.removePotionEffect(PotionEffectType.ABSORPTION);
        player.removePotionEffect(PotionEffectType.RESISTANCE);
        player.removePotionEffect(PotionEffectType.SATURATION);
        
        // Grand finale - nature explosion
        World world = player.getWorld();
        
        // Massive particle explosion
        for (int i = 0; i < 100; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double radius = random.nextDouble() * 10;
            double x = player.getLocation().getX() + radius * Math.cos(angle);
            double z = player.getLocation().getZ() + radius * Math.sin(angle);
            double y = player.getLocation().getY() + random.nextDouble() * 5;
            
            Location loc = new Location(world, x, y, z);
            
            // Mix of nature particles
            if (i % 3 == 0) {
                world.spawnParticle(Particle.HAPPY_VILLAGER, loc, 5, 0.2, 0.2, 0.2, 0);
            } else if (i % 3 == 1) {
                world.spawnParticle(Particle.COMPOSTER, loc, 8, 0.3, 0.3, 0.3, 0);
            } else {
                world.spawnParticle(Particle.FALLING_SPORE_BLOSSOM, loc, 3, 0.1, 0.1, 0.1, 0);
            }
        }
        
        // Big sound
        world.playSound(player.getLocation(), Sound.EVENT_RAID_HORN, 2, 1.5f);
        world.playSound(player.getLocation(), Sound.BLOCK_GROWING_PLANT_CROP, 2, 0.8f);
        
        // Final title
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showTitle(Title.title(
                Component.text("§2§lNATURE'S BLESSING"),
                Component.text("§aThe forest returns to slumber"),
                Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(1))
            ));
        }
        
        // Clear maps
        natureBlocks.remove(playerId);
        healedPlayers.remove(playerId);
        
        player.sendMessage(Component.text("§2Nature's Wrath subsides... for now."));
    }
}
