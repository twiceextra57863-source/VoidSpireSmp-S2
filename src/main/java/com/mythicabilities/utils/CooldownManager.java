package com.mythicabilities.utils;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import com.mythicabilities.MythicAbilities;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {
    
    private final Map<String, Map<UUID, Long>> cooldowns = new HashMap<>();
    private final MythicAbilities plugin;
    
    public CooldownManager(MythicAbilities plugin) {
        this.plugin = plugin;
        startCooldownDisplay();
    }
    
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
    
    private void startCooldownDisplay() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Update cooldown display for all players
                org.bukkit.Bukkit.getOnlinePlayers().forEach(player -> {
                    String abilityName = plugin.getAbilityManager().getPlayerAbility(player);
                    if (abilityName != null && isOnCooldown(player, abilityName)) {
                        long remaining = getRemainingCooldown(player, abilityName);
                        
                        // Create cooldown bar
                        int total = 30; // Max cooldown (adjust based on ability)
                        int progress = (int) ((remaining * 20) / total);
                        
                        StringBuilder bar = new StringBuilder("§8[");
                        for (int i = 0; i < 20; i++) {
                            if (i < progress) {
                                bar.append("§c█");
                            } else {
                                bar.append("§7█");
                            }
                        }
                        bar.append("§8]");
                        
                        // Send action bar
                        player.sendActionBar(Component.text(
                            "§cCooldown: §e" + remaining + "s " + bar.toString()
                        ));
                    }
                });
            }
        }.runTaskTimer(plugin, 0, 20); // Update every second
    }
}
