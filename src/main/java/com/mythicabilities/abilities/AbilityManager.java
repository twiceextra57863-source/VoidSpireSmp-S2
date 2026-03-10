package com.mythicabilities.abilities;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AbilityManager {
    
    private final Map<String, Ability> abilities = new HashMap<>();
    private final Map<UUID, String> playerAbilities = new HashMap<>();
    private final Map<UUID, Integer> comboCount = new HashMap<>();
    
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

abstract class Ability {
    protected String name;
    protected String displayName;
    protected int cooldown;
    protected ItemStack icon;
    
    public Ability(String name, String displayName, int cooldown, ItemStack icon) {
        this.name = name;
        this.displayName = displayName;
        this.cooldown = cooldown;
        this.icon = icon;
    }
    
    public abstract void onRightClick(Player player);
    public abstract void onLeftClick(Player player);
    public abstract void onCombo(Player player, Player target);
    
    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public int getCooldown() { return cooldown; }
    public ItemStack getIcon() { return icon.clone(); }
}
