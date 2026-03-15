package com.mythicabilities.katana;

import com.mythicabilities.MythicAbilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack; // CRITICAL IMPORT
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.*;

public class KatanaSpinGUI {
    
    private final MythicAbilities plugin;
    private final Map<UUID, Boolean> isSpinning = new HashMap<>();
    private final Map<UUID, List<ArmorStand>> spinStands = new HashMap<>();
    private final Map<UUID, Integer> spinFrames = new HashMap<>();
    private final Map<UUID, Double> spinAngle = new HashMap<>();
    private final Random random = new Random();
    
    // Animation constants
    private final int TOTAL_FRAMES = 300; // 15 seconds at 20fps
    private final int SWORD_COUNT = 5; // 5 swords spinning horizontally
    
    public KatanaSpinGUI(MythicAbilities plugin) {
        this.plugin = plugin;
    }
    
    /**
     * ULTIMATE HORIZONTAL SWORD SPIN ANIMATION
     * Swords spin across the screen horizontally with particle trails
     */
    public void startVisualSpin(Player player, boolean adminSpin) {
        UUID playerId = player.getUniqueId();
        
        if (isSpinning.getOrDefault(playerId, false)) return;
        
        isSpinning.put(playerId, true);
        spinFrames.put(playerId, 0);
        spinAngle.put(playerId, 0.0);
        
        // Freeze player
        player.setWalkSpeed(0);
        player.setAllowFlight(true);
        player.setFlying(true);
        
        // Get all katana names
        List<String> katanaNames = Arrays.asList(
            "Storm Breaker", "Flame Dragon", "Shadow Dancer", "Wind Cutter",
            "Earth Shaker", "Frost Bite", "Void Walker", "Sun Slash",
            "Nature's Fang", "Stone Edge", "Wave Splitter", "Blood Moon",
            "Soul Reaper", "Thunder God", "Celestial Blade", "Dragon's Wrath"
        );
        
        String selectedKatana = katanaNames.get(random.nextInt(katanaNames.size()));
        
        // Create multiple swords for horizontal spin
        createHorizontalSwords(player, katanaNames);
        
        // Play start sound
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1, 1);
        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.5f, 1.2f);
        
        // Start animation
        startHorizontalSpinAnimation(player, katanaNames, selectedKatana, adminSpin);
    }
    
    /**
     * Create multiple swords positioned horizontally around player
     */
    private void createHorizontalSwords(Player player, List<String> katanaNames) {
        UUID playerId = player.getUniqueId();
        List<ArmorStand> swords = new ArrayList<>();
        World world = player.getWorld();
        Location center = player.getLocation().clone().add(0, 1.5, 0);
        
        for (int i = 0; i < SWORD_COUNT; i++) {
            // Calculate position in horizontal circle
            double angle = (i * 360.0 / SWORD_COUNT);
            double rad = Math.toRadians(angle);
            double radius = 3.0;
            
            double x = center.getX() + radius * Math.cos(rad);
            double z = center.getZ() + radius * Math.sin(rad);
            double y = center.getY();
            
            Location swordLoc = new Location(world, x, y, z);
            
            // Create armor stand for sword
            ArmorStand stand = (ArmorStand) world.spawnEntity(swordLoc, EntityType.ARMOR_STAND);
            stand.setGravity(false);
            stand.setInvulnerable(true);
            stand.setVisible(false);
            stand.setMarker(true);
            stand.setSmall(true);
            
            // Create sword item - THIS USES ItemStack
            String randomKatana = katanaNames.get(random.nextInt(katanaNames.size()));
            ItemStack swordItem = plugin.getKatanaAdminManager().createKatana(randomKatana, null);
            stand.getEquipment().setItemInMainHand(swordItem);
            
            // Set initial pose - horizontal
            stand.setRightArmPose(new EulerAngle(Math.toRadians(90), 0, 0));
            
            swords.add(stand);
        }
        
        spinStands.put(playerId, swords);
    }
    
    /**
     * HORIZONTAL SPIN ANIMATION - Swords spin around player in a circle
     * with particle trails and dynamic speed changes
     */
    private void startHorizontalSpinAnimation(Player player, List<String> katanaNames, 
                                              String selectedKatana, boolean adminSpin) {
        UUID playerId = player.getUniqueId();
        List<ArmorStand> swords = spinStands.get(playerId);
        
        if (swords == null || swords.isEmpty()) return;
        
        World world = player.getWorld();
        Location center = player.getLocation().clone().add(0, 1.5, 0);
        
        new BukkitRunnable() {
            int frame = 0;
            double globalAngle = 0;
            
            @Override
            public void run() {
                if (frame >= TOTAL_FRAMES || !isSpinning.getOrDefault(playerId, false)) {
                    // Animation complete
                    for (ArmorStand s : swords) s.remove();
                    spinStands.remove(playerId);
                    showResult(player, selectedKatana, adminSpin);
                    this.cancel();
                    return;
                }
                
                // Calculate phase (1-3) for speed variation
                int phase;
                if (frame < 100) phase = 1;      // Fast spin
                else if (frame < 200) phase = 2; // Medium spin
                else phase = 3;                   // Slow spin with buildup
                
                // Spin speed based on phase
                double spinSpeed;
                float soundPitch;
                
                switch (phase) {
                    case 1: // Fast spin
                        spinSpeed = 0.8;
                        soundPitch = 1.0f + (frame * 0.005f);
                        break;
                    case 2: // Medium spin
                        spinSpeed = 0.4;
                        soundPitch = 1.3f + (frame * 0.003f);
                        break;
                    default: // Slow spin (dramatic)
                        spinSpeed = 0.15;
                        soundPitch = 1.6f + (frame * 0.002f);
                        break;
                }
                
                globalAngle += spinSpeed;
                
                // Update each sword position and rotation
                for (int i = 0; i < swords.size(); i++) {
                    ArmorStand stand = swords.get(i);
                    
                    // Calculate position in horizontal circle
                    double angle = globalAngle + (i * 360.0 / swords.size());
                    double rad = Math.toRadians(angle);
                    
                    // Radius changes slightly for dynamic effect
                    double radius = 3.0 + Math.sin(frame * 0.1) * 0.3;
                    
                    double x = center.getX() + radius * Math.cos(rad);
                    double z = center.getZ() + radius * Math.sin(rad);
                    
                    // Height bobbing
                    double y = center.getY() + Math.sin(frame * 0.2 + i) * 0.3;
                    
                    Location newLoc = new Location(world, x, y, z);
                    newLoc.setDirection(center.toVector().subtract(newLoc.toVector()));
                    stand.teleport(newLoc);
                    
                    // Rotate sword to always face center
                    stand.setRightArmPose(new EulerAngle(
                        Math.toRadians(90 + Math.sin(frame * 0.1 + i) * 10),
                        Math.toRadians(angle),
                        0
                    ));
                    
                    // Change sword appearance during slow phase - THIS USES ItemStack
                    if (phase == 3 && frame % 10 == 0) {
                        String randomKatana = katanaNames.get(random.nextInt(katanaNames.size()));
                        ItemStack swordItem = plugin.getKatanaAdminManager().createKatana(randomKatana, null);
                        stand.getEquipment().setItemInMainHand(swordItem);
                    }
                }
                
                // PARTICLE EFFECTS - TRAIL BEHIND SWORDS
                for (int i = 0; i < swords.size(); i++) {
                    ArmorStand stand = swords.get(i);
                    Location swordLoc = stand.getLocation();
                    
                    // Trail behind each sword
                    Vector direction = swordLoc.toVector().subtract(center.toVector()).normalize();
                    Location trailLoc = swordLoc.clone().subtract(direction.multiply(0.5));
                    
                    // Phase-based particles
                    if (phase == 1) {
                        // Fast spin - flame trail
                        world.spawnParticle(Particle.FLAME, trailLoc, 3, 0.1, 0.1, 0.1, 0);
                        world.spawnParticle(Particle.END_ROD, trailLoc, 2, 0.1, 0.1, 0.1, 0);
                    } else if (phase == 2) {
                        // Medium spin - sparkle trail
                        world.spawnParticle(Particle.FIREWORK, trailLoc, 3, 0.1, 0.1, 0.1, 0);
                        world.spawnParticle(Particle.ENCHANT, trailLoc, 2, 0.1, 0.1, 0.1, 0);
                    } else {
                        // Slow spin - dramatic glow
                        world.spawnParticle(Particle.GLOW, trailLoc, 2, 0.1, 0.1, 0.1, 0);
                        world.spawnParticle(Particle.TOTEM_OF_UNDYING, trailLoc, 1, 0.1, 0.1, 0.1, 0.5);
                    }
                }
                
                // CENTER EFFECT - Spiral particles at center
                for (int i = 0; i < 360; i += 30) {
                    double rad = Math.toRadians(i + frame * 5);
                    double x = center.getX() + 1.5 * Math.cos(rad);
                    double z = center.getZ() + 1.5 * Math.sin(rad);
                    double y = center.getY() + Math.sin(frame * 0.2) * 0.5;
                    
                    Location spiralLoc = new Location(world, x, y, z);
                    
                    if (phase == 3) {
                        world.spawnParticle(Particle.FLASH, spiralLoc, 1, 0, 0, 0, 0);
                    }
                    world.spawnParticle(Particle.END_ROD, spiralLoc, 2, 0.1, 0.1, 0.1, 0);
                }
                
                // SOUND EFFECTS
                if (frame % (phase == 1 ? 4 : (phase == 2 ? 6 : 8)) == 0) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3f, soundPitch);
                    
                    if (phase == 3 && frame % 16 == 0) {
                        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.8f);
                    }
                }
                
                // ACTION BAR with visual timer
                sendSpinActionBar(player, frame, phase);
                
                frame++;
                spinFrames.put(playerId, frame);
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    /**
     * Send action bar with spinning animation and timer
     */
    private void sendSpinActionBar(Player player, int frame, int phase) {
        StringBuilder bar = new StringBuilder();
        
        // Phase indicator
        if (phase == 1) bar.append("§c⚡ §6⚡ §e⚡ ");
        else if (phase == 2) bar.append("§e✨ §6✨ §a✨ ");
        else bar.append("§d✦ §5✦ §d✦ ");
        
        // Horizontal sword spin animation
        bar.append("§f[");
        int totalSpots = 20;
        int swordPos = (frame / 2) % totalSpots;
        
        for (int i = 0; i < totalSpots; i++) {
            if (i == swordPos) {
                bar.append("§6🗡️");
            } else if (Math.abs(i - swordPos) < 3) {
                bar.append("§e-");
            } else if (Math.abs(i - swordPos) < 6) {
                bar.append("§7-");
            } else {
                bar.append("§8-");
            }
        }
        bar.append("§f]");
        
        // Timer
        int secondsLeft = (TOTAL_FRAMES - frame) / 20;
        bar.append(" §e").append(String.format("%02d", secondsLeft)).append("s");
        
        player.sendActionBar(Component.text(bar.toString()));
    }
    
    /**
     * Show result with celebration effects
     */
    private void showResult(Player player, String selectedKatana, boolean adminSpin) {
        UUID playerId = player.getUniqueId();
        
        // Unfreeze player
        player.setWalkSpeed(0.2f);
        player.setAllowFlight(false);
        player.setFlying(false);
        
        if (adminSpin) {
            // Give katana
            plugin.getKatanaAdminManager().giveKatanaToPlayer(player, selectedKatana);
        }
        
        // CELEBRATION EFFECTS
        World world = player.getWorld();
        Location loc = player.getLocation().clone().add(0, 2, 0);
        
        // Victory fanfare
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
        player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1.2f);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1.5f);
        
        // Massive particle explosion
        for (int i = 0; i < 360; i += 15) {
            double rad = Math.toRadians(i);
            double x = loc.getX() + 3 * Math.cos(rad);
            double z = loc.getZ() + 3 * Math.sin(rad);
            
            for (double y = 0; y < 4; y += 0.5) {
                Location particleLoc = new Location(world, x, loc.getY() + y, z);
                world.spawnParticle(Particle.FIREWORK, particleLoc, 2, 0.1, 0.1, 0.1, 0);
                world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
            }
        }
        
        world.spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 200, 2, 2, 2, 0.5);
        world.spawnParticle(Particle.FLASH, loc, 3);
        
        // Title
        player.showTitle(Title.title(
            Component.text("§6§l✦ KATANA UNLOCKED! ✦"),
            Component.text(plugin.getKatanaAdminManager().getDisplayName(selectedKatana)),
            Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(3), Duration.ofSeconds(1))
        ));
        
        // Clear spin state
        isSpinning.remove(playerId);
        spinFrames.remove(playerId);
        spinAngle.remove(playerId);
    }
    
    /**
     * Check if player is spinning
     */
    public boolean isSpinning(Player player) {
        return isSpinning.getOrDefault(player.getUniqueId(), false);
    }
                                    }
