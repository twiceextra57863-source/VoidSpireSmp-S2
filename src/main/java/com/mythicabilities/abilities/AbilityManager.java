package com.mythicabilities.abilities;

import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AbilityManager {
    
    private final Map<String, Ability> abilities = new HashMap<>();
    private final Map<UUID, String> playerAbilities = new HashMap<>();
    private final Map<UUID, Integer> comboCount = new HashMap<>();
    
    // Constructor with NO parameters
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
    }
    
    public String getPlayerAbility(Player player) {
        return playerAbilities.get(player.getUniqueId());
    }
    
    public void addCombo(Player player) {
        int count = comboCount.getOrDefault(player.getUniqueId(), 0) + 1;
        comboCount.put(player.getUniqueId(), count);
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
