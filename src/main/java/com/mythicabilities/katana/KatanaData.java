package com.mythicabilities.katana;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.*;

public class KatanaData {
    
    private static final Map<UUID, KatanaInfo> playerKatanas = new HashMap<>();
    private static final List<String> KATANA_NAMES = Arrays.asList(
        "Storm Breaker", "Flame Dragon", "Shadow Dancer", "Wind Cutter",
        "Earth Shaker", "Frost Bite", "Void Walker", "Sun Slash",
        "Nature's Fang", "Stone Edge", "Wave Splitter", "Blood Moon",
        "Soul Reaper", "Thunder God", "Celestial Blade", "Dragon's Wrath"
    );
    
    private static final Map<String, String> KATANA_DISPLAY = new HashMap<>();
    private static final Map<String, Integer> KATANA_MODELS = new HashMap<>();
    
    static {
        // Initialize display names
        KATANA_DISPLAY.put("Storm Breaker", "§b§l⚡ Storm Breaker");
        KATANA_DISPLAY.put("Flame Dragon", "§c§l🔥 Flame Dragon");
        KATANA_DISPLAY.put("Shadow Dancer", "§8§l🌑 Shadow Dancer");
        KATANA_DISPLAY.put("Wind Cutter", "§a§l💨 Wind Cutter");
        KATANA_DISPLAY.put("Earth Shaker", "§6§l🌍 Earth Shaker");
        KATANA_DISPLAY.put("Frost Bite", "§3§l❄️ Frost Bite");
        KATANA_DISPLAY.put("Void Walker", "§5§l🌌 Void Walker");
        KATANA_DISPLAY.put("Sun Slash", "§e§l☀️ Sun Slash");
        KATANA_DISPLAY.put("Nature's Fang", "§2§l🌿 Nature's Fang");
        KATANA_DISPLAY.put("Stone Edge", "§7§l⛰️ Stone Edge");
        KATANA_DISPLAY.put("Wave Splitter", "§9§l🌊 Wave Splitter");
        KATANA_DISPLAY.put("Blood Moon", "§4§l🌙 Blood Moon");
        KATANA_DISPLAY.put("Soul Reaper", "§d§l💀 Soul Reaper");
        KATANA_DISPLAY.put("Thunder God", "§6§l⚡ Thunder God");
        KATANA_DISPLAY.put("Celestial Blade", "§b§l✨ Celestial Blade");
        KATANA_DISPLAY.put("Dragon's Wrath", "§c§l🐉 Dragon's Wrath");
        
        // Initialize model data
        for (int i = 0; i < KATANA_NAMES.size(); i++) {
            KATANA_MODELS.put(KATANA_NAMES.get(i), 2000 + i);
        }
    }
    
    public static class KatanaInfo {
        public final String name;
        public final String displayName;
        public final int modelData;
        public final long obtainedTime;
        
        public KatanaInfo(String name) {
            this.name = name;
            this.displayName = KATANA_DISPLAY.getOrDefault(name, "§fUnknown Katana");
            this.modelData = KATANA_MODELS.getOrDefault(name, 2000);
            this.obtainedTime = System.currentTimeMillis();
        }
    }
    
    public static void setPlayerKatana(Player player, String katanaName) {
        playerKatanas.put(player.getUniqueId(), new KatanaInfo(katanaName));
    }
    
    public static KatanaInfo getPlayerKatana(Player player) {
        return playerKatanas.get(player.getUniqueId());
    }
    
    public static boolean hasKatana(Player player) {
        return playerKatanas.containsKey(player.getUniqueId());
    }
    
    public static void removePlayerKatana(Player player) {
        playerKatanas.remove(player.getUniqueId());
    }
    
    public static List<String> getAllKatanaNames() {
        return new ArrayList<>(KATANA_NAMES);
    }
    
    public static String getDisplayName(String katanaName) {
        return KATANA_DISPLAY.getOrDefault(katanaName, "§fUnknown Katana");
    }
    
    public static int getModelData(String katanaName) {
        return KATANA_MODELS.getOrDefault(katanaName, 2000);
    }
    
    public static boolean isValidKatana(String katanaName) {
        return KATANA_NAMES.contains(katanaName);
    }
}
