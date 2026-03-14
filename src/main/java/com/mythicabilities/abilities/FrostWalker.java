package com.mythicabilities.abilities;

import com.mythicabilities.MythicAbilities;
import net.kyori.adventure.text.Component;
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

import java.util.ArrayList;
import java.util.List;

public class FrostWalker extends Ability {
    
    private final MythicAbilities plugin;
    private boolean abilityActive = false;
    private List<Block> iceBlocks = new ArrayList<>();
    
    public FrostWalker(MythicAbilities plugin) {
        super("frost_walker", "§b§lFrost Walker", 20, createIcon());
        this.plugin = plugin;
    }
    
    private static ItemStack createIcon() {
        ItemStack icon = new ItemStack(Material.ICE);
        ItemMeta meta = icon.getItemMeta();
        meta.displayName(Component.text("§b§lFrost Walker"));
        meta.lore(List.of(
            Component.text("§7Auto-triggers after 3 combos!"),
            Component.text("§7Creates an ice avalanche"),
            Component.text("§7Freezes and knocks back enemies"),
            Component.text("§bCooldown: 20 seconds")
        ));
        icon.setItemMeta(meta);
        return icon;
    }
    
    @Override
    public void onTrigger(Player player) {
        createIceAvalanche(player);
    }
    
    @Override
    public void onRightClick(Player player) {
        // Manual trigger bhi allow karte hain
        if (!abilityActive) {
            createIceAvalanche(player);
        }
    }
    
    @Override
    public void onCombo(Player player, Player target) {
        // Auto trigger on 3 combos
        if (!abilityActive) {
            onTrigger(player);
        }
    }
    
    private void createIceAvalanche(Player player) {
        if (abilityActive) return;
        
        abilityActive = true;
        Location startLoc = player.getEyeLocation();
        Vector direction = startLoc.getDirection().normalize();
        
        player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1, 1);
        player.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1, 0.5f);
        
        // Clear old blocks
        iceBlocks.clear();
        
        new BukkitRunnable() {
            int distance = 0;
            Location currentLoc = startLoc.clone();
            
            @Override
            public void run() {
                if (distance >= 15) { // 15 blocks distance
                    abilityActive = false;
                    // Remove ice blocks after delay
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            for (Block block : iceBlocks) {
                                if (block.getType() == Material.ICE || 
                                    block.getType() == Material.PACKED_ICE ||
                                    block.getType() == Material.BLUE_ICE) {
                                    block.setType(Material.AIR);
                                }
                            }
                            iceBlocks.clear();
                        }
                    }.runTaskLater(plugin, 40); // 2 seconds later
                    
                    this.cancel();
                    return;
                }
                
                // Move forward
                currentLoc.add(direction.clone().multiply(1.2));
                distance++;
                
                // Get blocks in area
                World world = player.getWorld();
                
                // Create ice block falling effect
                for (int x = -2; x <= 2; x++) {
                    for (int y = -1; y <= 2; y++) {
                        for (int z = -2; z <= 2; z++) {
                            // Random chance to spawn ice
                            if (Math.random() < 0.3) {
                                Block block = world.getBlockAt(
                                    currentLoc.getBlockX() + x,
                                    currentLoc.getBlockY() + y,
                                    currentLoc.getBlockZ() + z
                                );
                                
                                // Only replace air
                                if (block.getType() == Material.AIR) {
                                    // Random ice type
                                    Material iceType;
                                    double rand = Math.random();
                                    if (rand < 0.6) {
                                        iceType = Material.ICE;
                                    } else if (rand < 0.9) {
                                        iceType = Material.PACKED_ICE;
                                    } else {
                                        iceType = Material.BLUE_ICE;
                                    }
                                    
                                    block.setType(iceType);
                                    iceBlocks.add(block);
                                }
                            }
                        }
                    }
                }
                
                // Particle effects
                player.getWorld().spawnParticle(Particle.BLOCK, currentLoc, 30, 1, 1, 1, 
                    Material.ICE.createBlockData());
                player.getWorld().spawnParticle(Particle.SNOWFLAKE, currentLoc, 40, 1, 1, 1, 0.1);
                player.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, currentLoc, 20, 1, 1, 1, 0.1);
                
                // Play sound
                player.getWorld().playSound(currentLoc, Sound.BLOCK_GLASS_BREAK, 0.5f, 1.2f);
                
                // Hit detection
                for (Entity entity : player.getWorld().getNearbyEntities(currentLoc, 3, 3, 3)) {
                    if (entity instanceof LivingEntity target && !entity.equals(player)) {
                        // Freeze effect
                        target.setFreezeTicks(200); // 10 seconds freeze
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 3));
                        
                        // Knockback
                        Vector knockback = direction.clone().multiply(2.5).setY(0.8);
                        target.setVelocity(knockback);
                        
                        // Damage (4 = 2 hearts)
                        target.damage(4, player);
                        
                        // Ice particle explosion
                        target.getWorld().spawnParticle(Particle.BLOCK, target.getLocation().add(0, 1, 0),
                            50, 0.5, 0.5, 0.5, Material.ICE.createBlockData());
                        target.getWorld().spawnParticle(Particle.SNOWFLAKE, target.getLocation().add(0, 1, 0),
                            30, 0.5, 0.5, 0.5, 0.2);
                        
                        player.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 1, 1);
                        
                        // Don't cancel - ice continues
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 2);
    }
}
