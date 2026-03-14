package com.mythicabilities.abilities;

import com.mythicabilities.MythicAbilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
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

import java.time.Duration;
import java.util.*;

public class KagurasUmbrella extends Ability {
    
    private final MythicAbilities plugin;
    private final Map<UUID, Boolean> umbrellaActive = new HashMap<>();
    private final Map<UUID, Location> umbrellaLocation = new HashMap<>();
    private final Map<UUID, Boolean> isPurpleForm = new HashMap<>(); // true = purple (damage), false = yellow (control)
    private final Map<UUID, Long> lastThrow = new HashMap<>();
    private final Map<UUID, Long> lastTeleport = new HashMap<>();
    private final Map<UUID, ArmorStand> umbrellaEntity = new HashMap<>();
    private final Random random = new Random();
    
    public KagurasUmbrella(MythicAbilities plugin) {
        super("kaguras_umbrella", "§d§lKagura's Yin Yang", 32, createIcon());
        this.plugin = plugin;
    }
    
    private static ItemStack createIcon() {
        ItemStack icon = new ItemStack(Material.POPPED_CHORUS_FRUIT);
        ItemMeta meta = icon.getItemMeta();
        meta.displayName(Component.text("§d§lKagura's Yin Yang"));
        meta.lore(List.of(
            Component.text("§7Auto-triggers after 3 combos!"),
            Component.text("§7Summon the Seimei Umbrella!"),
            Component.text("§7• Left Click (Purple Form): Damage"),
            Component.text("§7  §f- Throw umbrella in line"),
            Component.text("§7  §f- Damage + Slow enemies"),
            Component.text("§7• Right Click (Yellow Form): Control"),
            Component.text("§7  §f- Spin umbrella around you"),
            Component.text("§7  §f- Pull + Root enemies"),
            Component.text("§7• Shift + Click: Rainbow Step"),
            Component.text("§7  §f- Teleport to umbrella"),
            Component.text("§7  §f- Damaging circle at both ends"),
            Component.text("§7• Finale: Yin Yang Collapse"),
            Component.text("§7  §f- Massive damage + stun + heal"),
            Component.text("§dCooldown: 32 seconds")
        ));
        icon.setItemMeta(meta);
        return icon;
    }
    
    @Override
    public void onTrigger(Player player) {
        activateYinYang(player);
    }
    
    @Override
    public void onCombo(Player player, Player target) {
        activateYinYang(player);
    }
    
    private void activateYinYang(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Start in purple form by default
        isPurpleForm.put(playerId, true);
        umbrellaActive.put(playerId, true);
        
        // Spawn umbrella initially near player
        Location umbrellaLoc = player.getLocation().clone().add(0, 2, 0);
        umbrellaLocation.put(playerId, umbrellaLoc);
        
        // Create visual umbrella entity
        ArmorStand umbrella = player.getWorld().spawn(umbrellaLoc, ArmorStand.class);
        umbrella.setInvisible(true);
        umbrella.setMarker(true);
        umbrella.setGravity(false);
        umbrella.setSmall(true);
        umbrellaEntity.put(playerId, umbrella);
        
        // Broadcast
        player.sendMessage(Component.text("§d§l✦ SEIMEI UMBRELLA SUMMONED ✦"));
        player.sendMessage(Component.text("§fPurple Form §7(Damage) | §eYellow Form §7(Control)"));
        
        // Title
        player.showTitle(Title.title(
            Component.text("§d§lYIN YANG CIRCLE"),
            Component.text("§f12 seconds of magic mastery"),
            Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(1), Duration.ofMillis(500))
        ));
        
