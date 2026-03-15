package com.mythicabilities.voting.katanas;

import com.mythicabilities.MythicAbilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.util.EulerAngle;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.*;

public class DragonsWrathKatana {
    
    private final MythicAbilities plugin;
    private final Random random = new Random();
    private final Map<UUID, DragonData> dragonUsers = new HashMap<>();
    private final Map<UUID, BossBar> ultimateBars = new HashMap<>();
    
    // Mathematical constants for dragon geometry
    private final double DRAGON_SCALE = 1.0;
    private final int DRAGON_SEGMENTS = 50;
    private final double[] SIN_TABLE = new double[360];
    private final double[] COS_TABLE = new double[360];
    
    public DragonsWrathKatana(MythicAbilities plugin) {
        this.plugin = plugin;
        initializeTrigTables();
    }
    
    private void initializeTrigTables() {
        for (int i = 0; i < 360; i++) {
            SIN_TABLE[i] = Math.sin(Math.toRadians(i));
            COS_TABLE[i] = Math.cos(Math.toRadians(i));
        }
    }
    
    public ItemStack createDragonKatana(Player owner) {
        ItemStack katana = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = katana.getItemMeta();
        
        meta.displayName(Component.text("§c§l❮ DRAGON'S WRATH ❯"));
        meta.setCustomModelData(1003);
        
        // Store owner data in persistent container
        meta.getPersistentDataContainer().set(
            new NamespacedKey(plugin, "katana_owner"),
            org.bukkit.persistence.PersistentDataType.STRING,
            owner.getUniqueId().toString()
        );
        
        meta.getPersistentDataContainer().set(
            new NamespacedKey(plugin, "katana_type"),
            org.bukkit.persistence.PersistentDataType.STRING,
            "Dragon's Wrath"
        );
        
        meta.lore(List.of(
            Component.text("§7The ancient dragon's soul awakens"),
            Component.text("§7within this legendary blade"),
            Component.text(""),
            Component.text("§c§lABILITIES:"),
            Component.text("§c• Dragon's Roar §7(Passive Aura)"),
            Component.text("§c• Flame Slash §7(Left Click)"),
            Component.text("§c• Dragon's Flight §7(Right Click)"),
            Component.text("§c• Dragon's Breath §7(Sneak + Left)"),
            Component.text("§4• §lDRAGON'S WRATH §7(Sneak + Right)"),
            Component.text(""),
            Component.text("§7Bound to: §e" + owner.getName()),
            Component.text(""),
            Component.text("§c§l⚔ LEGENDARY DRAGON KATANA ⚔")
        ));
        
        meta.addEnchant(org.bukkit.enchantments.Enchantment.SHARPNESS, 7, true);
        meta.addEnchant(org.bukkit.enchantments.Enchantment.FIRE_ASPECT, 3, true);
        meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 5, true);
        meta.setUnbreakable(true);
        
