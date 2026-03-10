package com.mythicabilities.admin;

import com.mythicabilities.MythicAbilities;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AdminPanelGUI implements Listener {
    
    private final MythicAbilities plugin;
    private final AdminCommand adminCommand;
    
    public AdminPanelGUI(MythicAbilities plugin, AdminCommand adminCommand) {
        this.plugin = plugin;
        this.adminCommand = adminCommand;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    public void openMainPanel(Player player) {
        Inventory gui = Bukkit.createInventory(null, 45, Component.text("§8⚡ Admin Control Panel ⚡"));
        
        // Fill border
        ItemStack border = createGuiItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 45; i++) {
            gui.setItem(i, border);
        }
        
        // SMP Control (Row 2)
        gui.setItem(10, createGuiItem(Material.COMMAND_BLOCK, 
            "§6§lSMP CONTROL",
            "§7Click to manage SMP",
            "§eStatus: " + (adminCommand.isSMPActive() ? "§aACTIVE" : "§cINACTIVE")));
        
        // Border Control
        gui.setItem(12, createGuiItem(Material.GRASS_BLOCK,
            "§a§lWORLD BORDER",
            "§7Current Size: §e" + getBorderSize(),
            "§7Click to configure"));
        
        // PVP Control
        Material pvpMaterial = adminCommand.isPVPEnabled() ? Material.DIAMOND_SWORD : Material.STONE_SWORD;
        gui.setItem(14, createGuiItem(pvpMaterial,
            "§c§lPVP CONTROL",
            "§7Status: " + (adminCommand.isPVPEnabled() ? "§aENABLED" : "§cDISABLED"),
            "§7Click to toggle"));
        
        // Grace Period
        gui.setItem(16, createGuiItem(Material.CLOCK,
            "§b§lGRACE PERIOD",
            "§7Time Left: §e" + adminCommand.getGraceTimeLeft() + "s",
            "§7Click to manage"));
        
        // Timer Settings (Row 3)
        gui.setItem(19, createGuiItem(Material.REDSTONE,
            "§e§lTIMER SETTINGS",
            "§7Set grace timer",
            "§7/ 30s / 60s / 120s"));
        
        // Teleport Controls
        gui.setItem(21, createGuiItem(Material.ENDER_PEARL,
            "§5§lTELEPORT CONTROLS",
            "§7Teleport all to spawn",
            "§7Teleport players to border"));
        
        // Player List
        gui.setItem(23, createGuiItem(Material.PLAYER_HEAD,
            "§2§lPLAYER MANAGEMENT",
            "§7Online: §a" + Bukkit.getOnlinePlayers().size(),
            "§7Click to manage players"));
        
        // Give Ability
        gui.setItem(25, createGuiItem(Material.BLAZE_POWDER,
            "§d§lGIVE ABILITY",
            "§7Give abilities to players",
            "§7/giveability <player> <ability>"));
        
        // Advanced Settings (Row 4)
        gui.setItem(28, createGuiItem(Material.REPEATER,
            "§8§lADVANCED SETTINGS",
            "§7Mob Spawning: §aON",
            "§7Fire Spread: §cOFF",
            "§7Daylight Cycle: §aON"));
        
        gui.setItem(30, createGuiItem(Material.BOOK,
            "§9§lCOMMANDS LIST",
            "§7/smp start - Start SMP",
            "§7/smp stop - Stop SMP",
            "§7/smp status - Show status"));
        
        gui.setItem(32, createGuiItem(Material.BARRIER,
            "§4§lEMERGENCY STOP",
            "§c§lSTOP ALL PROCESSES",
            "§7Click to emergency stop"));
        
        gui.setItem(34, createGuiItem(Material.NETHER_STAR,
            "§6§lSMP FEATURES",
            "§7• 20 Abilities System",
            "§7• Dynamic World Border",
            "§7• PvP Arena Ready"));
        
        player.openInventory(gui);
    }
    
    private ItemStack createGuiItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        
        if (lore.length > 0) {
            List<Component> loreComponents = Arrays.stream(lore)
                .map(line -> Component.text(line))
                .collect(Collectors.toList());
            meta.lore(loreComponents);
        }
        
        item.setItemMeta(meta);
        return item;
    }
    
    private String getBorderSize() {
        if (plugin.getServer().getWorlds().isEmpty()) return "Unknown";
        return String.valueOf((int) plugin.getServer().getWorlds().get(0).getWorldBorder().getSize());
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().title().equals(Component.text("§8⚡ Admin Control Panel ⚡"))) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        event.setCancelled(true);
        
        switch (event.getSlot()) {
            case 10: // SMP Control
                player.performCommand("smp status");
                player.closeInventory();
                break;
                
            case 12: // Border Control
                player.performCommand("smp border");
                player.closeInventory();
                break;
                
            case 14: // PVP Control
                player.performCommand("smp pvp");
                player.closeInventory();
                break;
                
            case 16: // Grace Period
                player.performCommand("smp grace");
                player.closeInventory();
                break;
                
            case 19: // Timer Settings
                openTimerSettings(player);
                break;
                
            case 23: // Player Management
                openPlayerList(player);
                break;
                
            case 25: // Give Ability
                player.sendMessage("§eUse: §6/giveability <player> <ability>");
                player.closeInventory();
                break;
                
            case 32: // Emergency Stop
                emergencyStop(player);
                break;
        }
    }
    
    private void openTimerSettings(Player player) {
        Inventory timerGui = Bukkit.createInventory(null, 27, Component.text("§8⏰ Timer Settings"));
        
        // Fill border
        ItemStack border = createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            timerGui.setItem(i, border);
        }
        
        timerGui.setItem(11, createGuiItem(Material.LIME_DYE, "§a30 SECONDS", "§7Grace Period"));
        timerGui.setItem(13, createGuiItem(Material.ORANGE_DYE, "§660 SECONDS", "§7Default"));
        timerGui.setItem(15, createGuiItem(Material.RED_DYE, "§c120 SECONDS", "§7Long Grace"));
        
        player.openInventory(timerGui);
    }
    
    private void openPlayerList(Player player) {
        // Simple implementation - you can expand this
        player.sendMessage("§6Online Players:");
        for (Player p : Bukkit.getOnlinePlayers()) {
            player.sendMessage("§7- " + p.getName() + " §e[§a" + p.getPing() + "ms§e]");
        }
        player.closeInventory();
    }
    
    private void emergencyStop(Player player) {
        player.performCommand("smp stop");
        player.sendMessage("§c§l⚠ EMERGENCY STOP ACTIVATED ⚠");
        player.closeInventory();
    }
}
