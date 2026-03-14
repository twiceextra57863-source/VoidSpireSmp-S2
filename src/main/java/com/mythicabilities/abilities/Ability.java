package com.mythicabilities.abilities;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public abstract class Ability {
    protected String name;
    protected String displayName;
    protected int cooldown;
    protected ItemStack icon;
    protected boolean autoTriggerOnCombo = true; // Naya feature
    
    public Ability(String name, String displayName, int cooldown, ItemStack icon) {
        this.name = name;
        this.displayName = displayName;
        this.cooldown = cooldown;
        this.icon = icon;
    }
    
    // Ab combo par automatically call hoga
    public abstract void onTrigger(Player player);
    
    // Old methods ab optional hain
    public void onRightClick(Player player) {} // Optional
    public void onLeftClick(Player player) {}  // Optional
    public void onCombo(Player player, Player target) {
        // Default behavior - auto trigger on 3 combos
        if (autoTriggerOnCombo) {
            onTrigger(player);
        }
    }
    
    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public int getCooldown() { return cooldown; }
    public ItemStack getIcon() { return icon.clone(); }
    public boolean isAutoTrigger() { return autoTriggerOnCombo; }
}
