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

public class MandelaEffect extends Ability {
    
    private final MythicAbilities plugin;
    private final Map<UUID, List<Player>> clones = new HashMap<>();
    private final Map<UUID, Boolean> realityGlitch = new HashMap<>();
    private final Map<UUID, Integer> loopCount = new HashMap<>();
    private final Random random = new Random();
    
    private final String[] glitchMessages = {
        "§5You feel... wrong...",
        "§dThis has happened before...",
        "§bYou're not sure what's real anymore...",
        "§cERROR: Memory corruption detected",
        "§aBut that's not how you remember it...",
        "§6The timeline is unstable...",
        "§fHave you been here before?",
        "§eThat's not how it happened...",
        "§2Wait... that's not right...",
        "§4SYSTEM MALFUNCTION"
    };
    
    private final String[] fakeMessages = {
        "§cYou have already died here...",
        "§eYou were supposed to be somewhere else...",
        "§aIs this a dream?",
        "§6You don't recognize this place...",
        "§dYour memories are fading...",
        "§5This never happened... or did it?"
    };
    
    public MandelaEffect(MythicAbilities plugin) {
        super("mandela_effect", "§5§lThe Mandela Effect", 40, createIcon());
        this.plugin = plugin;
    }
    
    private static ItemStack createIcon() {
        ItemStack icon = new ItemStack(Material.ENDER_EYE);
        ItemMeta meta = icon.getItemMeta();
        meta.displayName(Component.text("§5§lThe Mandela Effect"));
        meta.lore(List.of(
            Component.text("§7Auto-triggers after 3 combos!"),
            Component.text("§7Bend reality to your will"),
            Component.text("§7• Reality Shift - 12 seconds"),
            Component.text("§7• Create quantum clones"),
            Component.text("§7• Trap enemies in time loops"),
            Component.text("§7• Glitch their perception"),
            Component.text("§7• Reality Collapse finale"),
            Component.text("§5Cooldown: 40 seconds")
        ));
        icon.setItemMeta(meta);
        return icon;
    }
    
    @Override
    public void onTrigger(Player player) {
        activateMandelaEffect(player);
    }
    
    @Override
    public void onCombo(Player player, Player target) {
        activateMandelaEffect(player);
    }
    
