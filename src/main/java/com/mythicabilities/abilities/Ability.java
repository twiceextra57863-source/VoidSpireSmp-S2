package com.mythicabilities.abilities;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public abstract class Ability {
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
