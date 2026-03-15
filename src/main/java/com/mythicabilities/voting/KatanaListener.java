package com.mythicabilities.voting;

import com.mythicabilities.MythicAbilities;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

public class KatanaListener implements Listener {
    
    private final MythicAbilities plugin;
    private final KatanaManager katanaManager;
    
    public KatanaListener(MythicAbilities plugin) {
        this.plugin = plugin;
        this.katanaManager = plugin.getKatanaManager();
    }
    
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        
        if (katanaManager.isBoundKatana(item)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§cYou cannot drop a bound katana!");
        }
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        
        // Check if trying to move bound katana to storage
        if (isStorageInventory(event.getView().getType())) {
            if (katanaManager.isBoundKatana(current) || katanaManager.isBoundKatana(cursor)) {
                event.setCancelled(true);
                event.getWhoClicked().sendMessage("§cYou cannot store a bound katana!");
            }
        }
    }
    
    private boolean isStorageInventory(InventoryType type) {
        return type == InventoryType.CHEST ||
               type == InventoryType.BARREL ||
               type == InventoryType.SHULKER_BOX ||
               type == InventoryType.ENDER_CHEST ||
               type == InventoryType.HOPPER ||
               type == InventoryType.DISPENSER ||
               type == InventoryType.DROPPER;
    }
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        // Remove bound katanas from drops
        event.getDrops().removeIf(item -> katanaManager.isBoundKatana(item));
        
        // Handle death logic
        katanaManager.handleKatanaDeath(player);
    }
    
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        // Give back katana if applicable
        katanaManager.handlePlayerRespawn(player);
    }
    
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player hunter)) return;
        if (!(event.getEntity() instanceof Player target)) return;
        
        // Check if target has bounty
        // This will be expanded for bounty hunting
    }
}
