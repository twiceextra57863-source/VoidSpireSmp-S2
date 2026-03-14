package com.mythicabilities.utils;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
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
                for (Player player : Bukkit.getOnlinePlayers()) {
                    String abilityName = plugin.getAbilityManager().getPlayerAbility(player);
                    if (abilityName == null) continue;
                    
                    if (isOnCooldown(player, abilityName)) {
                        long remaining = getRemainingCooldown(player, abilityName);
                        int totalCooldown = getTotalCooldown(abilityName);
                        int progress = (int) ((remaining * 20) / totalCooldown);
                        
                        // OPTION 1: Diamond Luxe (Premium Look)
                        String bar = createDiamondBar(progress, remaining);
                        
                        // OPTION 2: Gradient Circles (Modern Look)
                        // String bar = createGradientCircleBar(progress, remaining);
                        
                        // OPTION 3: Block Style (Classic Look)
                        // String bar = createBlockBar(progress, remaining);
                        
                        player.sendActionBar(Component.text("§d" + getAbilityIcon(abilityName) + " §7" + bar + " §e" + remaining + "s"));
                        
                    } else {
                        // Show ready status
                        player.sendActionBar(Component.text("§a" + getAbilityIcon(abilityName) + " §7[§a■■■■■■■■■■■■■■■■■■■■§7] §aREADY!"));
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 20);
    }
    
    private String createDiamondBar(int progress, long remaining) {
        StringBuilder bar = new StringBuilder("§8[");
        for (int i = 0; i < 20; i++) {
            if (i < progress) {
                // Gradient diamonds based on remaining time
                if (remaining > 20) bar.append("§a♦");
                else if (remaining > 10) bar.append("§e♦");
                else if (remaining > 5) bar.append("§6♦");
                else bar.append("§c♦");
            } else {
                bar.append("§8◇");
            }
        }
        bar.append("§8]");
        return bar.toString();
    }
    
    private String createGradientCircleBar(int progress, long remaining) {
        StringBuilder bar = new StringBuilder("§8[");
        String[] colors = {"§a", "§b", "§d", "§c"}; // Green → Aqua → Pink → Red
        for (int i = 0; i < 20; i++) {
            if (i < progress) {
                int colorIndex = Math.min(colors.length - 1, (i * colors.length) / 20);
                bar.append(colors[colorIndex] + "●");
            } else {
                bar.append("§7○");
            }
        }
        bar.append("§8]");
        return bar.toString();
    }
    
    private String createBlockBar(int progress, long remaining) {
        StringBuilder bar = new StringBuilder("§8[");
        for (int i = 0; i < 20; i++) {
            if (i < progress) {
                if (remaining > 20) bar.append("§a█");
                else if (remaining > 10) bar.append("§e█");
                else if (remaining > 5) bar.append("§6█");
                else bar.append("§c█");
            } else {
                bar.append("§7░");
            }
        }
        bar.append("§8]");
        return bar.toString();
    }
    
    private int getTotalCooldown(String abilityName) {
        // Return cooldown based on ability
        switch (abilityName) {
            case "inferno_touch": return 30;
            case "frost_walker": return 20;
            case "void_walker": return 25;
            case "mandela_effect": return 40;
            case "natures_wrath": return 30;
            case "aerial_mace": return 25;
            case "kaguras_umbrella": return 32;
            case "suyous_transcendence": return 35;
            case "lings_shadow": return 30;
            case "first_of_stone": return 35;
            default: return 30;
        }
    }
    
    private String getAbilityIcon(String abilityName) {
        // Return icon for each ability
        switch (abilityName) {
            case "inferno_touch": return "§c♨"; // Fire icon
            case "frost_walker": return "§b❄"; // Snowflake
            case "void_walker": return "§5👁"; // Eye
            case "mandela_effect": return "§d🌀"; // Cyclone
            case "natures_wrath": return "§2🌿"; // Leaf
            case "aerial_mace": return "§e⚡"; // Lightning
            case "kaguras_umbrella": return "§d☂"; // Umbrella
            case "suyous_transcendence": return "§6⚔"; // Crossed swords
            case "lings_shadow": return "§5🗡"; // Dagger
            case "first_of_stone": return "§7⛰"; // Mountain
            default: return "§f✦";
        }
    }
}
