package com.mythicabilities.abilities;

import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AbilityManager {
    
    private final Map<String, Ability> abilities = new HashMap<>();
    private final Map<UUID, String> playerAbilities = new HashMap<>();
    private final Map<UUID, Integer> comboCount = new HashMap<>();
    
    public AbilityManager() {
        // Empty constructor
    }
    
    public void registerAbility(Ability ability) {
        abilities.put(ability.getName(), ability);
    }
    
    public Ability getAbility(String name) {
        return abilities.get(name);
    }
    
    public void setPlayerAbility(Player player, String abilityName) {
        playerAbilities.put(player.getUniqueId(), abilityName);
        // Reset combo when new ability assigned
        comboCount.remove(player.getUniqueId());
    }
    
    public String getPlayerAbility(Player player) {
        return playerAbilities.get(player.getUniqueId());
    }
    
    public void addCombo(Player player) {
        UUID playerId = player.getUniqueId();
        int currentCombo = comboCount.getOrDefault(playerId, 0);
        comboCount.put(playerId, currentCombo + 1);
        
        // Debug - remove in production
        player.sendMessage("§8[Debug] Combo: " + (currentCombo + 1));
    }
    
    public int getCombo(Player player) {
        return comboCount.getOrDefault(player.getUniqueId(), 0);
    }
    
    public void resetCombo(Player player) {
        comboCount.remove(player.getUniqueId());
    }
    
    public Map<String, Ability> getAllAbilities() {
        return abilities;
    }
}
