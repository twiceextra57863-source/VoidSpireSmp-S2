package com.mythicabilities.voting;

import com.mythicabilities.MythicAbilities;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.*;

public class KatanaManager {
    
    private final MythicAbilities plugin;
    private final Map<UUID, Integer> deathCount = new HashMap<>();
    private final Map<UUID, Long> teamCreationCooldown = new HashMap<>();
    private final Map<UUID, Long> lastBountyCheck = new HashMap<>();
    private final Random random = new Random();
    
    // Constants
    private final long COOLDOWN_24H = 86400000; // 24 hours in milliseconds
    private final int MAX_DEATHS = 3;
    
    public KatanaManager(MythicAbilities plugin) {
        this.plugin = plugin;
    }
    
    // ============= DEATH COUNT METHODS =============
    
    public int getDeathCount(Player player) {
        return deathCount.getOrDefault(player.getUniqueId(), 0);
    }
    
    public void incrementDeathCount(Player player) {
        UUID playerId = player.getUniqueId();
        int current = deathCount.getOrDefault(playerId, 0);
        deathCount.put(playerId, current + 1);
    }
    
    public void resetDeathCount(Player player) {
        deathCount.remove(player.getUniqueId());
    }
    
    // ============= COOLDOWN METHODS =============
    
    public long getTeamCooldown(Player player) {
        UUID playerId = player.getUniqueId();
        Long cooldown = teamCreationCooldown.get(playerId);
        
        if (cooldown == null) return 0;
        
        long timeLeft = (cooldown + COOLDOWN_24H) - System.currentTimeMillis();
        return Math.max(0, timeLeft / 3600000); // Convert to hours
    }
    
    public void setTeamCooldown(Player player) {
        teamCreationCooldown.put(player.getUniqueId(), System.currentTimeMillis());
    }
    
    public boolean canCreateTeam(Player player) {
        UUID playerId = player.getUniqueId();
        Long cooldown = teamCreationCooldown.get(playerId);
        
        if (cooldown == null) return true;
        
        long timeLeft = (cooldown + COOLDOWN_24H) - System.currentTimeMillis();
        return timeLeft <= 0;
    }
    
    // ============= KATANA CREATION =============
    
    public ItemStack createBoundKatana(Player owner, String katanaType) {
        ItemStack katana = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = katana.getItemMeta();
        
        // Set display name based on katana type
        String displayName = getKatanaDisplayName(katanaType);
        meta.displayName(Component.text(displayName));
        
        // Set custom model data for texture
        meta.setCustomModelData(2000 + getKatanaIndex(katanaType));
        
        // Store owner data
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
            new NamespacedKey(plugin, "katana_bound"),
            PersistentDataType.BOOLEAN,
            true
        );
        
        // Add lore
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7A legendary katana bound to"));
        lore.add(Component.text("§7its owner. Cannot be dropped."));
        lore.add(Component.text(""));
        lore.add(Component.text("§7Bound to: §e" + owner.getName()));
        lore.add(Component.text("§7Type: " + getKatanaColor(katanaType) + katanaType));
        lore.add(Component.text(""));
        lore.add(Component.text("§c§l⚠ DEATHS: §f" + getDeathCount(owner) + "/3 ⚠"));
        lore.add(Component.text("§7On 3rd death, becomes bounty!"));
        
        meta.lore(lore);
        
        // Add enchantments
        meta.addEnchant(org.bukkit.enchantments.Enchantment.SHARPNESS, 5, true);
        meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 3, true);
        meta.setUnbreakable(true);
        
        katana.setItemMeta(meta);
        return katana;
    }
    
    // ============= KATANA VALIDATION =============
    
    public boolean isBoundKatana(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        
        return item.getItemMeta().getPersistentDataContainer().has(
            new NamespacedKey(plugin, "katana_bound"),
            PersistentDataType.BOOLEAN
        );
    }
    
    public UUID getKatanaOwner(ItemStack item) {
        if (!isBoundKatana(item)) return null;
        
        String ownerStr = item.getItemMeta().getPersistentDataContainer().get(
            new NamespacedKey(plugin, "katana_owner"),
            PersistentDataType.STRING
        );
        
        return ownerStr != null ? UUID.fromString(ownerStr) : null;
    }
    
    public String getKatanaType(ItemStack item) {
        if (!isBoundKatana(item)) return null;
        
        return item.getItemMeta().getPersistentDataContainer().get(
            new NamespacedKey(plugin, "katana_type"),
            PersistentDataType.STRING
        );
    }
    
    // ============= DEATH HANDLING =============
    
    public void handleKatanaDeath(Player player) {
        incrementDeathCount(player);
        int deaths = getDeathCount(player);
        
        if (deaths >= MAX_DEATHS) {
            // Create bounty
            createBounty(player);
            resetDeathCount(player);
            setTeamCooldown(player);
        }
    }
    
    public void handlePlayerRespawn(Player player) {
        Team team = plugin.getVoteManager().getTeam(player);
        if (team == null) return;
        
        if (team.isLeader(player)) {
            int deaths = getDeathCount(player);
            if (deaths < MAX_DEATHS) {
                ItemStack katana = createBoundKatana(player, team.getKatanaType());
                player.getInventory().addItem(katana);
                player.sendMessage("§aYour katana has been returned!");
                player.sendMessage("§cDeaths: " + deaths + "/" + MAX_DEATHS);
            }
        }
    }
    
    // ============= BOUNTY SYSTEM =============
    
    private void createBounty(Player player) {
        Location loc = player.getLocation().clone().add(0, 2, 0);
        
        // Create bounty armor stand
        ArmorStand bountyStand = (ArmorStand) player.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
        bountyStand.setGravity(false);
        bountyStand.setInvulnerable(true);
        bountyStand.setVisible(false);
        bountyStand.setMarker(true);
        bountyStand.setSmall(true);
        
        // Create bounty katana
        Team team = plugin.getVoteManager().getTeam(player);
        String katanaType = team != null ? team.getKatanaType() : "Storm Breaker";
        ItemStack bountyKatana = createBoundKatana(player, katanaType);
        bountyStand.getEquipment().setHelmet(bountyKatana);
        
        // Broadcast bounty
        Bukkit.broadcast(Component.text("§c§l⚔ BOUNTY CREATED! ⚔"));
        Bukkit.broadcast(Component.text("§e" + player.getName() + "'s katana is now up for grabs!"));
        
        // Start bounty effects
        startBountyEffects(bountyStand, player);
    }
    
    private void startBountyEffects(ArmorStand stand, Player owner) {
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (stand.isDead() || ticks >= 1200) { // 60 seconds
                    if (!stand.isDead()) stand.remove();
                    this.cancel();
                    return;
                }
                
                // Rotate stand
                stand.setRotation(stand.getLocation().getYaw() + 10, 0);
                
                // Particle effects
                Location loc = stand.getLocation();
                World world = loc.getWorld();
                
                // Spiral particles
                for (int i = 0; i < 360; i += 30) {
                    double rad = Math.toRadians(i + ticks * 5);
                    double x = loc.getX() + 2 * Math.cos(rad);
                    double z = loc.getZ() + 2 * Math.sin(rad);
                    double y = loc.getY() + Math.sin(ticks * 0.2) * 0.5;
                    
                    Location particleLoc = new Location(world, x, y, z);
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, particleLoc, 2, 0, 0, 0, 0);
                    world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
                }
                
                ticks += 2;
            }
        }.runTaskTimer(plugin, 0, 2);
    }
    
    // ============= HELPER METHODS =============
    
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
}
