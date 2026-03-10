package com.mythicabilities.utils;

import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {
    
    private final Map<String, Map<UUID, Long>> cooldowns = new HashMap<>();
    
    public void setCooldown(Player player, String abilityName, int seconds) {
        cooldowns.computeIfAbsent(abilityName, k -> new HashMap<>())
                 .put(player.getUniqueId(), System.currentTimeMillis() + (seconds * 1000L));
    }
    
    public boolean isOnCooldown(Player player, String abilityName) {
        Map<UUID, Long> abilityCooldowns = cooldowns.get(abilityName);
        if (abilityCooldowns == null) return false;
        
        Long cooldown = abilityCooldowns.get(player.getUniqueId());
        return cooldown != null && cooldown > System.currentTimeMillis();
    }
    
    public long getRemainingCooldown(Player player, String abilityName) {
        Map<UUID, Long> abilityCooldowns = cooldowns.get(abilityName);
        if (abilityCooldowns == null) return 0;
        
        Long cooldown = abilityCooldowns.get(player.getUniqueId());
        if (cooldown == null) return 0;
        
        return (cooldown - System.currentTimeMillis()) / 1000;
    }
}
