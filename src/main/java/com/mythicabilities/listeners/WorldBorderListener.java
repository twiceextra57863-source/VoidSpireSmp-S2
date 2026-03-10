package com.mythicabilities.listeners;

import com.mythicabilities.MythicAbilities;
import com.mythicabilities.admin.WorldBorderManager;
import com.mythicabilities.utils.TitleBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class WorldBorderListener implements Listener {
    
    private final MythicAbilities plugin;
    private final WorldBorderManager borderManager;
    
    public WorldBorderListener(MythicAbilities plugin) {
        this.plugin = plugin;
        this.borderManager = new WorldBorderManager(plugin);
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        if (borderManager.isOutsideBorder(player)) {
            // Show warning
            TitleBar.sendBorderWarning(player);
            
            // Teleport back if too far
            if (borderManager.isOutsideBorder(player)) {
                borderManager.teleportInsideBorder(player);
            }
        }
    }
}
