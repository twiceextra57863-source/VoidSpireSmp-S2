package com.mythicabilities.katana;

import com.mythicabilities.MythicAbilities;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class KatanaManager {
    
    private final MythicAbilities plugin;
    private final Map<String, Integer> katanaModelData = new HashMap<>();
    private final Map<String, String> katanaDisplayNames = new HashMap<>();
    private final Random random = new Random();
    
    // All 16 katanas
    public static final String[] KATANA_NAMES = {
        "Storm Breaker", "Flame Dragon", "Shadow Dancer", "Wind Cutter",
        "Earth Shaker", "Frost Bite", "Void Walker", "Sun Slash",
        "Nature's Fang", "Stone Edge", "Wave Splitter", "Blood Moon",
        "Soul Reaper", "Thunder God", "Celestial Blade", "Dragon's Wrath"
    };
    
    public KatanaManager(MythicAbilities plugin) {
        this.plugin = plugin;
        initializeKatanaData();
    }
    
    private void initializeKatanaData() {
        // Display names
        katanaDisplayNames.put("Storm Breaker", "§b§l⚡ Storm Breaker ⚡");
        katanaDisplayNames.put("Flame Dragon", "§c§l🔥 Flame Dragon 🔥");
        katanaDisplayNames.put("Shadow Dancer", "§8§l🌑 Shadow Dancer 🌑");
        katanaDisplayNames.put("Wind Cutter", "§a§l💨 Wind Cutter 💨");
        katanaDisplayNames.put("Earth Shaker", "§6§l🌍 Earth Shaker 🌍");
        katanaDisplayNames.put("Frost Bite", "§3§l❄️ Frost Bite ❄️");
        katanaDisplayNames.put("Void Walker", "§5§l🌌 Void Walker 🌌");
        katanaDisplayNames.put("Sun Slash", "§e§l☀️ Sun Slash ☀️");
        katanaDisplayNames.put("Nature's Fang", "§2§l🌿 Nature's Fang 🌿");
        katanaDisplayNames.put("Stone Edge", "§7§l⛰️ Stone Edge ⛰️");
        katanaDisplayNames.put("Wave Splitter", "§9§l🌊 Wave Splitter 🌊");
        katanaDisplayNames.put("Blood Moon", "§4§l🌙 Blood Moon 🌙");
        katanaDisplayNames.put("Soul Reaper", "§d§l💀 Soul Reaper 💀");
        katanaDisplayNames.put("Thunder God", "§6§l⚡ Thunder God ⚡");
        katanaDisplayNames.put("Celestial Blade", "§b§l✨ Celestial Blade ✨");
        katanaDisplayNames.put("Dragon's Wrath", "§c§l🐉 Dragon's Wrath 🐉");
        
        // Model data (2000-2015)
        for (int i = 0; i < KATANA_NAMES.length; i++) {
            katanaModelData.put(KATANA_NAMES[i], 2000 + i);
        }
    }
    
    // ============= KATANA CREATION =============
    
    public ItemStack createKatana(String katanaName, Player owner) {
        if (!isValidKatana(katanaName)) {
            return null;
        }
        
        ItemStack katana = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = katana.getItemMeta();
        
        // Set display name
        meta.displayName(Component.text(getDisplayName(katanaName)));
        
        // Set custom model data
        meta.setCustomModelData(getModelData(katanaName));
        
        // Store metadata
        meta.getPersistentDataContainer().set(
            new NamespacedKey(plugin, "katana_name"),
            PersistentDataType.STRING,
            katanaName
        );
        
        if (owner != null) {
            meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "katana_owner"),
                PersistentDataType.STRING,
                owner.getUniqueId().toString()
            );
            meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "katana_bound"),
                PersistentDataType.BOOLEAN,
                true
            );
        }
        
        // Add lore
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7A legendary katana with"));
        lore.add(Component.text("§7immense power..."));
        lore.add(Component.text(""));
        
        if (owner != null) {
            lore.add(Component.text("§7Bound to: §e" + owner.getName()));
            lore.add(Component.text("§c§l⚠ BOUND ITEM ⚠"));
            lore.add(Component.text("§7Cannot be dropped or stored"));
        } else {
            lore.add(Component.text("§7Unbound - Can be given"));
        }
        
        lore.add(Component.text(""));
        lore.add(Component.text("§c§l⚔ LEGENDARY KATANA ⚔"));
        
        meta.lore(lore);
        
        // Enchantments
        meta.addEnchant(org.bukkit.enchantments.Enchantment.SHARPNESS, 7, true);
        meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 5, true);
        meta.addEnchant(org.bukkit.enchantments.Enchantment.MENDING, 1, true);
        meta.setUnbreakable(true);
        
        katana.setItemMeta(meta);
        return katana;
    }
    
    public ItemStack createRandomKatana(Player owner) {
        String randomName = KATANA_NAMES[random.nextInt(KATANA_NAMES.length)];
        return createKatana(randomName, owner);
    }
    
    // ============= KATANA VALIDATION =============
    
    public boolean isValidKatana(String name) {
        for (String katana : KATANA_NAMES) {
            if (katana.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }
    
    public String getKatanaName(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        
        return item.getItemMeta().getPersistentDataContainer().get(
            new NamespacedKey(plugin, "katana_name"),
            PersistentDataType.STRING
        );
    }
    
    public UUID getKatanaOwner(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        
        String ownerStr = item.getItemMeta().getPersistentDataContainer().get(
            new NamespacedKey(plugin, "katana_owner"),
            PersistentDataType.STRING
        );
        
        return ownerStr != null ? UUID.fromString(ownerStr) : null;
    }
    
    public boolean isBoundKatana(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        
        Boolean bound = item.getItemMeta().getPersistentDataContainer().get(
            new NamespacedKey(plugin, "katana_bound"),
            PersistentDataType.BOOLEAN
        );
        
        return bound != null && bound;
    }
    
    // ============= GIVING METHODS =============
    
    public boolean giveKatanaToPlayer(Player target, String katanaName) {
        ItemStack katana = createKatana(katanaName, target);
        if (katana == null) return false;
        
        if (target.getInventory().firstEmpty() == -1) {
            target.getWorld().dropItem(target.getLocation(), katana);
            target.sendMessage("§cInventory full! Katana dropped on ground.");
        } else {
            target.getInventory().addItem(katana);
        }
        
        target.sendMessage("§a§l✦ You received: " + getDisplayName(katanaName));
        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
        
        return true;
    }
    
    public void giveRandomKatanaToPlayer(Player target) {
        String randomName = KATANA_NAMES[random.nextInt(KATANA_NAMES.length)];
        giveKatanaToPlayer(target, randomName);
    }
    
    public void giveKatanaToAll(String katanaName) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            giveKatanaToPlayer(p, katanaName);
        }
        Bukkit.broadcast(Component.text("§a§l✦ Everyone received: " + getDisplayName(katanaName)));
    }
    
    public void giveRandomKatanaToAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            giveRandomKatanaToPlayer(p);
        }
        Bukkit.broadcast(Component.text("§a§l✦ Everyone received a random katana!"));
    }
    
    // ============= REMOVAL METHODS =============
    
    public void removeKatanaFromPlayer(Player target) {
        for (ItemStack item : target.getInventory().getContents()) {
            if (item != null && isBoundKatana(item)) {
                target.getInventory().remove(item);
            }
        }
    }
    
    public void removeAllKatanas() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            removeKatanaFromPlayer(p);
        }
    }
    
    // ============= INFO METHODS =============
    
    public String getDisplayName(String katanaName) {
        return katanaDisplayNames.getOrDefault(katanaName, "§fUnknown Katana");
    }
    
    public int getModelData(String katanaName) {
        return katanaModelData.getOrDefault(katanaName, 2000);
    }
    
    public List<String> getAllKatanaNames() {
        return Arrays.asList(KATANA_NAMES);
    }
    
    public String[] getKatanaNames() {
        return KATANA_NAMES;
    }
    
    public int getKatanaCount() {
        return KATANA_NAMES.length;
    }
}
