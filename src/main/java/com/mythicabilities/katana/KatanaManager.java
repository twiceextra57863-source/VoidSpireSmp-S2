package com.mythicabilities.katana;

import com.mythicabilities.MythicAbilities;
import com.mythicabilities.katana.katanas.*;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class KatanaManager {
    
    private final MythicAbilities plugin;
    private final Map<String, KatanaAbility> abilities = new HashMap<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Random random = new Random();
    
    public KatanaManager(MythicAbilities plugin) {
        this.plugin = plugin;
        registerAllAbilities();
    }
    
    private void registerAllAbilities() {
        abilities.put("Storm Breaker", new StormBreaker(plugin));
        abilities.put("Flame Dragon", new FlameDragon(plugin));
        abilities.put("Shadow Dancer", new ShadowDancer(plugin));
        abilities.put("Wind Cutter", new WindCutter(plugin));
        abilities.put("Earth Shaker", new EarthShaker(plugin));
        abilities.put("Frost Bite", new FrostBite(plugin));
        abilities.put("Void Walker", new VoidWalkerKatana(plugin));
        abilities.put("Sun Slash", new SunSlash(plugin));
        abilities.put("Nature's Fang", new NaturesFang(plugin));
        abilities.put("Stone Edge", new StoneEdge(plugin));
        abilities.put("Wave Splitter", new WaveSplitter(plugin));
        abilities.put("Blood Moon", new BloodMoon(plugin));
        abilities.put("Soul Reaper", new SoulReaper(plugin));
        abilities.put("Thunder God", new ThunderGod(plugin));
        abilities.put("Celestial Blade", new CelestialBlade(plugin));
        abilities.put("Dragon's Wrath", new DragonsWrath(plugin));
    }
    
    // ============= KATANA CREATION =============
    
    public ItemStack createKatana(String katanaName, Player owner) {
        if (!KatanaData.isValidKatana(katanaName)) {
            return null;
        }
        
        ItemStack katana = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = katana.getItemMeta();
        
        meta.displayName(Component.text(KatanaData.getDisplayName(katanaName)));
        meta.setCustomModelData(KatanaData.getModelData(katanaName));
        
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
        }
        
        meta.getPersistentDataContainer().set(
            new NamespacedKey(plugin, "katana_bound"),
            PersistentDataType.BOOLEAN,
            owner != null
        );
        
        // Add lore
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("§7A legendary katana with"));
        lore.add(Component.text("§7immense power..."));
        lore.add(Component.text(""));
        
        if (owner != null) {
            lore.add(Component.text("§7Bound to: §e" + owner.getName()));
        } else {
            lore.add(Component.text("§7Unbound - Can be given"));
        }
        
        lore.add(Component.text(""));
        lore.add(Component.text("§c§l⚔ LEGENDARY KATANA ⚔"));
        
        meta.lore(lore);
        
        // Enchantments
        meta.addEnchant(org.bukkit.enchantments.Enchantment.SHARPNESS, 5, true);
        meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 3, true);
        meta.setUnbreakable(true);
        
        katana.setItemMeta(meta);
        
        // Store in data
        if (owner != null) {
            KatanaData.setPlayerKatana(owner, katanaName);
        }
        
        return katana;
    }
    
    public ItemStack createRandomKatana(Player owner) {
        List<String> names = KatanaData.getAllKatanaNames();
        String randomName = names.get(random.nextInt(names.size()));
        return createKatana(randomName, owner);
    }
    
    // ============= KATANA GIVING METHODS =============
    
    public boolean giveKatanaToPlayer(Player target, String katanaName) {
        ItemStack katana = createKatana(katanaName, target);
        if (katana == null) return false;
        
        // Clear inventory if full
        if (target.getInventory().firstEmpty() == -1) {
            target.getWorld().dropItem(target.getLocation(), katana);
            target.sendMessage("§cInventory full! Katana dropped on ground.");
        } else {
            target.getInventory().addItem(katana);
        }
        
        target.sendMessage("§a§l✦ You received: " + KatanaData.getDisplayName(katanaName));
        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
        
        return true;
    }
    
    public void giveRandomKatanaToPlayer(Player target) {
        String randomName = KatanaData.getAllKatanaNames()
            .get(random.nextInt(KatanaData.getAllKatanaNames().size()));
        giveKatanaToPlayer(target, randomName);
    }
    
    public void giveKatanaToAll(String katanaName) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            giveKatanaToPlayer(p, katanaName);
        }
        Bukkit.broadcast(Component.text("§a§l✦ Everyone received: " + KatanaData.getDisplayName(katanaName)));
    }
    
    public void giveRandomKatanaToAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            giveRandomKatanaToPlayer(p);
        }
        Bukkit.broadcast(Component.text("§a§l✦ Everyone received a random katana!"));
    }
    
    // ============= KATANA INFO =============
    
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
    
    // ============= ABILITY HANDLING =============
    
    public void triggerAbility(Player player, String katanaName, String abilityType) {
        KatanaAbility ability = abilities.get(katanaName);
        if (ability == null) return;
        
        // Check cooldown
        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        long lastUse = cooldowns.getOrDefault(playerId, 0L);
        
        int cooldownTime = ability.getCooldown(abilityType);
        if (now - lastUse < cooldownTime * 1000L) {
            long remaining = (cooldownTime * 1000L - (now - lastUse)) / 1000;
            player.sendActionBar(Component.text("§c⏳ " + abilityType + " cooldown: " + remaining + "s"));
            return;
        }
        
        cooldowns.put(playerId, now);
        
        // Trigger appropriate ability
        switch (abilityType.toLowerCase()) {
            case "left":
                ability.onLeftClick(player);
                break;
            case "right":
                ability.onRightClick(player);
                break;
            case "sneakleft":
                ability.onSneakLeft(player);
                break;
            case "sneakright":
                ability.onSneakRight(player);
                break;
            case "passive":
                ability.onPassive(player);
                break;
        }
    }
            }
