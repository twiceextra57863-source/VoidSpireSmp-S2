package com.mythicabilities.voting;

import com.mythicabilities.MythicAbilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.*;

public class KatanaManager {
    
    private final MythicAbilities plugin;
    private final Map<UUID, Integer> deathCount = new HashMap<>();
    private final Map<UUID, Long> teamCreationCooldown = new HashMap<>();
    private Bounty activeBounty = null;
    private BossBar bountyBar;
    private final Random random = new Random();
    
    // Constants
    private final long COOLDOWN_24H = 86400000; // 24 hours in milliseconds
    private final int MAX_DEATHS = 3;
    
    public KatanaManager(MythicAbilities plugin) {
        this.plugin = plugin;
        this.bountyBar = BossBar.bossBar(Component.text("§6⚔ No Active Bounty ⚔"), 1.0f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);
    }
    
    // ========== NEW: Getter methods for KatanaCommand ==========
    
    /**
     * Get death count for a player
     */
    public int getDeathCount(Player player) {
        return deathCount.getOrDefault(player.getUniqueId(), 0);
    }
    
    /**
     * Get team creation cooldown in hours
     */
    public long getTeamCooldown(Player player) {
        UUID playerId = player.getUniqueId();
        Long cooldown = teamCreationCooldown.get(playerId);
        
        if (cooldown == null) return 0;
        
        long timeLeft = (cooldown + COOLDOWN_24H) - System.currentTimeMillis();
        return Math.max(0, timeLeft / 3600000); // Convert to hours
    }
    
    // ========== Original Methods ==========
    
    /**
     * Create a katana bound to a specific leader
     */
    public ItemStack createBoundKatana(Player owner, String katanaType) {
        ItemStack katana = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = katana.getItemMeta();
        
        // Set display name based on katana type
        String displayName = getKatanaDisplayName(katanaType);
        meta.displayName(Component.text(displayName));
        
        // Set custom model data for texture
        meta.setCustomModelData(1000 + getKatanaIndex(katanaType));
        
        // Add persistent data to bind to owner
        meta.getPersistentDataContainer().set(
            new NamespacedKey(plugin, "katana_owner"),
            PersistentDataType.STRING,
            owner.getUniqueId().toString()
        );
        
        meta.getPersistentDataContainer().set(
            new NamespacedKey(plugin, "katana_type"),
            PersistentDataType.STRING,
            katanaType
        );
        
        meta.getPersistentDataContainer().set(
            new NamespacedKey(plugin, "is_bound"),
            PersistentDataType.BOOLEAN,
            true
        );
        
        // Add lore
        meta.lore(List.of(
            Component.text("§7Bound to: §e" + owner.getName()),
            Component.text("§7Type: " + getKatanaColor(katanaType) + katanaType),
            Component.text(""),
            Component.text("§c§l⚠ BOUND ITEM ⚠"),
            Component.text("§7• Cannot be dropped"),
            Component.text("§7• Cannot be stored"),
            Component.text("§7• Vanishes on death"),
            Component.text("§7• Becomes bounty on 3rd death")
        ));
        
        // Add enchantments
        meta.addEnchant(org.bukkit.enchantments.Enchantment.SHARPNESS, 5, true);
        meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 3, true);
        meta.addEnchant(org.bukkit.enchantments.Enchantment.MENDING, 1, true);
        
        // Make unbreakable
        meta.setUnbreakable(true);
        
