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
        if (cooldown == null) return false;
        
        return cooldown > System.currentTimeMillis();
    }
    
    public long getRemainingCooldown(Player player, String abilityName) {
        Map<UUID, Long> abilityCooldowns = cooldowns.get(abilityName);
        if (abilityCooldowns == null) return 0;
        
        Long cooldown = abilityCooldowns.get(player.getUniqueId());
        if (cooldown == null) return 0;
        
        return (cooldown - System.currentTimeMillis()) / 1000;
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
            case "inferno_touch": return "§c♨";
            case "frost_walker": return "§b❄";
            case "void_walker": return "§5👁";
            case "mandela_effect": return "§d🌀";
            case "natures_wrath": return "§2🌿";
            case "aerial_mace": return "§e⚡";
            case "kaguras_umbrella": return "§d☂";
            case "suyous_transcendence": return "§6⚔";
            case "lings_shadow": return "§5🗡";
            case "first_of_stone": return "§7⛰";
            default: return "§f✦";
        }
    }
    
    private void startCooldownDisplay() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    String abilityName = plugin.getAbilityManager().getPlayerAbility(player);
                    if (abilityName == null) {
                        // Show combo if available
                        int combo = plugin.getAbilityManager().getCombo(player);
                        if (combo > 0) {
                            player.sendActionBar(Component.text("§6Combo: §e" + combo + "§6/3"));
                        }
                        continue;
                    }
                    
                    if (isOnCooldown(player, abilityName)) {
                        long remaining = getRemainingCooldown(player, abilityName);
                        int totalCooldown = getTotalCooldown(abilityName);
                        int progress = (int) ((remaining * 20) / totalCooldown);
                        
                        // Create diamond bar
                        StringBuilder bar = new StringBuilder("§8[");
                        for (int i = 0; i < 20; i++) {
                            if (i < progress) {
                                if (remaining > totalCooldown/2) bar.append("§a♦");
                                else if (remaining > totalCooldown/4) bar.append("§e♦");
                                else bar.append("§c♦");
                            } else {
                                bar.append("§8◇");
                            }
                        }
                        bar.append("§8]");
                        
                        player.sendActionBar(Component.text(
                            getAbilityIcon(abilityName) + " §7" + bar.toString() + " §e" + remaining + "s"
                        ));
                    } else {
                        // Show ready status
                        int combo = plugin.getAbilityManager().getCombo(player);
                        if (combo > 0) {
                            player.sendActionBar(Component.text(
                                getAbilityIcon(abilityName) + " §7[§a■■■■■■■■■■■■■■■■■■■■§7] §aREADY! §6Combo: §e" + combo + "§6/3"
                            ));
                        } else {
                            player.sendActionBar(Component.text(
                                getAbilityIcon(abilityName) + " §7[§a■■■■■■■■■■■■■■■■■■■■§7] §aREADY!"
                            ));
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0, 20);
    }
}
