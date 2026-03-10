package com.mythicabilities.admin;

import com.mythicabilities.MythicAbilities;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SMPManager {
    
    private final MythicAbilities plugin;
    private final Map<String, Boolean> features = new HashMap<>();
    private Location spawnLocation;
    
    public SMPManager(MythicAbilities plugin) {
        this.plugin = plugin;
        loadDefaultFeatures();
    }
    
    private void loadDefaultFeatures() {
        features.put("pvp", false);
        features.put("mob-spawning", true);
        features.put("fire-spread", false);
        features.put("daylight-cycle", true);
        features.put("weather-cycle", true);
        features.put("keep-inventory", false);
        features.put("keep-experience", true);
        features.put("natural-regeneration", true);
        features.put("entity-drops", true);
        features.put("block-drops", true);
    }
    
    public void teleportAllToSpawn() {
        World world = Bukkit.getWorlds().get(0);
        spawnLocation = world.getSpawnLocation();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.teleport(spawnLocation);
        }
    }
    
    public void teleportToBorder(Player player) {
        // Teleport player to a random location within border
        World world = player.getWorld();
        double borderSize = world.getWorldBorder().getSize() / 2;
        
        Location loc = new Location(
            world,
            spawnLocation.getX() + (Math.random() - 0.5) * borderSize,
            world.getHighestBlockYAt(spawnLocation),
            spawnLocation.getZ() + (Math.random() - 0.5) * borderSize
        );
        
        player.teleport(loc);
    }
    
    public void toggleFeature(String feature, boolean value) {
        if (features.containsKey(feature)) {
            features.put(feature, value);
            applyFeatureChanges(feature, value);
        }
    }
    
    private void applyFeatureChanges(String feature, boolean value) {
        World world = Bukkit.getWorlds().get(0);
        
        switch (feature) {
            case "pvp":
                world.setPVP(value);
                break;
            case "mob-spawning":
                world.setMonsterSpawnLimit(value ? 70 : 0);
                world.setAnimalSpawnLimit(value ? 15 : 0);
                break;
            case "daylight-cycle":
                world.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, value);
                break;
            case "weather-cycle":
                world.setGameRule(org.bukkit.GameRule.DO_WEATHER_CYCLE, value);
                break;
            case "keep-inventory":
                world.setGameRule(org.bukkit.GameRule.KEEP_INVENTORY, value);
                break;
            case "natural-regeneration":
                world.setGameRule(org.bukkit.GameRule.NATURAL_REGENERATION, value);
                break;
        }
    }
    
    public boolean isFeatureEnabled(String feature) {
        return features.getOrDefault(feature, false);
    }
    
    public Map<String, Boolean> getAllFeatures() {
        return features;
    }
}
