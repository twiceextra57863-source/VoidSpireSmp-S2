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

public class SuyousTranscendence extends Ability {
    
    private final MythicAbilities plugin;
    private final Map<UUID, Boolean> transcendenceActive = new HashMap<>();
    private final Map<UUID, Boolean> isMortalForm = new HashMap<>(); // true = mortal, false = immortal
    private final Map<UUID, Long> lastFormSwitch = new HashMap<>();
    private final Map<UUID, Map<String, Long>> skillCooldowns = new HashMap<>();
    private final Map<UUID, Integer> formStack = new HashMap<>();
    private final Random random = new Random();
    
    // Constants
    private final long FORM_SWITCH_COOLDOWN = 500; // 0.5 seconds
    private final long SKILL_COOLDOWN = 2000; // 2 seconds base
    
    public SuyousTranscendence(MythicAbilities plugin) {
        super("suyous_transcendence", "§6§lSuyou's Transcendence", 35, createIcon());
        this.plugin = plugin;
    }
    
    private static ItemStack createIcon() {
        ItemStack icon = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = icon.getItemMeta();
        meta.displayName(Component.text("§6§lSuyou's Transcendence"));
        meta.lore(List.of(
            Component.text("§7Auto-triggers after 3 combos!"),
            Component.text("§7Embody the Transient Immortal!"),
            Component.text("§7• Two Forms - Tap vs Hold:"),
            Component.text("§7  §fLeft Click/Tap = Mortal Form"),
            Component.text("§7  §f  +40% AS, +20% MS - Hit & Run"),
            Component.text("§7  §fRight Click/Hold = Immortal Form"),
            Component.text("§7  §f  30% DMG reduction, +25% DMG - Tank"),
            Component.text("§7• Sneak Key: Blade Surge"),
            Component.text("§7  §fTap: Throw + teleport"),
            Component.text("§7  §fHold: Charge + stun"),
            Component.text("§7• Shift Key: Soul Sever"),
            Component.text("§7  §fTap: Execute (missing HP)"),
            Component.text("§7  §fHold: 3 cleaves + heal"),
            Component.text("§7• Ctrl Key: Evil Queller"),
            Component.text("§7  §fTap: Backflip + slow"),
            Component.text("§7  §fHold: Charged arrow snipe"),
            Component.text("§7• Form Switch: 0.3s invincibility"),
            Component.text("§6Cooldown: 35 seconds")
        ));
        icon.setItemMeta(meta);
        return icon;
    }
    
    @Override
    public void onTrigger(Player player) {
        activateTranscendence(player);
    }
    
    @Override
    public void onCombo(Player player, Player target) {
        activateTranscendence(player);
    }
    
    private void activateTranscendence(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Start in mortal form by default
        isMortalForm.put(playerId, true);
        transcendenceActive.put(playerId, true);
        formStack.put(playerId, 0);
        skillCooldowns.put(playerId, new HashMap<>());
        
        // Broadcast
        player.sendMessage(Component.text("§6§l✦ TRANSIENT IMMORTAL AWAKENS ✦"));
        player.sendMessage(Component.text("§fMortal Form §7(Tap) | §eImmortal Form §7(Hold)"));
        
        // Title
        player.showTitle(Title.title(
            Component.text("§6§lSUYOU'S TRANSCENDENCE"),
            Component.text("§f12 seconds of dual-form mastery"),
            Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(1), Duration.ofMillis(500))
        ));
        
