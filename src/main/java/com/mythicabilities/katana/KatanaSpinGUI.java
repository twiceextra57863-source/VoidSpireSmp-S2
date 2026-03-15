package com.mythicabilities.katana;

import com.mythicabilities.MythicAbilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;

import java.time.Duration;
import java.util.*;

public class KatanaSpinGUI {
    
    private final MythicAbilities plugin;
    private final Map<UUID, Boolean> isSpinning = new HashMap<>();
    private final Map<UUID, ArmorStand> spinStands = new HashMap<>();
    private final Map<UUID, Integer> spinFrames = new HashMap<>();
    private final Random random = new Random();
    
    // Animation constants
    private final int TOTAL_FRAMES = 300; // 15 seconds at 20fps
    
    public KatanaSpinGUI(MythicAbilities plugin) {
        this.plugin = plugin;
    }
    
    public void startVisualSpin(Player player, boolean adminSpin) {
        UUID playerId = player.getUniqueId();
        
        if (isSpinning.getOrDefault(playerId, false)) return;
        
        isSpinning.put(playerId, true);
        
        // Freeze player
        player.setWalkSpeed(0);
        player.setAllowFlight(true);
        player.setFlying(true);
        
        // Get all katana names
        List<String> katanaNames = KatanaData.getAllKatanaNames();
        String selectedKatana = katanaNames.get(random.nextInt(katanaNames.size()));
        
        // Create armor stand
        Location spawnLoc = player.getLocation().clone().add(0, 2, 2);
        ArmorStand spinStand = (ArmorStand) player.getWorld().spawnEntity(spawnLoc, EntityType.ARMOR_STAND);
        spinStand.setGravity(false);
        spinStand.setInvulnerable(true);
        spinStand.setVisible(false);
        spinStand.setMarker(true);
        spinStand.setSmall(true);
        spinStand.setCustomNameVisible(true);
        spinStand.setCustomName("§6✦ SPINNING ✦");
        
        spinStands.put(playerId, spinStand);
        spinFrames.put(playerId, 0);
        
        // Play start sound
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1, 1);
        
        // Start animation
        startSpinAnimation(player, katanaNames, selectedKatana, adminSpin);
    }
    
    private void startSpinAnimation(Player player, List<String> katanaNames, String selectedKatana, boolean adminSpin) {
        UUID playerId = player.getUniqueId();
        ArmorStand spinStand = spinStands.get(playerId);
        
        if (spinStand == null) return;
        
        new BukkitRunnable() {
            int frame = 0;
            double rotation = 0;
            
            @Override
            public void run() {
                if (frame >= TOTAL_FRAMES || !isSpinning.getOrDefault(playerId, false)) {
                    showResult(player, selectedKatana, adminSpin);
                    this.cancel();
                    return;
                }
                
                // Calculate phase (1-3)
                int phase;
                if (frame < 100) phase = 1;      // Fast
                else if (frame < 200) phase = 2; // Medium
                else phase = 3;                   // Slow
                
                // Spin speed based on phase
                double spinSpeed = phase == 1 ? 0.8 : (phase == 2 ? 0.4 : 0.15);
                rotation += spinSpeed;
                spinStand.setHeadPose(new EulerAngle(0, rotation, 0));
                
                // Show different katana names
                int abilityIndex = (frame / (phase == 3 ? 10 : 3)) % katanaNames.size();
                String currentKatana = katanaNames.get(abilityIndex);
                spinStand.setCustomName(KatanaData.getDisplayName(currentKatana) + " §f✦");
                
                // Move in circle
                double angle = frame * (phase == 1 ? 0.3 : (phase == 2 ? 0.15 : 0.05));
                double radius = 2.5 + Math.sin(frame * 0.1) * 0.5;
                double x = player.getLocation().getX() + radius * Math.cos(angle);
                double z = player.getLocation().getZ() + radius * Math.sin(angle);
                double y = player.getLocation().getY() + 1.5 + Math.sin(frame * 0.2) * 0.5;
                
                spinStand.teleport(new Location(player.getWorld(), x, y, z));
                
                // Particles
                spawnSpinParticles(player, spinStand.getLocation(), phase);
                
                // Sounds
                if (frame % (phase == 1 ? 4 : (phase == 2 ? 6 : 8)) == 0) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 
                        0.5f, 1 + (frame * 0.01f));
                }
                
                frame++;
                spinFrames.put(playerId, frame);
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    private void spawnSpinParticles(Player player, Location center, int phase) {
        World world = player.getWorld();
        
        switch (phase) {
            case 1:
                // Fast spin - spiral
                for (int i = 0; i < 5; i++) {
                    double angle = Math.random() * 2 * Math.PI;
                    double x = center.getX() + 2 * Math.cos(angle);
                    double z = center.getZ() + 2 * Math.sin(angle);
                    Location loc = new Location(world, x, center.getY(), z);
                    world.spawnParticle(Particle.END_ROD, loc, 2, 0.1, 0.1, 0.1, 0.02);
                }
                break;
                
            case 2:
                // Medium spin - notes
                world.spawnParticle(Particle.NOTE, center, 5, 0.3, 0.3, 0.3, 1);
                break;
                
            case 3:
                // Slow spin - glow
                world.spawnParticle(Particle.GLOW, center, 3, 0.2, 0.2, 0.2, 0);
                break;
        }
        
        world.spawnParticle(Particle.ENCHANT, center, 5, 0.2, 0.2, 0.2, 0);
    }
    
    private void showResult(Player player, String selectedKatana, boolean adminSpin) {
        UUID playerId = player.getUniqueId();
        
        // Remove armor stand
        ArmorStand spinStand = spinStands.remove(playerId);
        if (spinStand != null) spinStand.remove();
        
        // Unfreeze player
        player.setWalkSpeed(0.2f);
        player.setAllowFlight(false);
        player.setFlying(false);
        
        if (adminSpin) {
            // Admin spin - give katana
            plugin.getKatanaManager().giveKatanaToPlayer(player, selectedKatana);
        }
        
        // Celebration
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
        
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, 
            player.getLocation().add(0, 2, 0), 100, 1, 1, 1, 0.5);
        
        player.showTitle(Title.title(
            Component.text("§6§l✦ KATANA UNLOCKED! ✦"),
            Component.text(KatanaData.getDisplayName(selectedKatana)),
            Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(3), Duration.ofSeconds(1))
        ));
        
        if (!adminSpin) {
            player.sendMessage("§cThis was a preview! Admin must give you the katana.");
        }
        
        isSpinning.remove(playerId);
        spinFrames.remove(playerId);
    }
          }
