package com.mythicabilities.katana;

import org.bukkit.entity.Player;

public interface KatanaAbility {
    
    void onLeftClick(Player player);
    
    void onRightClick(Player player);
    
    void onSneakLeft(Player player);
    
    void onSneakRight(Player player);
    
    void onPassive(Player player);
    
    int getCooldown(String abilityType);
    
    String getDescription();
}