        // Play awakening sound
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THUNDER, 1, 0.8f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1, 1.2f);
        
        // Initial transformation effect
        player.getWorld().spawnParticle(Particle.FLASH, player.getLocation().add(0, 1, 0), 1);
        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation().add(0, 1, 0), 100, 1, 1, 1, 0.2);
        
        // Apply form buffs
        applyFormBuffs(player);
        
        // Start form particles
        startFormParticles(player);
        
        // End after 12 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                if (transcendenceActive.getOrDefault(playerId, false)) {
                    transcendentFinale(player);
                }
            }
        }.runTaskLater(plugin, 240); // 12 seconds
    }
    
    private void applyFormBuffs(Player player) {
        UUID playerId = player.getUniqueId();
        boolean mortal = isMortalForm.getOrDefault(playerId, true);
        
        // Clear previous form buffs
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.STRENGTH);
        player.removePotionEffect(PotionEffectType.RESISTANCE);
        player.removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
        player.removePotionEffect(PotionEffectType.FAST_DIGGING);
        
        if (mortal) {
            // Mortal Form: Speed & Attack Speed
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 260, 1)); // +20% movement
            player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, 260, 2)); // +40% attack speed
            player.sendActionBar(Component.text("§fMortal Form: §7+40% AS, +20% MS"));
        } else {
            // Immortal Form: Damage Reduction & Damage
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 260, 1)); // 30% reduction
            player.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 260, 0)); // +25% damage
            player.sendActionBar(Component.text("§eImmortal Form: §730% DMG reduction, +25% DMG"));
        }
    }
    
    private void startFormParticles(Player player) {
        UUID playerId = player.getUniqueId();
        
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 240 || !transcendenceActive.getOrDefault(playerId, false)) {
                    this.cancel();
                    return;
                }
                
                boolean mortal = isMortalForm.getOrDefault(playerId, true);
                Location loc = player.getLocation();
                
                if (mortal) {
                    // Mortal form particles - blue/white, fast-moving
                    player.getWorld().spawnParticle(Particle.END_ROD, loc.clone().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.02);
                    player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc.clone().add(0, 1, 0), 2, 0.3, 0.3, 0.3, 0);
                    
                    // Speed lines
                    if (player.isSprinting()) {
                        Vector dir = player.getLocation().getDirection().multiply(-0.5);
                        player.getWorld().spawnParticle(Particle.CLOUD, loc.clone().add(0, 1, 0).add(dir), 5, 0.1, 0.1, 0.1, 0.01);
                    }
                } else {
                    // Immortal form particles - gold/orange, heavy
                    player.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(0, 1, 0), 8, 0.4, 0.4, 0.4, 0.01);
                    player.getWorld().spawnParticle(Particle.BLOCK, loc.clone().add(0, 0.5, 0), 5, 0.3, 0.3, 0.3, Material.GOLD_BLOCK.createBlockData());
                    
                    // Ground impact
                    if (ticks % 10 == 0) {
                        player.getWorld().spawnParticle(Particle.BLOCK, loc.clone().add(0, 0.1, 0), 10, 0.5, 0.1, 0.5, Material.STONE.createBlockData());
                    }
                }
                
                ticks += 5;
            }
        }.runTaskTimer(plugin, 0, 5);
    }
    
    // Method to switch forms (called by listener on shift key)
    public void switchForm(Player player) {
        UUID playerId = player.getUniqueId();
        
        if (!transcendenceActive.getOrDefault(playerId, false)) return;
        
        // Check switch cooldown
        long now = System.currentTimeMillis();
        long lastSwitch = lastFormSwitch.getOrDefault(playerId, 0L);
        
        if (now - lastSwitch < FORM_SWITCH_COOLDOWN) {
            return; // Silent cooldown
        }
        
        lastFormSwitch.put(playerId, now);
        
        // Toggle form
        boolean currentForm = isMortalForm.getOrDefault(playerId, true);
        isMortalForm.put(playerId, !currentForm);
        
        // Brief invincibility
        player.setInvulnerable(true);
        new BukkitRunnable() {
            @Override
            public void run() {
                player.setInvulnerable(false);
            }
        }.runTaskLater(plugin, 6); // 0.3 seconds
        
        // Transition effect
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 1, 1.2f);
        player.getWorld().spawnParticle(Particle.FLASH, player.getLocation().add(0, 1, 0), 1);
        
        // Apply new form buffs
        applyFormBuffs(player);
        
        // Add stack for finale
        formStack.put(playerId, formStack.getOrDefault(playerId, 0) + 1);
    }
    
    // Sneak key = Blade Surge (Skill 1)
    public void bladeSurge(Player player, boolean holding) {
        UUID playerId = player.getUniqueId();
        
        if (!transcendenceActive.getOrDefault(playerId, false)) return;
        
        // Check cooldown
        if (!checkSkillCooldown(player, "bladeSurge")) return;
        
        boolean mortal = isMortalForm.getOrDefault(playerId, true);
        
        if (!holding) {
            // Tap - Mortal form: Throw weapon, teleport
            mortalBladeSurge(player);
        } else {
            // Hold - Immortal form: Charge + stun
            immortalBladeSurge(player);
        }
    }
    
    private void mortalBladeSurge(Player player) {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize().multiply(1.5);
        
        player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1, 1.2f);
        
        new BukkitRunnable() {
            Location loc = eye.clone();
            int distance = 0;
            
            @Override
            public void run() {
                if (distance > 8) {
                    // Teleport to weapon
                    player.teleport(loc);
                    
                    // Slash backwards
                    Vector backDir = player.getLocation().getDirection().multiply(-2);
                    Location slashLoc = player.getLocation().clone().add(backDir);
                    
                    // Damage in cone behind
                    for (Entity entity : player.getWorld().getNearbyEntities(slashLoc, 3, 2, 3)) {
                        if (entity instanceof LivingEntity target && !entity.equals(player)) {
                            double damage = 200 + random.nextInt(200); // 200-400 damage
                            target.damage(damage / 10, player); // Convert to Minecraft hearts
                            
                            // Hit effect
                            target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0, 1, 0), 3);
                            target.getWorld().spawnParticle(Particle.END_ROD, target.getLocation().add(0, 1, 0), 20, 0.3, 0.5, 0.3, 0.05);
                        }
                    }
                    
                    // Weapon throw return effect
                    player.getWorld().spawnParticle(Particle.END_ROD, loc, 30, 0.3, 0.3, 0.3, 0.1);
                    this.cancel();
                    return;
                }
                
                loc.add(direction);
                distance++;
                
                // Weapon trail
                player.getWorld().spawnParticle(Particle.END_ROD, loc, 5, 0.1, 0.1, 0.1, 0.02);
                player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 1, 0.1, 0.1, 0.1, 0);
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    private void immortalBladeSurge(Player player) {
        Location start = player.getLocation();
        Vector direction = start.getDirection().normalize();
        
        player.playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1, 0.6f);
        
        new BukkitRunnable() {
            double distance = 0;
            boolean hasStunned = false;
            
            @Override
            public void run() {
                if (distance >= 6 || hasStunned) {
                    this.cancel();
                    return;
                }
                
                // Move forward
                Location newLoc = player.getLocation().clone().add(direction.clone().multiply(1.2));
                player.teleport(newLoc);
                
                distance += 1.2;
                
                // Charge particles
                player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.02);
                player.getWorld().spawnParticle(Particle.BLOCK, player.getLocation().add(0, 0.5, 0), 5, 0.3, 0.3, 0.3, Material.GOLD_BLOCK.createBlockData());
                
                // Check for enemy hit
                for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation(), 2, 2, 2)) {
                    if (entity instanceof LivingEntity target && !entity.equals(player)) {
                        // Stun
                        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 16, 10)); // Effectively stun
                        
                        // Damage
                        double damage = 300 + random.nextInt(200); // 300-500 damage
                        target.damage(damage / 10, player);
                        
                        // Stun effect
                        target.getWorld().spawnParticle(Particle.FLASH, target.getLocation().add(0, 1, 0), 1);
                        target.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0);
                        
                        target.sendMessage(Component.text("§c§lSTUNNED!"));
                        
                        hasStunned = true;
                        this.cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    // Shift key = Soul Sever (Skill 2)
    public void soulSever(Player player, boolean holding) {
        UUID playerId = player.getUniqueId();
        
        if (!transcendenceActive.getOrDefault(playerId, false)) return;
        
        // Check cooldown
        if (!checkSkillCooldown(player, "soulSever")) return;
        
        boolean mortal = isMortalForm.getOrDefault(playerId, true);
        
        if (!holding) {
            // Tap - Mortal: Execute based on missing HP
            mortalSoulSever(player);
        } else {
            // Hold - Immortal: 3 cleaves + heal
            immortalSoulSever(player);
        }
    }
    
    private void mortalSoulSever(Player player) {
        // Sweeping attack - execute
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1.2f);
        
        // Particle fan
        for (int angle = -45; angle <= 45; angle += 5) {
            double rad = Math.toRadians(angle);
            Vector dir = player.getLocation().getDirection().rotateAroundY(rad);
            Location particleLoc = player.getEyeLocation().clone().add(dir.multiply(2));
            
            player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, particleLoc, 2, 0.2, 0.2, 0.2, 0);
        }
        
        // Damage enemies in cone
        for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation().add(player.getLocation().getDirection().multiply(2)), 4, 2, 4)) {
            if (entity instanceof LivingEntity target && !entity.equals(player)) {
                // Check if in cone (dot product)
                Vector toTarget = target.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
                double dot = player.getLocation().getDirection().dot(toTarget);
                
                if (dot > 0.7) { // In front cone
                    // Execute damage based on missing HP
                    double missingHpPercent = 1 - (target.getHealth() / target.getMaxHealth());
                    double bonusDamage = 50 + (missingHpPercent * 150); // 50-200 bonus
                    double baseDamage = 100;
                    
                    target.damage((baseDamage + bonusDamage) / 10, player);
                    
                    // Execute effect
                    target.getWorld().spawnParticle(Particle.FLASH, target.getLocation().add(0, 1, 0), 1);
                    target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.5);
                    
                    if (missingHpPercent > 0.5) {
                        target.sendMessage(Component.text("§c§lEXECUTE!"));
                    }
                }
            }
        }
    }
    
    private void immortalSoulSever(Player player) {
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1, 0.8f);
        
        new BukkitRunnable() {
            int cleaveCount = 0;
            
            @Override
            public void run() {
                if (cleaveCount >= 3) {
                    this.cancel();
                    return;
                }
                
                cleaveCount++;
                
                // Cleave effect
                for (int angle = -30; angle <= 30; angle += 10) {
                    double rad = Math.toRadians(angle);
                    Vector dir = player.getLocation().getDirection().rotateAroundY(rad);
                    Location cleaveLoc = player.getEyeLocation().clone().add(dir.multiply(3));
                    
                    player.getWorld().spawnParticle(Particle.BLOCK, cleaveLoc, 10, 0.5, 0.5, 0.5, Material.GOLD_BLOCK.createBlockData());
                }
                
                // Damage enemies
                double totalDamage = 0;
                
                for (Entity entity : player.getWorld().getNearbyEntities(player.getLocation().add(player.getLocation().getDirection().multiply(2)), 4, 2, 4)) {
                    if (entity instanceof LivingEntity target && !entity.equals(player)) {
                        double damage = (cleaveCount == 3) ? 400 : 250; // Third cleave does more
                        target.damage(damage / 10, player);
                        totalDamage += damage;
                        
                        // Heal on third cleave
                        if (cleaveCount == 3) {
                            double heal = totalDamage * 0.2; // 20% heal
                            player.setHealth(Math.min(player.getHealth() + (heal / 10), player.getMaxHealth()));
                            
                            // Heal effect
                            player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 2, 0), 5, 0.5, 0.5, 0.5, 0);
                        }
                    }
                }
                
                // Wait between cleaves
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
        }.runTaskTimer(plugin, 0, 6); // 0.3 seconds between cleaves
    }
    
    // Ctrl key = Evil Queller (Skill 3)
    public void evilQueller(Player player, boolean holding) {
        UUID playerId = player.getUniqueId();
        
        if (!transcendenceActive.getOrDefault(playerId, false)) return;
        
        // Check cooldown
        if (!checkSkillCooldown(player, "evilQueller")) return;
        
        boolean mortal = isMortalForm.getOrDefault(playerId, true);
        
        if (!holding) {
            // Tap - Mortal: Backflip + slow
            mortalEvilQueller(player);
        } else {
            // Hold - Immortal: Charged arrow snipe
            immortalEvilQueller(player);
        }
    }
    
    private void mortalEvilQueller(Player player) {
        // Backflip
        Vector backDir = player.getLocation().getDirection().multiply(-2).setY(0.5);
        player.setVelocity(backDir);
        
        player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_JUMP, 1, 1.2f);
        
        // Swing attack while flipping
        Location swingLoc = player.getLocation().clone().add(player.getLocation().getDirection().multiply(2));
        
        // Damage and slow enemies
        for (Entity entity : player.getWorld().getNearbyEntities(swingLoc, 3, 2, 3)) {
            if (entity instanceof LivingEntity target && !entity.equals(player)) {
                target.damage(150 / 10, player); // 150 damage
                
                // Slow
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 1)); // 40% slow for 1.5s
                
                // Hit effect
                target.getWorld().spawnParticle(Particle.CLOUD, target.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.05);
                target.sendMessage(Component.text("§fSlowed!"));
            }
        }
        
        // Trail during backflip
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 10) {
                    this.cancel();
                    return;
                }
                
                player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.01);
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    private void immortalEvilQueller(Player player) {
        player.sendMessage(Component.text("§eCharging arrow... release to fire!"));
        player.playSound(player.getLocation(), Sound.ENTITY_BREEZE_CHARGE, 1, 0.8f);
        
        // Charging particles
        new BukkitRunnable() {
            int chargeTicks = 0;
            
            @Override
            public void run() {
                if (chargeTicks >= 40) { // Max charge 2 seconds
                    fireArrow(player, chargeTicks);
                    this.cancel();
                    return;
                }
                
                // Charging effect
                player.getWorld().spawnParticle(Particle.FLAME, player.getEyeLocation(), 5, 0.1, 0.1, 0.1, 0.01);
                player.getWorld().spawnParticle(Particle.END_ROD, player.getEyeLocation(), 3, 0.1, 0.1, 0.1, 0);
                
                chargeTicks++;
            }
            
            @Override
            public synchronized void cancel() {
                super.cancel();
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    private void fireArrow(Player player, int chargeTicks) {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize().multiply(3);
        
        // Damage based on charge
        double damage = 150 + (chargeTicks * 10); // 150-550 damage
        
        player.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 2, 0.8f);
        
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
                
                // Arrow trail
                player.getWorld().spawnParticle(Particle.FLAME, loc, 10, 0.2, 0.2, 0.2, 0.02);
                player.getWorld().spawnParticle(Particle.CRIT, loc, 5, 0.1, 0.1, 0.1, 0.1);
                
                // Hit detection
                for (Entity entity : player.getWorld().getNearbyEntities(loc, 1.5, 1.5, 1.5)) {
                    if (entity instanceof LivingEntity target && !entity.equals(player)) {
                        target.damage(damage / 10, player);
                        
                        // Impact effect
                        target.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, target.getLocation(), 1);
                        target.getWorld().spawnParticle(Particle.FLAME, target.getLocation().add(0, 1, 0), 40, 0.5, 0.5, 0.5, 0.1);
                        
                        // Knockback
                        Vector knockback = direction.clone().multiply(1.5).setY(0.3);
                        target.setVelocity(knockback);
                        
                        this.cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    private boolean checkSkillCooldown(Player player, String skill) {
        UUID playerId = player.getUniqueId();
        Map<String, Long> cooldowns = skillCooldowns.get(playerId);
        
        long now = System.currentTimeMillis();
        long lastUse = cooldowns.getOrDefault(skill, 0L);
        
        if (now - lastUse < SKILL_COOLDOWN) {
            long remaining = (SKILL_COOLDOWN - (now - lastUse)) / 1000;
            player.sendActionBar(Component.text("§7" + skill + " cooldown: §6" + remaining + "s"));
            return false;
        }
        
        cooldowns.put(skill, now);
        return true;
    }
    
    private void transcendentFinale(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Grand Finale - both forms merge
        World world = player.getWorld();
        int stacks = formStack.getOrDefault(playerId, 0);
        
        // Massive explosion
        world.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 2, 0.6f);
        world.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 1, 1.2f);
        
        // True damage AOE (ignores armor)
        for (Entity entity : world.getNearbyEntities(player.getLocation(), 8, 5, 8)) {
            if (entity instanceof LivingEntity target && !entity.equals(player)) {
                // True damage (bypass armor - just direct damage)
                double damage = 300 + (stacks * 50); // 300-550 true damage
                target.damage(damage / 10, player);
                
                // Disarm (via weakness + mining fatigue)
                target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 5));
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 3));
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0));
                
                target.sendMessage(Component.text("§c§lTRANSCENDENT ASSAULT!"));
            }
        }
        
        // Particle apocalypse
        world.spawnParticle(Particle.FLASH, player.getLocation(), 3);
        world.spawnParticle(Particle.EXPLOSION_EMITTER, player.getLocation(), 2);
        world.spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation(), 500, 4, 3, 4, 0.2);
        world.spawnParticle(Particle.END_ROD, player.getLocation(), 400, 3, 3, 3, 0.3);
        
        // Title
        player.showTitle(Title.title(
            Component.text("§6§lTRANSCENDENT ASSAULT"),
            Component.text("§f" + stacks + " form stacks | 300 true damage"),
            Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(1))
        ));
        
        // Clean up
        transcendenceActive.remove(playerId);
        isMortalForm.remove(playerId);
        formStack.remove(playerId);
        skillCooldowns.remove(playerId);
        
        // Remove buffs
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.STRENGTH);
        player.removePotionEffect(PotionEffectType.RESISTANCE);
        player.removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
        player.removePotionEffect(PotionEffectType.FAST_DIGGING);
        
        player.sendMessage(Component.text("§6Suyou's Transcendence fades..."));
    }
    
    @Override
    public void onLeftClick(Player player) {
        // Tap - used for mortal form skills via listener
        // This is handled by the main listener calling bladeSurge with holding=false
    }
    
    @Override
    public void onRightClick(Player player) {
        // Hold - used for immortal form skills via listener
        // This is handled by the main listener calling bladeSurge with holding=true
    }
}
