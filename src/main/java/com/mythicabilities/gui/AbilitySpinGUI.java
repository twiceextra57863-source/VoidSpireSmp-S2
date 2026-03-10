package com.mythicabilities.gui;

import com.mythicabilities.MythicAbilities;
import com.mythicabilities.abilities.Ability;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.*;

public class AbilitySpinGUI implements Listener, org.bukkit.command.CommandExecutor {
    
    private final MythicAbilities plugin;
    private final Map<UUID, Boolean> isSpinning = new HashMap<>();
    private final Random random = new Random();
    
    public AbilitySpinGUI(MythicAbilities plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    public void openSpinGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, Component.text("§8✦ Spin for Ability ✦"));
        
        // Fill border with glass
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.displayName(Component.text(" "));
        border.setItemMeta(borderMeta);
        
        for (int i = 0; i < 54; i++) {
            gui.setItem(i, border);
        }
        
        // Center slot for spin wheel
        ItemStack spinWheel = new ItemStack(Material.NETHER_STAR);
        ItemMeta spinMeta = spinWheel.getItemMeta();
        spinMeta.displayName(Component.text("§6§l✦ SPIN ✦"));
        spinMeta.lore(List.of(
            Component.text("§7Click to spin for a random ability!"),
            Component.text("§eYou'll get 1 of 20 abilities!")
        ));
        spinWheel.setItemMeta(spinMeta);
        gui.setItem(22, spinWheel);
        
        // Show ability previews around
        List<Ability> abilities = new ArrayList<>(plugin.getAbilityManager().getAllAbilities().values());
        int[] slots = {19, 20, 21, 23, 24, 25};
        
        for (int i = 0; i < Math.min(6, abilities.size()); i++) {
            gui.setItem(slots[i], abilities.get(i).getIcon());
        }
        
        player.openInventory(gui);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().title().equals(Component.text("§8✦ Spin for Ability ✦"))) return;
        
        event.setCancelled(true);
        
        if (event.getSlot() == 22 && !isSpinning.getOrDefault(player.getUniqueId(), false)) {
            startSpinAnimation(player);
        }
    }
    
    private void startSpinAnimation(Player player) {
        isSpinning.put(player.getUniqueId(), true);
        
        List<String> abilityNames = new ArrayList<>(plugin.getAbilityManager().getAllAbilities().keySet());
        String selectedAbility = abilityNames.get(random.nextInt(abilityNames.size()));
        
        player.closeInventory();
        
        // Spin animation
        new BukkitRunnable() {
            int spinCount = 0;
            final int maxSpins = 30;
            
            @Override
            public void run() {
                if (spinCount >= maxSpins) {
                    // Show selected ability
                    player.showTitle(Title.title(
                        Component.text("§6§l✦ ABILITY UNLOCKED! ✦"),
                        Component.text("§e" + plugin.getAbilityManager().getAbility(selectedAbility).getDisplayName()),
                        Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(3), Duration.ofSeconds(1))
                    ));
                    
                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
                    
                    // Save ability to player
                    plugin.getAbilityManager().setPlayerAbility(player, selectedAbility);
                    isSpinning.put(player.getUniqueId(), false);
                    
                    // Spawn celebration particles
                    for (int i = 0; i < 5; i++) {
                        player.getWorld().spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING,
                            player.getLocation().add(0, 2, 0), 30, 0.5, 0.5, 0.5, 0.5);
                    }
                    
                    this.cancel();
                    return;
                }
                
                // Show spinning animation
                String currentAbility = abilityNames.get(random.nextInt(abilityNames.size()));
                player.sendActionBar(Component.text("§f✦ §e" + 
                    plugin.getAbilityManager().getAbility(currentAbility).getDisplayName() + " §f✦"));
                
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1, 
                    1 + (spinCount * 0.05f));
                
                spinCount++;
            }
        }.runTaskTimer(plugin, 0, 2);
    }
    
    @Override
    public boolean onCommand(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, 
                            String label, String[] args) {
        if (sender instanceof Player player) {
            openSpinGUI(player);
        }
        return true;
    }
                      }