    private void activateMandelaEffect(Player player) {
        UUID playerId = player.getUniqueId();
        realityGlitch.put(playerId, true);
        
        // Reality shift message
        Bukkit.broadcast(Component.text("§5§l✦✦✦ THE MANDELA EFFECT ✦✦✦"));
        Bukkit.broadcast(Component.text("§dReality is glitching..."));
        
        // Title for all players
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showTitle(Title.title(
                Component.text("§5§lREALITY GLITCH"),
                Component.text(p.equals(player) ? "§dYou control the timeline" : "§bWhat is real?"),
                Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(1))
            ));
        }
        
        // Apply effects to user
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 240, 4)); // Speed V
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 240, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 240, 2)); // Resistance III
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 240, 0));
        
        // Apply effects to enemies
        for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), 30, 30, 30)) {
            if (entity instanceof Player target && !target.equals(player)) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 240, 0));
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 240, 0));
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 240, 3));
                target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 240, 2));
                
                // Send random glitch messages
                sendGlitchMessages(target);
            }
        }
        
        // Create quantum clones
        createClones(player);
        
        // Start reality glitch effects
        startRealityGlitch(player);
        
        // Start memory manipulation
        startMemoryManipulation(player);
        
        // End after 12 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                endMandelaEffect(player);
            }
        }.runTaskLater(plugin, 240); // 12 seconds = 240 ticks
    }
    
    private void createClones(Player player) {
        List<Player> cloneList = new ArrayList<>();
        Location playerLoc = player.getLocation();
        
        for (int i = 0; i < 3; i++) {
            double angle = (i * 120) * Math.PI / 180;
            double x = playerLoc.getX() + 3 * Math.cos(angle);
            double z = playerLoc.getZ() + 3 * Math.sin(angle);
            
            Location cloneLoc = new Location(player.getWorld(), x, playerLoc.getY(), z);
            
            // Create armor stand as clone
            player.getWorld().playSound(cloneLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 0.8f);
            
            // Particle effect for clone appearance
            player.getWorld().spawnParticle(Particle.PORTAL, cloneLoc, 50, 1, 1, 1, 2);
            
            // Store clone info (we'll use armor stands or fake players)
            new BukkitRunnable() {
                int ticks = 0;
                
                @Override
                public void run() {
                    if (ticks >= 240 || !realityGlitch.getOrDefault(player.getUniqueId(), false)) {
                        this.cancel();
                        return;
                    }
                    
                    // Clone particle effect
                    player.getWorld().spawnParticle(Particle.ENCHANT, cloneLoc, 10, 0.5, 1, 0.5, 0);
                    player.getWorld().spawnParticle(Particle.ENCHANT, cloneLoc, 5, 0.5, 1, 0.5, 0);
                    
                    ticks += 5;
                }
            }.runTaskTimer(plugin, 0, 5);
        }
        
        clones.put(player.getUniqueId(), cloneList);
    }
    
    private void startRealityGlitch(Player player) {
        new BukkitRunnable() {
            int ticks = 0;
            World world = player.getWorld();
            
            @Override
            public void run() {
                if (ticks >= 240 || !realityGlitch.getOrDefault(player.getUniqueId(), false)) {
                    this.cancel();
                    return;
                }
                
                // Random visual glitches
                if (random.nextInt(10) < 3) {
                    // Glitch sky color temporarily
                    world.setStorm(random.nextBoolean());
                    
                    // Random lightning effect (visual only)
                    Location randomLoc = player.getLocation().clone().add(
                        random.nextInt(20) - 10,
                        0,
                        random.nextInt(20) - 10
                    );
                    world.strikeLightningEffect(randomLoc);
                    
                    // Screen glitch effect for enemies
                    for (Entity entity : world.getNearbyEntities(player.getLocation(), 30, 30, 30)) {
                        if (entity instanceof Player target && !target.equals(player)) {
                            target.sendMessage(glitchMessages[random.nextInt(glitchMessages.length)]);
                            
                            // Random screen effect
                            if (random.nextInt(5) == 0) {
                                target.showTitle(Title.title(
                                    Component.text("§kGLITCH"),
                                    Component.text("§r"),
                                    Title.Times.times(Duration.ZERO, Duration.ofMillis(500), Duration.ZERO)
                                ));
                            }
                        }
                    }
                }
                
                // Particle glitch
                for (int i = 0; i < 20; i++) {
                    Location randomLoc = player.getLocation().clone().add(
                        random.nextInt(30) - 15,
                        random.nextInt(10),
                        random.nextInt(30) - 15
                    );
                    
                    Particle particle = random.nextBoolean() ? Particle.PORTAL : Particle.ENCHANT;
                    world.spawnParticle(particle, randomLoc, 5, 0.5, 0.5, 0.5, 0.5);
                }
                
                ticks += 5;
            }
        }.runTaskTimer(plugin, 0, 5);
    }
    
    private void startMemoryManipulation(Player player) {
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 240 || !realityGlitch.getOrDefault(player.getUniqueId(), false)) {
                    this.cancel();
                    return;
                }
                
                for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), 30, 30, 30)) {
                    if (entity instanceof Player target && !target.equals(player)) {
                        // Random teleport of compass (visual only)
                        if (random.nextInt(10) < 2) {
                            Location fakeLoc = target.getLocation().clone().add(
                                random.nextInt(20) - 10,
                                0,
                                random.nextInt(20) - 10
                            );
                            target.setCompassTarget(fakeLoc);
                        }
                        
                        // Send fake messages
                        if (random.nextInt(15) < 2) {
                            target.sendMessage(fakeMessages[random.nextInt(fakeMessages.length)]);
                        }
                        
                        // Randomly change their direction perception
                        if (random.nextInt(20) < 2) {
                            target.setVelocity(new Vector(
                                random.nextDouble() - 0.5,
                                0.3,
                                random.nextDouble() - 0.5
                            ));
                        }
                        
                        // Potential time loop
                        if (random.nextInt(30) < 2 && !loopCount.containsKey(target.getUniqueId())) {
                            startTimeLoop(target);
                        }
                    }
                }
                
                ticks += 10;
            }
        }.runTaskTimer(plugin, 0, 10);
    }
    
    private void startTimeLoop(Player target) {
        loopCount.put(target.getUniqueId(), 0);
        Location loopStart = target.getLocation().clone();
        
        target.sendMessage("§dYou're stuck in a time loop...");
        target.playSound(target.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 1, 0.5f);
        
        new BukkitRunnable() {
            int loopTicks = 0;
            
            @Override
            public void run() {
                if (loopTicks >= 100) { // 5 seconds loop
                    loopCount.remove(target.getUniqueId());
                    
                    // Loop break damage
                    target.damage(4);
                    target.getWorld().spawnParticle(Particle.SONIC_BOOM, target.getLocation(), 1);
                    target.sendMessage("§cThe loop breaks... you're disoriented!");
                    
                    this.cancel();
                    return;
                }
                
                // Every second, reset to loop start
                if (loopTicks % 20 == 0) {
                    target.teleport(loopStart);
                    target.sendMessage("§7...wait, haven't you done this before?");
                    target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1, 0.5f);
                }
                
                loopTicks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    private void sendGlitchMessages(Player target) {
        new BukkitRunnable() {
            int count = 0;
            
            @Override
            public void run() {
                if (count >= 12) { // Send 12 messages over time
                    this.cancel();
                    return;
                }
                
                target.sendMessage(glitchMessages[random.nextInt(glitchMessages.length)]);
                count++;
            }
        }.runTaskTimer(plugin, 40, 40); // Every 2 seconds
    }
    
    private void endMandelaEffect(Player player) {
        UUID playerId = player.getUniqueId();
        realityGlitch.put(playerId, false);
        
        // Remove all effects from user
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        player.removePotionEffect(PotionEffectType.RESISTANCE);
        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        
        // Grand Finale - Reality Collapse
        World world = player.getWorld();
        
        // Massive particle explosion
        world.spawnParticle(Particle.EXPLOSION_EMITTER, player.getLocation(), 5);
        world.spawnParticle(Particle.PORTAL, player.getLocation(), 500, 5, 5, 5, 5);
        world.spawnParticle(Particle.FLASH, player.getLocation(), 1);
        
        // Big sound
        world.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2, 0.5f);
        world.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1, 0.8f);
        
        // Teleport all enemies randomly
        for (Entity entity : world.getNearbyEntities(player.getLocation(), 30, 30, 30)) {
            if (entity instanceof Player target && !target.equals(player)) {
                Location randomLoc = player.getLocation().clone().add(
                    random.nextInt(20) - 10,
                    0,
                    random.nextInt(20) - 10
                );
                randomLoc.setY(world.getHighestBlockYAt(randomLoc));
                target.teleport(randomLoc);
                
                target.sendMessage("§cReality collapses around you!");
            }
        }
        
        // Teleport user back to original
        player.teleport(player.getLocation());
        
        // Black & white effect (via night vision + blindness combo)
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 200, 0));
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 200, 0));
        
        // Final title
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showTitle(Title.title(
                Component.text("§5§lTHE MANDELA EFFECT"),
                Component.text("§dReality settles... for now"),
                Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(3), Duration.ofSeconds(1))
            ));
        }
        
        // Clear clones
        clones.remove(playerId);
        loopCount.clear();
        
        player.sendMessage(Component.text("§5The glitch fades away... but did it ever happen?"));
    }
    
    // Called from listener to handle quantum superposition
    public boolean isQuantumClone(Location loc) {
        // 50% chance that attacking a clone damages real player
        return random.nextBoolean();
    }
    
    @Override
    public void onLeftClick(Player player) {
        // Quantum blast on left click while active
        if (realityGlitch.getOrDefault(player.getUniqueId(), false)) {
            shootQuantumBlast(player);
        }
    }
    
    private void shootQuantumBlast(Player player) {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize().multiply(2);
        
        player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1, 0.8f);
        
        new BukkitRunnable() {
            Location loc = eye.clone();
            int distance = 0;
            
            @Override
            public void run() {
                if (distance > 20) {
                    this.cancel();
                    return;
                }
                
                loc.add(direction);
                distance++;
                
                // Glitch particles
                player.getWorld().spawnParticle(Particle.PORTAL, loc, 15, 0.2, 0.2, 0.2, 0.5);
                player.getWorld().spawnParticle(Particle.ENCHANT, loc, 10, 0.2, 0.2, 0.2, 0);
                
                for (Entity entity : player.getWorld().getNearbyEntities(loc, 1.5, 1.5, 1.5)) {
                    if (entity instanceof LivingEntity target && !entity.equals(player)) {
                        // Quantum damage (random 3-8)
                        int damage = 3 + random.nextInt(6);
                        target.damage(damage, player);
                        
                        // Glitch effect on hit
                        target.getWorld().spawnParticle(Particle.SONIC_BOOM, target.getLocation(), 1);
                        target.sendMessage(glitchMessages[random.nextInt(glitchMessages.length)]);
                        
                        this.cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    @Override
    public void onRightClick(Player player) {
        // Manual trigger
        if (!realityGlitch.getOrDefault(player.getUniqueId(), false)) {
            onTrigger(player);
        }
    }
            }