        // Play summon sound
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1, 0.8f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_CHORUS_FRUIT_TELEPORT, 1, 1.2f);
        
        // Initial particle burst
        player.getWorld().spawnParticle(Particle.SPELL_MOB, umbrellaLoc, 100, 0.5, 0.5, 0.5, 1);
        player.getWorld().spawnParticle(Particle.PORTAL, umbrellaLoc, 50, 0.3, 0.3, 0.3, 0.5);
        
        // Start umbrella particles
        startUmbrellaParticles(player);
        
        // End after 12 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                if (umbrellaActive.getOrDefault(playerId, false)) {
                    yinYangCollapse(player);
                }
            }
        }.runTaskLater(plugin, 240); // 12 seconds
    }
    
    private void startUmbrellaParticles(Player player) {
        UUID playerId = player.getUniqueId();
        
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 240 || !umbrellaActive.getOrDefault(playerId, false)) {
                    this.cancel();
                    return;
                }
                
                Location umbrellaLoc = umbrellaLocation.get(playerId);
                if (umbrellaLoc == null) return;
                
                boolean purpleForm = isPurpleForm.getOrDefault(playerId, true);
                
                // Move umbrella entity
                ArmorStand umbrellaEnt = umbrellaEntity.get(playerId);
                if (umbrellaEnt != null) {
                    umbrellaEnt.teleport(umbrellaLoc);
                }
                
                // Form-specific particles
                if (purpleForm) {
                    // Purple particles (damage form)
                    player.getWorld().spawnParticle(Particle.SPELL_MOB, umbrellaLoc, 10, 0.2, 0.2, 0.2, 1);
                    player.getWorld().spawnParticle(Particle.PORTAL, umbrellaLoc, 5, 0.1, 0.1, 0.1, 0.5);
                    
                    // Purple aura around player
                    player.getWorld().spawnParticle(Particle.SPELL_MOB, player.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 1);
                } else {
                    // Yellow particles (control form)
                    player.getWorld().spawnParticle(Particle.WAX_ON, umbrellaLoc, 10, 0.2, 0.2, 0.2, 0);
                    player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, umbrellaLoc, 5, 0.2, 0.2, 0.2, 0);
                    
                    // Yellow aura around player
                    player.getWorld().spawnParticle(Particle.WAX_ON, player.getLocation().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0);
                }
                
                // Yin Yang symbol on ground under umbrella
                for (int i = 0; i < 4; i++) {
                    double angle = (ticks * 2 + i * 90) * Math.PI / 180;
                    double x = umbrellaLoc.getX() + 0.8 * Math.cos(angle);
                    double z = umbrellaLoc.getZ() + 0.8 * Math.sin(angle);
                    
                    Location symbolLoc = new Location(player.getWorld(), x, umbrellaLoc.getY() - 1, z);
                    if (purpleForm) {
                        player.getWorld().spawnParticle(Particle.SPELL_MOB, symbolLoc, 2, 0, 0.1, 0, 1);
                    } else {
                        player.getWorld().spawnParticle(Particle.WAX_ON, symbolLoc, 2, 0, 0.1, 0, 0);
                    }
                }
                
                ticks += 5;
            }
        }.runTaskTimer(plugin, 0, 5);
    }
    
    @Override
    public void onLeftClick(Player player) {
        if (umbrellaActive.getOrDefault(player.getUniqueId(), false)) {
            // Left click always uses current form's throw
            throwUmbrella(player);
        }
    }
    
    @Override
    public void onRightClick(Player player) {
        if (umbrellaActive.getOrDefault(player.getUniqueId(), false)) {
            // Right click toggles form
            toggleForm(player);
        }
    }
    
    // Special method for shift+click (handled in listener)
    public void onShiftClick(Player player) {
        if (umbrellaActive.getOrDefault(player.getUniqueId(), false)) {
            rainbowStep(player);
        }
    }
    
    private void toggleForm(Player player) {
        UUID playerId = player.getUniqueId();
        boolean currentForm = isPurpleForm.getOrDefault(playerId, true);
        boolean newForm = !currentForm;
        
        isPurpleForm.put(playerId, newForm);
        
        // Effect
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1.5f);
        
        // Particle burst
        Location umbrellaLoc = umbrellaLocation.get(playerId);
        if (umbrellaLoc != null) {
            if (newForm) {
                // Switching to purple
                player.getWorld().spawnParticle(Particle.SPELL_MOB, umbrellaLoc, 50, 0.5, 0.5, 0.5, 1);
                player.sendActionBar(Component.text("§dPurple Form §f(Damage Mode)"));
            } else {
                // Switching to yellow
                player.getWorld().spawnParticle(Particle.WAX_ON, umbrellaLoc, 50, 0.5, 0.5, 0.5, 0);
                player.sendActionBar(Component.text("§eYellow Form §f(Control Mode)"));
            }
        }
    }
    
    private void throwUmbrella(Player player) {
        UUID playerId = player.getUniqueId();
        boolean purpleForm = isPurpleForm.getOrDefault(playerId, true);
        
        // Check cooldown
        long now = System.currentTimeMillis();
        long lastThrowTime = lastThrow.getOrDefault(playerId, 0L);
        
        if (now - lastThrowTime < 2000) { // 2 second cooldown
            long remaining = (2000 - (now - lastThrowTime)) / 1000;
            player.sendActionBar(Component.text("§7Umbrella cooldown: §d" + remaining + "s"));
            return;
        }
        
        lastThrow.put(playerId, now);
        
        Location start = player.getEyeLocation();
        Vector direction = start.getDirection().normalize().multiply(1.5);
        Location umbrellaLoc = umbrellaLocation.get(playerId);
        
        if (purpleForm) {
            // Purple form - throw in line
            throwLineDamage(player, start, direction);
        } else {
            // Yellow form - spin around
            spinUmbrella(player);
        }
    }
    
    private void throwLineDamage(Player player, Location start, Vector direction) {
        UUID playerId = player.getUniqueId();
        
        player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1, 0.8f);
        
        new BukkitRunnable() {
            Location loc = start.clone();
            int distance = 0;
            
            @Override
            public void run() {
                if (distance > 15) {
                    // Update umbrella location
                    umbrellaLocation.put(playerId, loc);
                    this.cancel();
                    return;
                }
                
                loc.add(direction);
                distance++;
                
                // Purple trail
                player.getWorld().spawnParticle(Particle.SPELL_MOB, loc, 15, 0.2, 0.2, 0.2, 1);
                player.getWorld().spawnParticle(Particle.PORTAL, loc, 8, 0.1, 0.1, 0.1, 0.3);
                
                // Damage enemies in path
                for (Entity entity : player.getWorld().getNearbyEntities(loc, 1.5, 1.5, 1.5)) {
                    if (entity instanceof LivingEntity target && !entity.equals(player)) {
                        // Damage
                        target.damage(8, player); // 4 hearts
                        
                        // Slow
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));
                        
                        // Hit effect
                        target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0);
                        target.getWorld().spawnParticle(Particle.SPELL_MOB, target.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 1);
                        
                        target.sendMessage(Component.text("§dYin energy drains you!"));
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    private void spinUmbrella(Player player) {
        UUID playerId = player.getUniqueId();
        Location center = player.getLocation();
        
        player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_IDLE_GROUND, 1, 1.2f);
        
        // Update umbrella location to spin around player
        new BukkitRunnable() {
            int angle = 0;
            
            @Override
            public void run() {
                if (angle >= 360) {
                    // Place umbrella in front after spin
                    Location finalLoc = player.getLocation().clone().add(player.getLocation().getDirection().multiply(2));
                    umbrellaLocation.put(playerId, finalLoc);
                    this.cancel();
                    return;
                }
                
                double rad = Math.toRadians(angle);
                double x = center.getX() + 3 * Math.cos(rad);
                double z = center.getZ() + 3 * Math.sin(rad);
                
                Location spinLoc = new Location(player.getWorld(), x, center.getY() + 1, z);
                umbrellaLocation.put(playerId, spinLoc);
                
                // Pull enemies toward center
                for (Entity entity : player.getWorld().getNearbyEntities(spinLoc, 3, 2, 3)) {
                    if (entity instanceof LivingEntity target && !entity.equals(player)) {
                        Vector pull = center.toVector().subtract(target.getLocation().toVector()).normalize().multiply(0.5);
                        target.setVelocity(pull);
                        
                        // Root if close enough
                        if (target.getLocation().distance(center) < 2) {
                            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 10)); // Effectively root
                            target.sendMessage(Component.text("§eYang energy holds you!"));
                        }
                        
                        // Yellow particles
                        player.getWorld().spawnParticle(Particle.WAX_ON, target.getLocation().add(0, 1, 0), 10, 0.2, 0.3, 0.2, 0);
                    }
                }
                
                angle += 15;
            }
        }.runTaskTimer(plugin, 0, 2);
    }
    
    private void rainbowStep(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Check cooldown
        long now = System.currentTimeMillis();
        long lastTeleportTime = lastTeleport.getOrDefault(playerId, 0L);
        
        if (now - lastTeleportTime < 4000) { // 4 second cooldown
            long remaining = (4000 - (now - lastTeleportTime)) / 1000;
            player.sendActionBar(Component.text("§7Rainbow Step cooldown: §d" + remaining + "s"));
            return;
        }
        
        Location umbrellaLoc = umbrellaLocation.get(playerId);
        if (umbrellaLoc == null) return;
        
        Location originalLoc = player.getLocation();
        
        // Teleport
        player.teleport(umbrellaLoc);
        
        lastTeleport.put(playerId, now);
        
        // Effects
        player.getWorld().playSound(originalLoc, Sound.ITEM_CHORUS_FRUIT_TELEPORT, 1, 0.8f);
        player.getWorld().playSound(umbrellaLoc, Sound.ITEM_CHORUS_FRUIT_TELEPORT, 1, 1.2f);
        
        // Rainbow particles at both locations
        for (Location loc : Arrays.asList(originalLoc, umbrellaLoc)) {
            for (int i = 0; i < 3; i++) {
                double hue = (i * 120) * Math.PI / 180;
                player.getWorld().spawnParticle(Particle.SPELL_MOB, loc.clone().add(0, 1, 0), 30, 0.3, 0.3, 0.3, 1);
                player.getWorld().spawnParticle(Particle.WAX_ON, loc.clone().add(0, 1, 0), 20, 0.2, 0.2, 0.2, 0);
            }
            
            // Damage circle at both positions
            for (Entity entity : player.getWorld().getNearbyEntities(loc, 4, 3, 4)) {
                if (entity instanceof LivingEntity target && !entity.equals(player)) {
                    target.damage(4, player); // 2 hearts
                    
                    // Purple or yellow based on form
                    if (isPurpleForm.getOrDefault(playerId, true)) {
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1));
                    } else {
                        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60, 0));
                    }
                }
            }
        }
        
        player.sendActionBar(Component.text("§dRainbow Step!"));
    }
    
    private void yinYangCollapse(Player player) {
        UUID playerId = player.getUniqueId();
        Location umbrellaLoc = umbrellaLocation.get(playerId);
        
        if (umbrellaLoc == null) return;
        
        // Grand Finale - Massive explosion
        World world = player.getWorld();
        
        // Sound
        world.playSound(umbrellaLoc, Sound.ENTITY_WITHER_SPAWN, 1, 0.6f);
        world.playSound(umbrellaLoc, Sound.ENTITY_GENERIC_EXPLODE, 2, 0.5f);
        
        // Massive particle burst
        world.spawnParticle(Particle.EXPLOSION_EMITTER, umbrellaLoc, 3);
        world.spawnParticle(Particle.FLASH, umbrellaLoc, 1);
        world.spawnParticle(Particle.SPELL_MOB, umbrellaLoc, 500, 5, 3, 5, 2);
        world.spawnParticle(Particle.WAX_ON, umbrellaLoc, 500, 5, 3, 5, 2);
        world.spawnParticle(Particle.PORTAL, umbrellaLoc, 300, 4, 3, 4, 2);
        
        // Damage and stun all enemies in large area
        double totalDamageDealt = 0;
        
        for (Entity entity : world.getNearbyEntities(umbrellaLoc, 8, 5, 8)) {
            if (entity instanceof LivingEntity target && !entity.equals(player)) {
                // Damage
                double damage = 15; // 7.5 hearts
                target.damage(damage, player);
                totalDamageDealt += Math.min(damage, target.getHealth());
                
                // Stun (via slowness + blindness)
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 10));
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
                target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 1));
                
                // Mark effect
                target.sendMessage(Component.text("§d§lYIN YANG COLLAPSE!"));
                
                // Individual hit effect
                world.spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0);
            }
        }
        
        // Heal user for 20% of damage dealt
        double healAmount = totalDamageDealt * 0.2;
        player.setHealth(Math.min(player.getHealth() + healAmount, player.getMaxHealth()));
        
        // Healing effect
        player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 2, 0), (int)healAmount, 0.5, 0.5, 0.5, 0);
        
        // Title
        player.showTitle(Title.title(
            Component.text("§d§lYIN YANG COLLAPSE"),
            Component.text("§f" + Math.round(totalDamageDealt) + " damage | +" + Math.round(healAmount) + " health"),
            Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(1), Duration.ofMillis(500))
        ));
        
        // Clean up
        endYinYang(player);
    }
    
    private void endYinYang(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Remove umbrella entity
        ArmorStand umbrellaEnt = umbrellaEntity.get(playerId);
        if (umbrellaEnt != null) {
            umbrellaEnt.remove();
        }
        
        // Clear all maps
        umbrellaActive.remove(playerId);
        umbrellaLocation.remove(playerId);
        isPurpleForm.remove(playerId);
        umbrellaEntity.remove(playerId);
        lastThrow.remove(playerId);
        lastTeleport.remove(playerId);
        
        player.sendMessage(Component.text("§dSeimei Umbrella fades away..."));
    }
    
    @Override
    public void onLeftClick(Player player) {
        // Already handled above
    }
    
    @Override
    public void onRightClick(Player player) {
        // Already handled above
    }
}