        katana.setItemMeta(meta);
        return katana;
    }
    
    /**
     * Check if katana is bound to a player
     */
    public boolean isBoundKatana(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(
            new NamespacedKey(plugin, "is_bound"),
            PersistentDataType.BOOLEAN
        );
    }
    
    /**
     * Get owner of bound katana
     */
    public UUID getKatanaOwner(ItemStack item) {
        if (!isBoundKatana(item)) return null;
        
        String ownerUuid = item.getItemMeta().getPersistentDataContainer().get(
            new NamespacedKey(plugin, "katana_owner"),
            PersistentDataType.STRING
        );
        
        return ownerUuid != null ? UUID.fromString(ownerUuid) : null;
    }
    
    /**
     * Handle player death with katana
     */
    public void handleKatanaDeath(Player player) {
        UUID playerId = player.getUniqueId();
        Team team = plugin.getVoteManager().getTeam(player);
        
        if (team == null) return;
        
        // Check if player is leader
        if (team.isLeader(player)) {
            // Increment death count
            int deaths = deathCount.getOrDefault(playerId, 0) + 1;
            deathCount.put(playerId, deaths);
            
            // Broadcast death message
            Bukkit.broadcast(Component.text("§c§l⚠ " + player.getName() + " has died! §e" + 
                deaths + "/" + MAX_DEATHS + " deaths ⚠"));
            
            // Check if reached max deaths
            if (deaths >= MAX_DEATHS) {
                handleTeamDisband(team, player);
            } else {
                // Respawn with same katana (it's bound)
                // The katana will be given on respawn via event listener
            }
        } else {
            // Member death - just respawn, no team impact
            player.sendMessage("§7You died as a team member. Your leader is still safe!");
        }
    }
    
    /**
     * Handle team disband on 3 deaths
     */
    private void handleTeamDisband(Team team, Player leader) {
        UUID leaderId = leader.getUniqueId();
        
        // Set cooldown
        teamCreationCooldown.put(leaderId, System.currentTimeMillis());
        
        // Get all members
        List<Player> members = team.getMembers();
        
        // Broadcast disband message
        Bukkit.broadcast(Component.text("§c§l╔════════════════════════════════════╗"));
        Bukkit.broadcast(Component.text("§c§l║     💀 KINGDOM FALLEN! 💀         §c§l║"));
        Bukkit.broadcast(Component.text("§c§l║                                    §c§l║"));
        Bukkit.broadcast(Component.text("§c§l║  " + leader.getName() + "'s kingdom has    §c§l║"));
        Bukkit.broadcast(Component.text("§c§l║  been destroyed!                  §c§l║"));
        Bukkit.broadcast(Component.text("§c§l║                                    §c§l║"));
        Bukkit.broadcast(Component.text("§c§l║  §7The katana becomes a §eBOUNTY§7!   §c§l║"));
        Bukkit.broadcast(Component.text("§c§l╚════════════════════════════════════╝"));
        
        // Remove katana from leader
        removeBoundKatana(leader);
        
        // Create bounty
        createBounty(leader, team.getKatanaType(), members);
        
        // Disband team
        plugin.getVoteManager().disbandTeam(team);
        
        // Notify members
        for (Player member : members) {
            if (!member.equals(leader)) {
                member.sendMessage("§cYour kingdom has fallen! You are now free.");
            }
        }
    }
    
    /**
     * Create a bounty for the katana
     */
    private void createBounty(Player fallenLeader, String katanaType, List<Player> members) {
        // Create bounty item
        ItemStack bountyKatana = createBoundKatana(fallenLeader, katanaType);
        
        // Create armor stand to hold bounty
        Location spawnLoc = fallenLeader.getLocation().clone().add(0, 2, 0);
        ArmorStand bountyStand = (ArmorStand) fallenLeader.getWorld().spawnEntity(spawnLoc, EntityType.ARMOR_STAND);
        bountyStand.setGravity(false);
        bountyStand.setInvulnerable(true);
        bountyStand.setVisible(false);
        bountyStand.setMarker(true);
        bountyStand.setSmall(true);
        bountyStand.getEquipment().setHelmet(bountyKatana);
        
        // Store bounty info
        activeBounty = new Bounty(
            bountyStand,
            bountyKatana,
            katanaType,
            members,
            System.currentTimeMillis() + 600000 // 10 minutes
        );
        
        // Start bounty effects
        startBountyEffects();
    }
    
    /**
     * Start bounty effects (particles, music, boss bar)
     */
    private void startBountyEffects() {
        if (activeBounty == null) return;
        
        ArmorStand stand = activeBounty.getStand();
        Location loc = stand.getLocation();
        World world = loc.getWorld();
        
        // Update boss bar
        bountyBar.name(Component.text("§6⚔ BOUNTY: " + activeBounty.getKatanaType() + " ⚔"));
        bountyBar.progress(1.0f);
        
        // Show boss bar to all players
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showBossBar(bountyBar);
        }
        
        // Start particle effects
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (activeBounty == null || stand.isDead()) {
                    this.cancel();
                    return;
                }
                
                long timeLeft = activeBounty.getEndTime() - System.currentTimeMillis();
                if (timeLeft <= 0) {
                    // Bounty expired - give to nearest player
                    claimBounty(null);
                    this.cancel();
                    return;
                }
                
                // Update boss bar progress
                float progress = (float) timeLeft / 600000; // 10 minutes
                bountyBar.progress(Math.max(0, progress));
                
                // Rotate stand
                stand.setRotation(stand.getLocation().getYaw() + 5, 0);
                
                // Dragon aura particles
                for (int i = 0; i < 360; i += 30) {
                    double rad = Math.toRadians(i + ticks * 2);
                    double x = loc.getX() + 2 * Math.cos(rad);
                    double z = loc.getZ() + 2 * Math.sin(rad);
                    double y = loc.getY() + Math.sin(ticks * 0.1) * 0.5;
                    
                    Location particleLoc = new Location(world, x, y, z);
                    world.spawnParticle(Particle.DRAGON_BREATH, particleLoc, 2, 0.1, 0.1, 0.1, 0);
                    world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
                }
                
                // Flame ring
                for (int i = 0; i < 360; i += 20) {
                    double rad = Math.toRadians(i);
                    double x = loc.getX() + 3 * Math.cos(rad);
                    double z = loc.getZ() + 3 * Math.sin(rad);
                    
                    Location ringLoc = new Location(world, x, loc.getY(), z);
                    world.spawnParticle(Particle.FLAME, ringLoc, 1, 0, 0, 0, 0);
                }
                
                // Play intense music periodically
                if (ticks % 100 == 0) { // Every 5 seconds
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 0.8f);
                        p.playSound(p.getLocation(), Sound.MUSIC_DISC_11, 0.3f, 1.0f);
                    }
                }
                
                ticks += 2;
            }
        }.runTaskTimer(plugin, 0, 2);
    }
    
    /**
     * Claim bounty (when hunter kills bounty holder)
     */
    public void claimBounty(Player hunter) {
        if (activeBounty == null) return;
        
        ArmorStand stand = activeBounty.getStand();
        ItemStack katana = activeBounty.getKatana();
        
        if (hunter == null) {
            // Timer expired - give to nearest player
            hunter = getNearestPlayer(stand.getLocation());
        }
        
        if (hunter != null) {
            // Check if hunter is team member
            Team hunterTeam = plugin.getVoteManager().getTeam(hunter);
            
            if (hunterTeam != null && activeBounty.getOriginalMembers().contains(hunter)) {
                // Team member - auto receive
                hunter.getInventory().addItem(katana);
                hunter.sendMessage("§a§l✦ The katana returns to its kingdom! ✦");
            } else {
                // Check if hunter is a leader
                boolean isLeader = hunterTeam != null && hunterTeam.isLeader(hunter);
                
                if (isLeader) {
                    // Leader needs 3 kills
                    handleLeaderKill(hunter, katana);
                    return;
                } else {
                    // Normal player - gets katana immediately
                    hunter.getInventory().addItem(katana);
                    
                    // Broadcast
                    Bukkit.broadcast(Component.text("§6§l⚔ " + hunter.getName() + " §ehas claimed the bounty! ⚔"));
                    
                    // Epic effects
                    hunter.getWorld().strikeLightningEffect(hunter.getLocation());
                    hunter.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, hunter.getLocation().add(0, 2, 0), 100, 1, 1, 1, 0.5);
                }
            }
        }
        
        // Remove bounty
        stand.remove();
        activeBounty = null;
        
        // Hide boss bar
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.hideBossBar(bountyBar);
        }
    }
    
    /**
     * Handle leader killing bounty holder
     */
    private void handleLeaderKill(Player leader, ItemStack katana) {
        UUID leaderId = leader.getUniqueId();
        int kills = deathCount.getOrDefault(leaderId, 0) + 1;
        deathCount.put(leaderId, kills);
        
        leader.sendMessage("§c§l⚔ Leader Kill " + kills + "/3 ⚔");
        
        if (kills >= 3) {
            // Leader gets katana after 3 kills
            leader.getInventory().addItem(katana);
            deathCount.remove(leaderId);
            
            Bukkit.broadcast(Component.text("§6§l⚔ " + leader.getName() + " §ehas claimed the katana after 3 kills! ⚔"));
            
            // Remove bounty
            activeBounty.getStand().remove();
            activeBounty = null;
            
            // Hide boss bar
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.hideBossBar(bountyBar);
            }
        }
    }
    
    /**
     * Get nearest player to location
     */
    private Player getNearestPlayer(Location loc) {
        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        
        for (Entity e : loc.getWorld().getNearbyEntities(loc, 20, 20, 20)) {
            if (e instanceof Player p) {
                double distance = p.getLocation().distance(loc);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = p;
                }
            }
        }
        
        return nearest;
    }
    
    /**
     * Remove bound katana from player (on death)
     */
    private void removeBoundKatana(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && isBoundKatana(item) && getKatanaOwner(item).equals(player.getUniqueId())) {
                player.getInventory().remove(item);
                break;
            }
        }
    }
    
    /**
     * Check if player can create team
     */
    public boolean canCreateTeam(Player player) {
        UUID playerId = player.getUniqueId();
        Long cooldown = teamCreationCooldown.get(playerId);
        
        if (cooldown == null) return true;
        
        long timeLeft = (cooldown + COOLDOWN_24H) - System.currentTimeMillis();
        return timeLeft <= 0;
    }
    
    /**
     * Handle player respawn (give back katana if not bounty)
     */
    public void handlePlayerRespawn(Player player) {
        Team team = plugin.getVoteManager().getTeam(player);
        if (team == null) return;
        
        if (team.isLeader(player)) {
            int deaths = deathCount.getOrDefault(player.getUniqueId(), 0);
            
            if (deaths < MAX_DEATHS) {
                // Give back katana
                ItemStack katana = createBoundKatana(player, team.getKatanaType());
                player.getInventory().addItem(katana);
                
                player.sendMessage("§aYour katana has been returned to you!");
                player.sendMessage("§cDeaths: " + deaths + "/" + MAX_DEATHS);
            }
        }
    }
    
    // Helper methods
    private String getKatanaDisplayName(String type) {
        switch (type) {
            case "Storm Breaker": return "§b§l⚡ Storm Breaker ⚡";
            case "Flame Dragon": return "§c§l🔥 Flame Dragon 🔥";
            case "Shadow Dancer": return "§8§l🌑 Shadow Dancer 🌑";
            case "Wind Cutter": return "§a§l💨 Wind Cutter 💨";
            case "Earth Shaker": return "§6§l🌍 Earth Shaker 🌍";
            case "Frost Bite": return "§3§l❄️ Frost Bite ❄️";
            case "Void Walker": return "§5§l🌌 Void Walker 🌌";
            case "Sun Slash": return "§e§l☀️ Sun Slash ☀️";
            case "Nature's Fang": return "§2§l🌿 Nature's Fang 🌿";
            case "Stone Edge": return "§7§l⛰️ Stone Edge ⛰️";
            case "Wave Splitter": return "§9§l🌊 Wave Splitter 🌊";
            case "Blood Moon": return "§4§l🌙 Blood Moon 🌙";
            case "Soul Reaper": return "§d§l💀 Soul Reaper 💀";
            case "Thunder God": return "§6§l⚡ Thunder God ⚡";
            case "Celestial Blade": return "§b§l✨ Celestial Blade ✨";
            default: return "§fKatana";
        }
    }
    
    private ChatColor getKatanaColor(String type) {
        switch (type) {
            case "Storm Breaker": return ChatColor.AQUA;
            case "Flame Dragon": return ChatColor.RED;
            case "Shadow Dancer": return ChatColor.DARK_GRAY;
            case "Wind Cutter": return ChatColor.GREEN;
            case "Earth Shaker": return ChatColor.GOLD;
            case "Frost Bite": return ChatColor.DARK_AQUA;
            case "Void Walker": return ChatColor.DARK_PURPLE;
            case "Sun Slash": return ChatColor.YELLOW;
            case "Nature's Fang": return ChatColor.DARK_GREEN;
            case "Stone Edge": return ChatColor.GRAY;
            case "Wave Splitter": return ChatColor.BLUE;
            case "Blood Moon": return ChatColor.DARK_RED;
            case "Soul Reaper": return ChatColor.LIGHT_PURPLE;
            case "Thunder God": return ChatColor.GOLD;
            case "Celestial Blade": return ChatColor.AQUA;
            default: return ChatColor.WHITE;
        }
    }
    
    private int getKatanaIndex(String type) {
        String[] types = {
            "Storm Breaker", "Flame Dragon", "Shadow Dancer", "Wind Cutter",
            "Earth Shaker", "Frost Bite", "Void Walker", "Sun Slash",
            "Nature's Fang", "Stone Edge", "Wave Splitter", "Blood Moon",
            "Soul Reaper", "Thunder God", "Celestial Blade"
        };
        
        for (int i = 0; i < types.length; i++) {
            if (types[i].equals(type)) return i + 1;
        }
        return 1;
    }
    
    // Inner class for Bounty
    private class Bounty {
        private final ArmorStand stand;
        private final ItemStack katana;
        private final String katanaType;
        private final List<Player> originalMembers;
        private final long endTime;
        
        public Bounty(ArmorStand stand, ItemStack katana, String katanaType, List<Player> members, long endTime) {
            this.stand = stand;
            this.katana = katana;
            this.katanaType = katanaType;
            this.originalMembers = members;
            this.endTime = endTime;
        }
        
        public ArmorStand getStand() { return stand; }
        public ItemStack getKatana() { return katana; }
        public String getKatanaType() { return katanaType; }
        public List<Player> getOriginalMembers() { return originalMembers; }
        public long getEndTime() { return endTime; }
    }
}