        katana.setItemMeta(meta);
        return katana;
    }
    
    // ============= PASSIVE: DRAGON'S ROAR =============
    
    public void triggerDragonRoar(Player player) {
        World world = player.getWorld();
        Location center = player.getLocation().clone().add(0, 1, 0);
        
        // Mathematical dragon scale pattern
        new BukkitRunnable() {
            int tick = 0;
            
            @Override
            public void run() {
                if (tick >= 100) { // 5 seconds
                    this.cancel();
                    return;
                }
                
                double radius = 5.0;
                double height = tick * 0.05;
                
                // Fibonacci spiral for dragon scales
                for (int i = 0; i < 20; i++) {
                    double phi = i * 137.5 * Math.PI / 180; // Golden angle
                    double r = radius * Math.sqrt(i * 0.05);
                    
                    double x = center.getX() + r * Math.cos(phi + tick * 0.1);
                    double z = center.getZ() + r * Math.sin(phi + tick * 0.1);
                    double y = center.getY() + height + Math.sin(phi * 3) * 0.5;
                    
                    Location scaleLoc = new Location(world, x, y, z);
                    
                    // Scale-like particles
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, scaleLoc, 1, 0, 0, 0, 0);
                    world.spawnParticle(Particle.END_ROD, scaleLoc, 1, 0, 0, 0, 0);
                    
                    // Connect scales with lines
                    if (i > 0) {
                        double prevPhi = (i-1) * 137.5 * Math.PI / 180;
                        double prevR = radius * Math.sqrt((i-1) * 0.05);
                        
                        double prevX = center.getX() + prevR * Math.cos(prevPhi + tick * 0.1);
                        double prevZ = center.getZ() + prevR * Math.sin(prevPhi + tick * 0.1);
                        double prevY = center.getY() + height + Math.sin(prevPhi * 3) * 0.5;
                        
                        // Draw line between scales
                        drawLine(new Location(world, prevX, prevY, prevZ), 
                                new Location(world, x, y, z), 
                                Particle.FLAME, 5);
                    }
                }
                
                // Apply effects to nearby enemies
                for (Entity e : world.getNearbyEntities(center, 5, 3, 5)) {
                    if (e instanceof LivingEntity target && !e.equals(player)) {
                        if (target instanceof Player) {
                            target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 1));
                            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 0));
                        }
                    }
                }
                
                // Dragon roar sound
                if (tick % 20 == 0) {
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 0.8f);
                }
                
                tick += 2;
            }
        }.runTaskTimer(plugin, 0, 2);
    }
    
    // ============= ABILITY 1: FLAME SLASH =============
    
    public void performFlameSlash(Player player) {
        Location start = player.getEyeLocation();
        Vector direction = start.getDirection().normalize();
        
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_SHOOT, 1, 1);
        
        // Dragon head geometry
        new BukkitRunnable() {
            double distance = 0;
            
            @Override
            public void run() {
                if (distance >= 8) {
                    this.cancel();
                    return;
                }
                
                distance += 0.5;
                
                // Create dragon head at current position
                Location headLoc = start.clone().add(direction.clone().multiply(distance));
                
                // Dragon head wireframe (mathematical)
                for (int i = 0; i < 360; i += 30) {
                    double rad = Math.toRadians(i);
                    
                    // Head shape - ellipsoid
                    double headX = headLoc.getX() + 0.8 * COS_TABLE[i];
                    double headY = headLoc.getY() + 0.6 * SIN_TABLE[i] + 0.3;
                    double headZ = headLoc.getZ() + 0.8 * COS_TABLE[i];
                    
                    Location particleLoc = new Location(player.getWorld(), headX, headY, headZ);
                    player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 2, 0.1, 0.1, 0.1, 0);
                    
                    // Horns
                    if (i % 90 == 0) {
                        double hornX = headLoc.getX() + 1.2 * COS_TABLE[i];
                        double hornY = headLoc.getY() + 1.0;
                        double hornZ = headLoc.getZ() + 1.2 * COS_TABLE[i];
                        
                        Location hornLoc = new Location(player.getWorld(), hornX, hornY, hornZ);
                        player.getWorld().spawnParticle(Particle.FLAME, hornLoc, 3, 0, 0, 0, 0);
                        
                        // Connect horn to head
                        drawLine(headLoc, hornLoc, Particle.FLAME, 3);
                    }
                }
                
                // Flame wave
                for (int i = 0; i < 360; i += 15) {
                    double rad = Math.toRadians(i);
                    double waveX = headLoc.getX() + 2 * Math.cos(rad);
                    double waveZ = headLoc.getZ() + 2 * Math.sin(rad);
                    
                    Location waveLoc = new Location(player.getWorld(), waveX, headLoc.getY(), waveZ);
                    player.getWorld().spawnParticle(Particle.FLAME, waveLoc, 3, 0.2, 0.2, 0.2, 0);
                }
                
                // Damage enemies
                for (Entity e : player.getWorld().getNearbyEntities(headLoc, 2, 2, 2)) {
                    if (e instanceof LivingEntity target && !e.equals(player)) {
                        target.damage(12, player);
                        target.setFireTicks(100);
                        
                        // Impact effect
                        player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, headLoc, 1);
                        player.getWorld().playSound(headLoc, Sound.ENTITY_GENERIC_EXPLODE, 1, 0.8f);
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    // ============= ABILITY 2: DRAGON'S FLIGHT =============
    
    public void performDragonFlight(Player player) {
        Location start = player.getLocation();
        DragonData data = new DragonData();
        data.isFlying = true;
        dragonUsers.put(player.getUniqueId(), data);
        
        // Create dragon wings
        new BukkitRunnable() {
            int flightTick = 0;
            
            @Override
            public void run() {
                if (flightTick >= 100 || !data.isFlying) { // 5 seconds flight
                    // Landing
                    performDragonLanding(player);
                    dragonUsers.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }
                
                // Wing animation - mathematical sine wave
                double wingAngle = Math.sin(flightTick * 0.3) * 30;
                
                for (int side = -1; side <= 1; side += 2) {
                    for (int segment = 0; segment < 10; segment++) {
                        double segmentProgress = segment / 10.0;
                        double x = player.getLocation().getX() + side * (2 + segment * 0.3) * Math.cos(Math.toRadians(wingAngle));
                        double y = player.getLocation().getY() + 1 + segment * 0.2;
                        double z = player.getLocation().getZ() + side * (1 + segment * 0.2);
                        
                        Location wingLoc = new Location(player.getWorld(), x, y, z);
                        
                        // Wing particles
                        player.getWorld().spawnParticle(Particle.END_ROD, wingLoc, 3, 0.1, 0.1, 0.1, 0);
                        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, wingLoc, 2, 0.1, 0.1, 0.1, 0);
                        
                        // Connect wing segments
                        if (segment > 0) {
                            double prevX = player.getLocation().getX() + side * (2 + (segment-1) * 0.3) * Math.cos(Math.toRadians(wingAngle));
                            double prevY = player.getLocation().getY() + 1 + (segment-1) * 0.2;
                            double prevZ = player.getLocation().getZ() + side * (1 + (segment-1) * 0.2);
                            
                            drawLine(new Location(player.getWorld(), prevX, prevY, prevZ),
                                    wingLoc, Particle.FLAME, 5);
                        }
                    }
                }
                
                // Flight trail
                Location trailLoc = player.getLocation().clone().add(0, 1, 0);
                player.getWorld().spawnParticle(Particle.FIREWORK, trailLoc, 10, 0.5, 0.5, 0.5, 0);
                
                // Flight sounds
                if (flightTick % 10 == 0) {
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 1.2f);
                }
                
                flightTick += 2;
            }
        }.runTaskTimer(plugin, 0, 2);
        
        // Launch player
        player.setVelocity(new Vector(0, 1.5, 0));
        player.setAllowFlight(true);
        player.setFlying(true);
        player.setFlySpeed(0.2f);
    }
    
    private void performDragonLanding(Player player) {
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setFlySpeed(0.1f);
        
        Location landLoc = player.getLocation();
        
        // Ground slam shockwave
        for (int i = 0; i < 360; i += 10) {
            double rad = Math.toRadians(i);
            double x = landLoc.getX() + 5 * Math.cos(rad);
            double z = landLoc.getZ() + 5 * Math.sin(rad);
            
            Location waveLoc = new Location(player.getWorld(), x, landLoc.getY(), z);
            
            // Shockwave rings
            for (double r = 0; r < 5; r += 0.5) {
                double ringX = landLoc.getX() + r * Math.cos(rad);
                double ringZ = landLoc.getZ() + r * Math.sin(rad);
                Location ringLoc = new Location(player.getWorld(), ringX, landLoc.getY(), ringZ);
                
                player.getWorld().spawnParticle(Particle.EXPLOSION, ringLoc, 1, 0, 0, 0, 0);
                player.getWorld().spawnParticle(Particle.BLOCK, ringLoc, 3, 0.2, 0.1, 0.2, Material.STONE.createBlockData());
            }
        }
        
        // Damage enemies
        for (Entity e : player.getWorld().getNearbyEntities(landLoc, 5, 3, 5)) {
            if (e instanceof LivingEntity target && !e.equals(player)) {
                target.damage(8, player);
                target.setVelocity(new Vector(0, 0.5, 0));
            }
        }
        
        player.getWorld().playSound(landLoc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1, 0.8f);
    }
    
    // ============= ABILITY 3: DRAGON'S BREATH =============
    
    public void performDragonBreath(Player player) {
        Location start = player.getEyeLocation();
        Vector direction = start.getDirection().normalize();
        
        player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_CHARGE, 1, 0.5f);
        
        // Charging effect
        new BukkitRunnable() {
            int chargeTick = 0;
            
            @Override
            public void run() {
                if (chargeTick >= 40) { // 2 second charge
                    releaseDragonBreath(player, start, direction);
                    this.cancel();
                    return;
                }
                
                // Charging spiral
                for (int i = 0; i < 360; i += 20) {
                    double rad = Math.toRadians(i + chargeTick * 10);
                    double radius = 2 + Math.sin(chargeTick * 0.2) * 0.5;
                    
                    double x = start.getX() + radius * Math.cos(rad);
                    double y = start.getY() + radius * Math.sin(rad);
                    double z = start.getZ();
                    
                    Location chargeLoc = new Location(player.getWorld(), x, y, z);
                    player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, chargeLoc, 2, 0, 0, 0, 0);
                }
                
                chargeTick++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    private void releaseDragonBreath(Player player, Location start, Vector direction) {
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_SHOOT, 2, 0.8f);
        
        new BukkitRunnable() {
            double distance = 0;
            double spread = 0;
            
            @Override
            public void run() {
                if (distance >= 10) {
                    this.cancel();
                    return;
                }
                
                distance += 0.3;
                spread += 0.1;
                
                // Cone of fire
                for (int i = -30; i <= 30; i += 5) {
                    double rad = Math.toRadians(i * spread);
                    Vector rotated = direction.clone().rotateAroundY(rad);
                    
                    for (double d = 0; d < 2; d += 0.5) {
                        Location breathLoc = start.clone().add(rotated.clone().multiply(distance + d));
                        
                        // Breath particles
                        player.getWorld().spawnParticle(Particle.DRAGON_BREATH, breathLoc, 3, 0.2, 0.2, 0.2, 0);
                        player.getWorld().spawnParticle(Particle.FLAME, breathLoc, 5, 0.1, 0.1, 0.1, 0);
                        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, breathLoc, 2, 0.1, 0.1, 0.1, 0);
                        
                        // Damage enemies
                        for (Entity e : player.getWorld().getNearbyEntities(breathLoc, 1.5, 1.5, 1.5)) {
                            if (e instanceof LivingEntity target && !e.equals(player)) {
                                target.damage(3, player);
                                target.setFireTicks(40);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    // ============= ULTIMATE: DRAGON'S WRATH =============
    
    public void performUltimate(Player player) {
        if (dragonUsers.containsKey(player.getUniqueId())) return;
        
        DragonData data = new DragonData();
        data.isUltimateActive = true;
        dragonUsers.put(player.getUniqueId(), data);
        
        // Ultimate boss bar
        BossBar ultimateBar = BossBar.bossBar(
            Component.text("§c§l❮ DRAGON'S WRATH ❯"),
            1.0f,
            BossBar.Color.RED,
            BossBar.Overlay.PROGRESS
        );
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showBossBar(ultimateBar);
        }
        ultimateBars.put(player.getUniqueId(), ultimateBar);
        
        // Ultimate sequence
        performUltimatePhase1(player);
    }
    
    private void performUltimatePhase1(Player player) {
        Location center = player.getLocation();
        World world = player.getWorld();
        
        // Phase 1: Awakening (0-2 seconds)
        player.setInvulnerable(true);
        player.setWalkSpeed(0);
        player.setAllowFlight(true);
        player.setFlying(true);
        
        // Teleport player up
        Location upLoc = center.clone().add(0, 5, 0);
        player.teleport(upLoc);
        
        // Sky change
        world.setStorm(true);
        world.setThundering(true);
        
        // Lightning strikes
        for (int i = 0; i < 20; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double radius = 15;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            
            Location strikeLoc = new Location(world, x, center.getY(), z);
            world.strikeLightningEffect(strikeLoc);
        }
        
        // Global sound
        world.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 2, 0.5f);
        
        // Title
        player.showTitle(Title.title(
            Component.text("§c§l❮ DRAGON'S WRATH ❯"),
            Component.text("§eThe ancient dragon awakens..."),
            Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(1))
        ));
        
        // Schedule phase 2
        new BukkitRunnable() {
            @Override
            public void run() {
                performUltimatePhase2(player);
            }
        }.runTaskLater(plugin, 40); // 2 seconds
    }
    
    private void performUltimatePhase2(Player player) {
        Location center = player.getLocation();
        World world = player.getWorld();
        
        // Phase 2: Transformation (2-5 seconds)
        
        // Create dragon skeleton (mathematical model)
        new BukkitRunnable() {
            int tick = 0;
            
            @Override
            public void run() {
                if (tick >= 60) { // 3 seconds
                    performUltimatePhase3(player);
                    this.cancel();
                    return;
                }
                
                double progress = tick / 60.0;
                double scale = 10 * progress;
                
                // Dragon body - parametric curve
                for (double t = 0; t < 2 * Math.PI; t += 0.2) {
                    // Body curve (sine wave)
                    double x = center.getX() + scale * Math.cos(t);
                    double y = center.getY() + Math.sin(t * 3) * 2;
                    double z = center.getZ() + scale * Math.sin(t);
                    
                    Location bodyLoc = new Location(world, x, y, z);
                    
                    // Scales
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, bodyLoc, 2, 0.1, 0.1, 0.1, 0);
                    world.spawnParticle(Particle.END_ROD, bodyLoc, 1, 0, 0, 0, 0);
                    
                    // Connect body segments
                    if (t > 0.2) {
                        double prevX = center.getX() + scale * Math.cos(t - 0.2);
                        double prevY = center.getY() + Math.sin((t - 0.2) * 3) * 2;
                        double prevZ = center.getZ() + scale * Math.sin(t - 0.2);
                        
                        drawLine(new Location(world, prevX, prevY, prevZ),
                                bodyLoc, Particle.FLAME, 10);
                    }
                }
                
                // Dragon head
                double headX = center.getX() + scale * 1.5;
                double headY = center.getY() + 2;
                double headZ = center.getZ();
                
                // Head sphere
                for (int phi = 0; phi < 360; phi += 30) {
                    for (int theta = 0; theta < 180; theta += 30) {
                        double radPhi = Math.toRadians(phi);
                        double radTheta = Math.toRadians(theta);
                        
                        double hx = headX + 1.5 * Math.sin(radTheta) * Math.cos(radPhi);
                        double hy = headY + 1.5 * Math.cos(radTheta);
                        double hz = headZ + 1.5 * Math.sin(radTheta) * Math.sin(radPhi);
                        
                        Location headLoc = new Location(world, hx, hy, hz);
                        world.spawnParticle(Particle.SOUL_FIRE_FLAME, headLoc, 1, 0, 0, 0, 0);
                    }
                }
                
                // Horns
                for (int side = -1; side <= 1; side += 2) {
                    for (int segment = 0; segment < 5; segment++) {
                        double hornX = headX + side * (1.0 + segment * 0.3);
                        double hornY = headY + 1.0 + segment * 0.4;
                        double hornZ = headZ;
                        
                        Location hornLoc = new Location(world, hornX, hornY, hornZ);
                        world.spawnParticle(Particle.FLAME, hornLoc, 2, 0, 0, 0, 0);
                    }
                }
                
                tick += 2;
            }
        }.runTaskTimer(plugin, 0, 2);
        
        // Dragon roar
        world.playSound(center, Sound.ENTITY_ENDER_DRAGON_AMBIENT, 2, 0.8f);
    }
    
    private void performUltimatePhase3(Player player) {
        Location center = player.getLocation();
        World world = player.getWorld();
        
        // Phase 3: Wrath (5-10 seconds) - Fire rain
        
        new BukkitRunnable() {
            int fireballCount = 0;
            
            @Override
            public void run() {
                if (fireballCount >= 50) {
                    performUltimatePhase4(player);
                    this.cancel();
                    return;
                }
                
                // Launch fireball
                double angle = random.nextDouble() * 2 * Math.PI;
                double x = center.getX() + 20 * Math.cos(angle);
                double z = center.getZ() + 20 * Math.sin(angle);
                double y = center.getY() + 15;
                
                Location start = new Location(world, x, y, z);
                Vector direction = center.toVector().subtract(start.toVector()).normalize();
                
                // Fireball trail
                new BukkitRunnable() {
                    Location current = start.clone();
                    
                    @Override
                    public void run() {
                        if (current.distance(center) < 1) {
                            // Impact
                            world.createExplosion(current, 0, false, false);
                            world.spawnParticle(Particle.EXPLOSION_EMITTER, current, 1);
                            
                            for (Entity e : world.getNearbyEntities(current, 3, 3, 3)) {
                                if (e instanceof LivingEntity target && !e.equals(player)) {
                                    target.damage(8, player);
                                }
                            }
                            
                            this.cancel();
                            return;
                        }
                        
                        current.add(direction.clone().multiply(0.5));
                        world.spawnParticle(Particle.FLAME, current, 5, 0.2, 0.2, 0.2, 0);
                        world.spawnParticle(Particle.SOUL_FIRE_FLAME, current, 3, 0.1, 0.1, 0.1, 0);
                    }
                }.runTaskTimer(plugin, 0, 1);
                
                fireballCount++;
            }
        }.runTaskTimer(plugin, 0, 2); // 2 ticks between fireballs = 50 in 5 seconds
    }
    
    private void performUltimatePhase4(Player player) {
        Location center = player.getLocation();
        World world = player.getWorld();
        
        // Phase 4: Finale (10-13 seconds) - Pull enemies
        
        // Pull effect
        new BukkitRunnable() {
            int pullTick = 0;
            
            @Override
            public void run() {
                if (pullTick >= 60) { // 3 seconds
                    performUltimatePhase5(player);
                    this.cancel();
                    return;
                }
                
                double pullStrength = 0.3;
                
                for (Entity e : world.getNearbyEntities(center, 30, 30, 30)) {
                    if (e instanceof LivingEntity target && !e.equals(player)) {
                        Vector pull = center.toVector().subtract(target.getLocation().toVector()).normalize().multiply(pullStrength);
                        target.setVelocity(pull);
                        
                        // Pull particles
                        drawLine(center, target.getLocation(), Particle.ENCHANT, 20);
                    }
                }
                
                // Golden light building
                for (int i = 0; i < 360; i += 20) {
                    double rad = Math.toRadians(i + pullTick * 5);
                    double x = center.getX() + (5 + pullTick * 0.1) * Math.cos(rad);
                    double z = center.getZ() + (5 + pullTick * 0.1) * Math.sin(rad);
                    
                    Location lightLoc = new Location(world, x, center.getY() + 2, z);
                    world.spawnParticle(Particle.END_ROD, lightLoc, 3, 0.1, 0.1, 0.1, 0);
                }
                
                pullTick++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    private void performUltimatePhase5(Player player) {
        Location center = player.getLocation();
        World world = player.getWorld();
        
        // Phase 5: Explosion (13-15 seconds)
        
        // Charge sound
        world.playSound(center, Sound.ENTITY_WITHER_SPAWN, 3, 0.5f);
        
        new BukkitRunnable() {
            int explodeTick = 0;
            
            @Override
            public void run() {
                if (explodeTick >= 40) { // 2 seconds
                    // Final explosion
                    performUltimateFinale(player);
                    this.cancel();
                    return;
                }
                
                double radius = 5 + explodeTick * 0.5;
                
                // Charging rings
                for (int i = 0; i < 360; i += 15) {
                    double rad = Math.toRadians(i + explodeTick * 10);
                    double x = center.getX() + radius * Math.cos(rad);
                    double z = center.getZ() + radius * Math.sin(rad);
                    
                    Location ringLoc = new Location(world, x, center.getY() + 2, z);
                    world.spawnParticle(Particle.TOTEM_OF_UNDYING, ringLoc, 2, 0.1, 0.1, 0.1, 0.5);
                    world.spawnParticle(Particle.FLASH, ringLoc, 1, 0, 0, 0, 0);
                }
                
                explodeTick++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    private void performUltimateFinale(Player player) {
        Location center = player.getLocation();
        World world = player.getWorld();
        DragonData data = dragonUsers.get(player.getUniqueId());
        
        // MASSIVE EXPLOSION
        for (int i = 0; i < 20; i++) {
            world.createExplosion(center, 0, false, false);
        }
        
        // Particle apocalypse
        for (int i = 0; i < 360; i += 5) {
            double rad = Math.toRadians(i);
            double x = center.getX() + 30 * Math.cos(rad);
            double z = center.getZ() + 30 * Math.sin(rad);
            
            for (double y = 0; y < 10; y += 2) {
                Location explodeLoc = new Location(world, x, center.getY() + y, z);
                world.spawnParticle(Particle.EXPLOSION_EMITTER, explodeLoc, 1);
                world.spawnParticle(Particle.FLASH, explodeLoc, 1);
                world.spawnParticle(Particle.TOTEM_OF_UNDYING, explodeLoc, 10, 1, 1, 1, 0.5);
            }
        }
        
        // Damage all enemies
        for (Entity e : world.getNearbyEntities(center, 30, 30, 30)) {
            if (e instanceof LivingEntity target && !e.equals(player)) {
                target.damage(20, player);
                target.setVelocity(new Vector(0, 2, 0));
            }
        }
        
        // Victory effects
        player.playSound(center, Sound.UI_TOAST_CHALLENGE_COMPLETE, 2, 1);
        world.playSound(center, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 2, 1);
        
        // Title
        player.showTitle(Title.title(
            Component.text("§c§l❮ DRAGON'S FURY ❯"),
            Component.text("§e20 damage to all enemies!"),
            Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(1))
        ));
        
        // End ultimate
        player.setInvulnerable(false);
        player.setWalkSpeed(0.2f);
        player.setAllowFlight(false);
        player.setFlying(false);
        
        // Remove boss bar
        BossBar bar = ultimateBars.remove(player.getUniqueId());
        if (bar != null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.hideBossBar(bar);
            }
        }
        
        dragonUsers.remove(player.getUniqueId());
    }
    
    // ============= UTILITY METHODS =============
    
    private void drawLine(Location start, Location end, Particle particle, int points) {
        World world = start.getWorld();
        Vector direction = end.toVector().subtract(start.toVector()).normalize();
        double distance = start.distance(end);
        
        for (int i = 0; i < points; i++) {
            double progress = (i * distance) / points;
            Location point = start.clone().add(direction.clone().multiply(progress));
            world.spawnParticle(particle, point, 1, 0, 0, 0, 0);
        }
    }
    
    // ============= DATA CLASS =============
    
    private class DragonData {
        boolean isFlying = false;
        boolean isUltimateActive = false;
        long lastRoar = 0;
        long lastSlash = 0;
        long lastFlight = 0;
        long lastBreath = 0;
        long lastUltimate = 0;
    }
}
