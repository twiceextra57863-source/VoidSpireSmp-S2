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
    private final Random random = new Random();
    
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
    
    // CHANGED: Made this method PUBLIC so it can be called from PlayerJoinListener
    public void startVisualSpin(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Don't spin if already spinning
        if (isSpinning.getOrDefault(playerId, false)) return;
        
        isSpinning.put(playerId, true);
        
        // Freeze player
        player.setWalkSpeed(0);
        player.setAllowFlight(true);
        player.setFlying(true);
        
        // Get all ability names
        List<String> abilityNames = new ArrayList<>(plugin.getAbilityManager().getAllAbilities().keySet());
        if (abilityNames.isEmpty()) {
            player.sendMessage(Component.text("§cNo abilities available!"));
            isSpinning.put(playerId, false);
            player.setWalkSpeed(0.2f);
            player.setAllowFlight(false);
            player.setFlying(false);
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
        
        // Play start sound
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1, 1);
        
        // Start spin animation
        startSpinAnimation(player, abilityNames, selectedAbility);
    }
    
    private void startSpinAnimation(Player player, List<String> abilityNames, String selectedAbility) {
        UUID playerId = player.getUniqueId();
        ArmorStand spinStand = spinStands.get(playerId);
        
        if (spinStand == null) return;
        
        new BukkitRunnable() {
            int frame = 0;
            final int totalFrames = 60; // 3 seconds at 20fps
            double rotation = 0;
            
            @Override
            public void run() {
                if (!isSpinning.getOrDefault(playerId, false) || frame >= totalFrames) {
                    // Animation complete - show result
                    showResult(player, selectedAbility);
                    this.cancel();
                    return;
                }
                
                // Calculate which ability to show this frame
                int abilityIndex = (frame / 3) % abilityNames.size(); // Change every 3 frames
                String currentAbility = abilityNames.get(abilityIndex);
                Ability ability = plugin.getAbilityManager().getAbility(currentAbility);
                
                // Rotate armor stand
                rotation += 0.3;
                spinStand.setHeadPose(new EulerAngle(0, rotation, 0));
                
                // Move armor stand in circle around player
                double angle = frame * 0.2;
                double x = player.getLocation().getX() + 2 * Math.cos(angle);
                double z = player.getLocation().getZ() + 2 * Math.sin(angle);
                double y = player.getLocation().getY() + 1.5 + Math.sin(frame * 0.2) * 0.3;
                
                Location newLoc = new Location(player.getWorld(), x, y, z);
                spinStand.teleport(newLoc);
                
                // Update display name with current ability
                if (ability != null) {
                    spinStand.setCustomName(ability.getDisplayName() + " §f✦");
                }
                
                // Particles around armor stand
                player.getWorld().spawnParticle(Particle.END_ROD, newLoc.clone().add(0, 0.5, 0), 5, 0.2, 0.2, 0.2, 0.02);
                player.getWorld().spawnParticle(Particle.PORTAL, newLoc.clone().add(0, 0.5, 0), 3, 0.1, 0.1, 0.1, 0.1);
                
                // Particle trail
                for (int i = 0; i < 3; i++) {
                    double trailAngle = angle + i * 2;
                    double trailX = player.getLocation().getX() + 1.5 * Math.cos(trailAngle);
                    double trailZ = player.getLocation().getZ() + 1.5 * Math.sin(trailAngle);
                    Location trailLoc = new Location(player.getWorld(), trailX, y - 0.5, trailZ);
                    player.getWorld().spawnParticle(Particle.FIREWORK, trailLoc, 2, 0.1, 0.1, 0.1, 0.02);
                }
                
                // Sound effects
                if (frame % 5 == 0) {
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1 + (frame * 0.01f));
                }
                
                // Send action bar with spinning text
                StringBuilder spinText = new StringBuilder("§f✦ ");
                for (int i = 0; i < 10; i++) {
                    if (i == frame % 10) {
                        spinText.append("§6⬤ ");
                    } else {
                        spinText.append("§7⬤ ");
                    }
                }
                spinText.append("§f ✦");
                player.sendActionBar(Component.text(spinText.toString()));
                
                frame++;
                spinFrames.put(playerId, frame);
            }
        }.runTaskTimer(plugin, 0, 1); // 20fps animation
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
        
        // Celebration effects
        player.getWorld().playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1.2f);
        
        // Massive particle explosion
        for (int i = 0; i < 10; i++) {
            double angle = i * 36 * Math.PI / 180;
            double x = player.getLocation().getX() + 3 * Math.cos(angle);
            double z = player.getLocation().getZ() + 3 * Math.sin(angle);
            Location particleLoc = new Location(player.getWorld(), x, player.getLocation().getY() + 1, z);
            
            player.getWorld().spawnParticle(Particle.FIREWORK, particleLoc, 30, 0.3, 0.5, 0.3, 0.1);
            player.getWorld().spawnParticle(Particle.FLASH, particleLoc, 1);
        }
        
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 2, 0), 100, 1, 1, 1, 0.5);
        
        // Show title
        player.showTitle(Title.title(
            Component.text("§6§l✦ ABILITY UNLOCKED! ✦"),
            Component.text(ability.getDisplayName()),
            Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(3), Duration.ofSeconds(1))
        ));
        
        // Send chat message
        player.sendMessage(Component.text("§a§l✦ Congratulations! ✦"));
        player.sendMessage(Component.text("§eYou received: " + ability.getDisplayName()));
        player.sendMessage(Component.text("§7Use right-click to activate!"));
        
        // Clear spin state
        isSpinning.put(playerId, false);
        spinFrames.remove(playerId);
    }
                                    }
