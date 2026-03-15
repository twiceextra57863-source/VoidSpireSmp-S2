package com.mythicabilities.gui;

import com.mythicabilities.MythicAbilities;
import com.mythicabilities.abilities.Ability;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;

import java.time.Duration;
import java.util.*;

public class AbilitySpinGUI implements Listener, org.bukkit.command.CommandExecutor {
    
    private final MythicAbilities plugin;
    private final Map<UUID, Boolean> isSpinning = new HashMap<>();
    private final Map<UUID, ArmorStand> spinStands = new HashMap<>();
    private final Map<UUID, Integer> spinFrames = new HashMap<>();
    private final Map<UUID, Integer> spinPhase = new HashMap<>();
    private final Random random = new Random();
    
    // Animation constants
    private final int TOTAL_FRAMES = 300; // 15 seconds at 20fps
    private final int PHASE_1_END = 100;  // Fast spin
    private final int PHASE_2_END = 200;  // Medium spin
    private final int PHASE_3_END = 300;  // Slow spin + result
    
    public AbilitySpinGUI(MythicAbilities plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, 
                            String label, String[] args) {
        if (sender instanceof Player player) {
            openSpinGUI(player);
        }
        return true;
    }
    
    public void openSpinGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, Component.text("§8✦ Spin for Ability ✦"));
        
        // Fill border with glass
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.displayName(Component.text(" "));
        border.setItemMeta(borderMeta);
        
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, border);
        }
        
        // Center slot for spin wheel
        ItemStack spinWheel = new ItemStack(Material.NETHER_STAR);
        ItemMeta spinMeta = spinWheel.getItemMeta();
        spinMeta.displayName(Component.text("§6§l✦ SPIN ✦"));
        spinMeta.lore(List.of(
            Component.text("§7Click to spin for a random ability!"),
            Component.text("§eYou'll get 1 of 20 abilities!")
        ));
        spinWheel.setItemMeta(spinMeta);
        gui.setItem(22, spinWheel);
        
        // Show ability previews around
        List<Ability> abilities = new ArrayList<>(plugin.getAbilityManager().getAllAbilities().values());
        int[] slots = {19, 20, 21, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34};
        
        for (int i = 0; i < Math.min(abilities.size(), slots.length); i++) {
            gui.setItem(slots[i], abilities.get(i).getIcon());
        }
        
        player.openInventory(gui);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().title().equals(Component.text("§8✦ Spin for Ability ✦"))) return;
        
        event.setCancelled(true);
        
        if (event.getSlot() == 22 && !isSpinning.getOrDefault(player.getUniqueId(), false)) {
            player.closeInventory();
            startVisualSpin(player);
        }
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // Freeze player during spin
        if (isSpinning.getOrDefault(event.getPlayer().getUniqueId(), false)) {
            if (event.getFrom().getX() != event.getTo().getX() || 
                event.getFrom().getZ() != event.getTo().getZ()) {
                event.setCancelled(true);
            }
        }
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Prevent right-click during spin
        if (isSpinning.getOrDefault(event.getPlayer().getUniqueId(), false)) {
            event.setCancelled(true);
        }
    }
    
    public void startVisualSpin(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Don't spin if already spinning
        if (isSpinning.getOrDefault(playerId, false)) return;
        
        isSpinning.put(playerId, true);
        spinPhase.put(playerId, 1); // Phase 1: Fast spin
        
        // Freeze player
        player.setWalkSpeed(0);
        player.setAllowFlight(true);
        player.setFlying(true);
        
        // Get all ability names
        List<String> abilityNames = new ArrayList<>(plugin.getAbilityManager().getAllAbilities().keySet());
        if (abilityNames.isEmpty()) {
            player.sendMessage(Component.text("§cNo abilities available!"));
            endSpin(player, null);
            return;
        }
        
        String selectedAbility = abilityNames.get(random.nextInt(abilityNames.size()));
        
        // Create armor stand for spinning
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
        
        // Start spin animation
        startSpinAnimation(player, abilityNames, selectedAbility);
    }
    
    private void startSpinAnimation(Player player, List<String> abilityNames, String selectedAbility) {
        UUID playerId = player.getUniqueId();
        ArmorStand spinStand = spinStands.get(playerId);
        
        if (spinStand == null) return;
        
        new BukkitRunnable() {
            int frame = 0;
            double rotation = 0;
            int lastAbilityIndex = -1;
            float basePitch = 0.5f;
            
            @Override
            public void run() {
                if (!isSpinning.getOrDefault(playerId, false) || frame >= TOTAL_FRAMES) {
                    // Animation complete - show result
                    showResult(player, selectedAbility);
                    this.cancel();
                    return;
                }
                
                // Determine current phase
                int phase;
                if (frame < PHASE_1_END) phase = 1;      // Fast spin
                else if (frame < PHASE_2_END) phase = 2; // Medium spin
                else phase = 3;                           // Slow spin
                
                spinPhase.put(playerId, phase);
                
                // Calculate spin speed based on phase
                double spinSpeed;
                float soundPitch;
                int particleDensity;
                
                switch (phase) {
                    case 1: // Fast spin
                        spinSpeed = 0.8;
                        soundPitch = basePitch + (frame * 0.01f);
                        particleDensity = 10;
                        break;
                    case 2: // Medium spin
                        spinSpeed = 0.4;
                        soundPitch = basePitch + 0.3f + (frame * 0.005f);
                        particleDensity = 7;
                        break;
                    default: // Slow spin
                        spinSpeed = 0.15;
                        soundPitch = basePitch + 0.6f + (frame * 0.002f);
                        particleDensity = 5;
                        break;
                }
                
                // Calculate which ability to show this frame
                int abilityIndex;
                if (phase == 3) {
                    // Slow phase - show abilities slower
                    abilityIndex = (frame / 10) % abilityNames.size();
                } else {
                    abilityIndex = (frame / 3) % abilityNames.size();
                }
                
                String currentAbility = abilityNames.get(abilityIndex);
                Ability ability = plugin.getAbilityManager().getAbility(currentAbility);
                
                // Rotate armor stand
                rotation += spinSpeed;
                spinStand.setHeadPose(new EulerAngle(0, rotation, 0));
                
                // Move armor stand in circle around player with varying height
                double angle = frame * (phase == 1 ? 0.3 : (phase == 2 ? 0.15 : 0.05));
                double radius = 2.5 + Math.sin(frame * 0.1) * 0.5;
                double x = player.getLocation().getX() + radius * Math.cos(angle);
                double z = player.getLocation().getZ() + radius * Math.sin(angle);
                double y = player.getLocation().getY() + 1.5 + Math.sin(frame * 0.2) * 0.5;
                
                Location newLoc = new Location(player.getWorld(), x, y, z);
                spinStand.teleport(newLoc);
                
                // Update display name with current ability
                if (ability != null) {
                    spinStand.setCustomName(ability.getDisplayName() + " §f✦");
                }
                
                // Phase-based particle effects
                spawnPhaseParticles(player, newLoc, phase, frame);
                
                // Musical sounds based on phase
                if (frame % (phase == 1 ? 4 : (phase == 2 ? 6 : 8)) == 0) {
                    playMusicalSound(player, phase, frame);
                }
                
                // Send action bar with spinning animation
                sendSpinActionBar(player, frame, phase);
                
                frame++;
                spinFrames.put(playerId, frame);
            }
        }.runTaskTimer(plugin, 0, 1); // 20fps animation
    }
    
    private void spawnPhaseParticles(Player player, Location center, int phase, int frame) {
        World world = player.getWorld();
        
        switch (phase) {
            case 1: // Fast spin - spiral particles
                for (int i = 0; i < 5; i++) {
                    double angle = (frame * 0.5 + i * 72) * Math.PI / 180;
                    double x = center.getX() + 1.5 * Math.cos(angle);
                    double z = center.getZ() + 1.5 * Math.sin(angle);
                    double y = center.getY() + Math.sin(frame * 0.2 + i) * 0.3;
                    
                    Location particleLoc = new Location(world, x, y, z);
                    world.spawnParticle(Particle.END_ROD, particleLoc, 2, 0.1, 0.1, 0.1, 0.02);
                    world.spawnParticle(Particle.PORTAL, particleLoc, 3, 0.1, 0.1, 0.1, 0.1);
                }
                
                // Outer ring
                for (int i = 0; i < 360; i += 30) {
                    double rad = Math.toRadians(i + frame * 5);
                    double x = player.getLocation().getX() + 3.5 * Math.cos(rad);
                    double z = player.getLocation().getZ() + 3.5 * Math.sin(rad);
                    Location ringLoc = new Location(world, x, player.getLocation().getY() + 1, z);
                    world.spawnParticle(Particle.FIREWORK, ringLoc, 1, 0, 0, 0, 0);
                }
                break;
                
            case 2: // Medium spin - note particles
                for (int i = 0; i < 3; i++) {
                    double angle = (frame * 0.3 + i * 120) * Math.PI / 180;
                    double x = center.getX() + 2 * Math.cos(angle);
                    double z = center.getZ() + 2 * Math.sin(angle);
                    
                    Location noteLoc = new Location(world, x, center.getY(), z);
                    world.spawnParticle(Particle.NOTE, noteLoc, 3, 0.2, 0.2, 0.2, 1);
                    world.spawnParticle(Particle.ENCHANT, noteLoc, 2, 0.1, 0.1, 0.1, 0);
                }
                
                // Floating particles
                for (int i = 0; i < 5; i++) {
                    double offsetX = (random.nextDouble() - 0.5) * 4;
                    double offsetZ = (random.nextDouble() - 0.5) * 4;
                    Location floatLoc = player.getLocation().clone().add(offsetX, random.nextDouble() * 2, offsetZ);
                    world.spawnParticle(Particle.HAPPY_VILLAGER, floatLoc, 1, 0, 0, 0, 0);
                }
                break;
                
            case 3: // Slow spin - dramatic effects
                // Beam of light
                for (double y = 0; y < 3; y += 0.3) {
                    Location beamLoc = center.clone().add(0, y, 0);
                    world.spawnParticle(Particle.END_ROD, beamLoc, 3, 0.1, 0.1, 0.1, 0.01);
                }
                
                // Sparkles around
                for (int i = 0; i < 8; i++) {
                    double angle = (i * 45) * Math.PI / 180;
                    double x = center.getX() + 2 * Math.cos(angle);
                    double z = center.getZ() + 2 * Math.sin(angle);
                    Location sparkleLoc = new Location(world, x, center.getY(), z);
                    world.spawnParticle(Particle.FIREWORK, sparkleLoc, 2, 0.1, 0.1, 0.1, 0.02);
                }
                
                // Glowing effect
                world.spawnParticle(Particle.GLOW, center, 5, 0.3, 0.3, 0.3, 0);
                break;
        }
        
        // Always spawn some basic particles - FIXED: Replaced SPELL with ENCHANT
        world.spawnParticle(Particle.ENCHANT, center, 5, 0.3, 0.3, 0.3, 0);
    }
    
    private void playMusicalSound(Player player, int phase, int frame) {
        switch (phase) {
            case 1: // Fast spin - quick notes
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 0.5f, 1.0f + (frame * 0.005f));
                if (frame % 8 == 0) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.3f, 1.2f);
                }
                break;
                
            case 2: // Medium spin - melody
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.4f, 1.1f + (frame * 0.003f));
                if (frame % 12 == 0) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_FLUTE, 0.3f, 1.3f);
                }
                break;
                
            case 3: // Slow spin - dramatic
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, 1.5f);
                if (frame % 10 == 0) {
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.3f, 1.8f);
                }
                break;
        }
    }
    
    private void sendSpinActionBar(Player player, int frame, int phase) {
        StringBuilder spinText = new StringBuilder();
        
        // Add phase indicator
        switch (phase) {
            case 1:
                spinText.append("§c⚡ §6⚡ §e⚡ ");
                break;
            case 2:
                spinText.append("§e♪ §6♫ §a♪ ");
                break;
            case 3:
                spinText.append("§d✦ §5✦ §d✦ ");
                break;
        }
        
        // Add spinning dots
        spinText.append("§f[");
        int totalDots = 15;
        int position = frame % totalDots;
        
        for (int i = 0; i < totalDots; i++) {
            if (i == position) {
                spinText.append("§6●");
            } else if (Math.abs(i - position) < 3) {
                spinText.append("§e●");
            } else if (Math.abs(i - position) < 6) {
                spinText.append("§7●");
            } else {
                spinText.append("§8●");
            }
        }
        spinText.append("§f]");
        
        // Add timer
        int secondsLeft = (TOTAL_FRAMES - frame) / 20;
        spinText.append(" §e").append(String.format("%02d", secondsLeft)).append("s");
        
        player.sendActionBar(Component.text(spinText.toString()));
    }
    
    private void showResult(Player player, String selectedAbility) {
        UUID playerId = player.getUniqueId();
        
        // Remove armor stand
        ArmorStand spinStand = spinStands.remove(playerId);
        if (spinStand != null) {
            spinStand.remove();
        }
        
        // Unfreeze player
        player.setWalkSpeed(0.2f);
        player.setAllowFlight(false);
        player.setFlying(false);
        
        // Get ability
        Ability ability = plugin.getAbilityManager().getAbility(selectedAbility);
        if (ability == null) {
            player.sendMessage(Component.text("§cError: Ability not found!"));
            isSpinning.put(playerId, false);
            return;
        }
        
        // Save ability to player
        plugin.getAbilityManager().setPlayerAbility(player, selectedAbility);
        
        // GRAND FINALE - Massive celebration
        World world = player.getWorld();
        Location loc = player.getLocation();
        
        // Play victory fanfare
        player.playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
        player.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1.2f);
        player.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1, 1.5f);
        player.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
        
        // Spiral explosion
        new BukkitRunnable() {
            int explosionFrame = 0;
            
            @Override
            public void run() {
                if (explosionFrame >= 20) {
                    this.cancel();
                    return;
                }
                
                double radius = explosionFrame * 0.5;
                for (int i = 0; i < 360; i += 30) {
                    double rad = Math.toRadians(i + explosionFrame * 10);
                    double x = loc.getX() + radius * Math.cos(rad);
                    double z = loc.getZ() + radius * Math.sin(rad);
                    double y = loc.getY() + 1 + Math.sin(explosionFrame * 0.5) * 0.5;
                    
                    Location particleLoc = new Location(world, x, y, z);
                    world.spawnParticle(Particle.FIREWORK, particleLoc, 3, 0.1, 0.1, 0.1, 0.02);
                    world.spawnParticle(Particle.END_ROD, particleLoc, 2, 0.1, 0.1, 0.1, 0.01);
                }
                
                explosionFrame++;
            }
        }.runTaskTimer(plugin, 0, 1);
        
        // Massive particle burst
        world.spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(0, 2, 0), 200, 2, 2, 2, 0.5);
        world.spawnParticle(Particle.FLASH, loc.clone().add(0, 2, 0), 3);
        world.spawnParticle(Particle.EXPLOSION_EMITTER, loc.clone().add(0, 2, 0), 2);
        
        // Show title with ability
        player.showTitle(Title.title(
            Component.text("§6§l✦ ABILITY UNLOCKED! ✦"),
            Component.text(ability.getDisplayName()),
            Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(3), Duration.ofSeconds(1))
        ));
        
        // Send chat message with box
        player.sendMessage("§6§l╔════════════════════════════════════╗");
        player.sendMessage("§6§l║     §e✦ CONGRATULATIONS! ✦        §6§l║");
        player.sendMessage("§6§l║                                    §6§l║");
        player.sendMessage("§6§l║   §fYou received: " + ability.getDisplayName() + "   §6§l║");
        player.sendMessage("§6§l║                                    §6§l║");
        player.sendMessage("§6§l║    §7Right-click to activate!      §6§l║");
        player.sendMessage("§6§l╚════════════════════════════════════╝");
        
        // Clear spin state
        isSpinning.put(playerId, false);
        spinPhase.remove(playerId);
        spinFrames.remove(playerId);
    }
    
    private void endSpin(Player player, String error) {
        UUID playerId = player.getUniqueId();
        
        // Remove armor stand
        ArmorStand spinStand = spinStands.remove(playerId);
        if (spinStand != null) {
            spinStand.remove();
        }
        
        // Unfreeze player
        player.setWalkSpeed(0.2f);
        player.setAllowFlight(false);
        player.setFlying(false);
        
        if (error != null) {
            player.sendMessage(error);
        }
        
        isSpinning.put(playerId, false);
        spinPhase.remove(playerId);
        spinFrames.remove(playerId);
    }
}
