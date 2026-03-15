package com.mythicabilities.katana;

import com.mythicabilities.MythicAbilities;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class KatanaAdminCommand implements CommandExecutor, TabCompleter {
    
    private final MythicAbilities plugin;
    private final com.mythicabilities.katana.KatanaManager katanaManager;
    private final KatanaSpinGUI spinGUI;
    
    public KatanaAdminCommand(MythicAbilities plugin) {
        this.plugin = plugin;
        this.katanaManager = plugin.getKatanaAdminManager();
        this.spinGUI = new KatanaSpinGUI(plugin);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        
        if (!sender.hasPermission("mythicabilities.katana.admin")) {
            sender.sendMessage("§cYou don't have permission!");
            return true;
        }
        
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            // ============= GIVE COMMANDS =============
            case "give":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /katanadmin give <player> [katana]");
                    return true;
                }
                handleGive(sender, args);
                break;
                
            case "giveall":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /katanadmin giveall [katana]");
                    return true;
                }
                handleGiveAll(sender, args);
                break;
                
            case "giverandom":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /katanadmin giverandom <player>");
                    return true;
                }
                handleGiveRandom(sender, args);
                break;
                
            case "giverandomall":
                handleGiveRandomAll(sender);
                break;
                
            // ============= SPIN COMMANDS =============
            case "spin":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /katanadmin spin <player>");
                    return true;
                }
                handleSpin(sender, args);
                break;
                
            case "spinall":
                handleSpinAll(sender);
                break;
                
            // ============= INFO COMMANDS =============
            case "list":
                handleList(sender);
                break;
                
            case "info":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /katanadmin info <player>");
                    return true;
                }
                handleInfo(sender, args);
                break;
                
            case "check":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cOnly players can check their katana!");
                    return true;
                }
                handleCheck((Player) sender);
                break;
                
            // ============= REMOVE COMMANDS =============
            case "remove":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /katanadmin remove <player>");
                    return true;
                }
                handleRemove(sender, args);
                break;
                
            case "removeall":
                handleRemoveAll(sender);
                break;
                
            // ============= RELOAD =============
            case "reload":
                handleReload(sender);
                break;
                
            default:
                sendHelp(sender);
                break;
        }
        
        return true;
    }
    
    // ============= HANDLER METHODS =============
    
    private void handleGive(CommandSender sender, String[] args) {
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found: " + args[1]);
            return;
        }
        
        if (args.length >= 3) {
            // Give specific katana
            String katanaName = args[2];
            // Handle multi-word katana names
            if (args.length > 3) {
                StringBuilder sb = new StringBuilder();
                for (int i = 2; i < args.length; i++) {
                    sb.append(args[i]).append(" ");
                }
                katanaName = sb.toString().trim();
            }
            
            if (!katanaManager.isValidKatana(katanaName)) {
                sender.sendMessage("§cInvalid katana name! Use /katanadmin list");
                return;
            }
            
            if (katanaManager.giveKatanaToPlayer(target, katanaName)) {
                sender.sendMessage("§a✅ Gave " + katanaManager.getDisplayName(katanaName) + " to " + target.getName());
            }
        } else {
            // Give random katana
            katanaManager.giveRandomKatanaToPlayer(target);
            sender.sendMessage("§a✅ Gave random katana to " + target.getName());
        }
    }
    
    private void handleGiveAll(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            String katanaName = args[1];
            // Handle multi-word katana names
            if (args.length > 2) {
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i < args.length; i++) {
                    sb.append(args[i]).append(" ");
                }
                katanaName = sb.toString().trim();
            }
            
            if (!katanaManager.isValidKatana(katanaName)) {
                sender.sendMessage("§cInvalid katana name! Use /katanadmin list");
                return;
            }
            
            katanaManager.giveKatanaToAll(katanaName);
            sender.sendMessage("§a✅ Gave " + katanaManager.getDisplayName(katanaName) + " to all players");
        }
    }
    
    private void handleGiveRandom(CommandSender sender, String[] args) {
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found: " + args[1]);
            return;
        }
        
        katanaManager.giveRandomKatanaToPlayer(target);
        sender.sendMessage("§a✅ Gave random katana to " + target.getName());
    }
    
    private void handleGiveRandomAll(CommandSender sender) {
        katanaManager.giveRandomKatanaToAll();
        sender.sendMessage("§a✅ Gave random katana to all players");
    }
    
    private void handleSpin(CommandSender sender, String[] args) {
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found: " + args[1]);
            return;
        }
        
        if (spinGUI.isSpinning(target)) {
            sender.sendMessage("§c" + target.getName() + " is already spinning!");
            return;
        }
        
        spinGUI.startVisualSpin(target, true);
        sender.sendMessage("§a✅ Started cinematic spin for " + target.getName());
    }
    
    private void handleSpinAll(CommandSender sender) {
        int count = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!spinGUI.isSpinning(p)) {
                spinGUI.startVisualSpin(p, true);
                count++;
            }
        }
        sender.sendMessage("§a✅ Started cinematic spin for " + count + " players");
    }
    
    private void handleList(CommandSender sender) {
        sender.sendMessage("§6§l╔═══════════ 16 LEGENDARY KATANAS ═══════════╗");
        
        String[] katanas = katanaManager.getKatanaNames();
        for (int i = 0; i < katanas.length; i++) {
            String display = katanaManager.getDisplayName(katanas[i]);
            sender.sendMessage("§6║ §f" + String.format("%2d", (i+1)) + ". " + display);
        }
        
        sender.sendMessage("§6╚══════════════════════════════════════════════╝");
    }
    
    private void handleInfo(CommandSender sender, String[] args) {
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found: " + args[1]);
            return;
        }
        
        // Find katana in inventory
        ItemStack katana = null;
        for (ItemStack item : target.getInventory().getContents()) {
            if (item != null && katanaManager.isBoundKatana(item)) {
                katana = item;
                break;
            }
        }
        
        sender.sendMessage("§6§l╔═══════════ PLAYER KATANA INFO ═══════════╗");
        sender.sendMessage("§6║ §ePlayer: §f" + target.getName());
        
        if (katana != null) {
            String katanaName = katanaManager.getKatanaName(katana);
            UUID ownerId = katanaManager.getKatanaOwner(katana);
            Player owner = ownerId != null ? Bukkit.getPlayer(ownerId) : null;
            
            sender.sendMessage("§6║ §eKatana: " + katanaManager.getDisplayName(katanaName));
            if (owner != null) {
                sender.sendMessage("§6║ §eBound to: §f" + owner.getName());
            }
            sender.sendMessage("§6║ §eModel: §f" + katana.getItemMeta().getCustomModelData());
        } else {
            sender.sendMessage("§6║ §cNo katana found in inventory!");
        }
        
        sender.sendMessage("§6╚═══════════════════════════════════════════╝");
    }
    
    private void handleCheck(Player player) {
        // Find katana in inventory
        ItemStack katana = null;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && katanaManager.isBoundKatana(item)) {
                katana = item;
                break;
            }
        }
        
        player.sendMessage("§6§l╔═══════════ YOUR KATANA ═══════════╗");
        if (katana != null) {
            String katanaName = katanaManager.getKatanaName(katana);
            UUID ownerId = katanaManager.getKatanaOwner(katana);
            
            player.sendMessage("§6║ " + katanaManager.getDisplayName(katanaName));
            if (ownerId != null && ownerId.equals(player.getUniqueId())) {
                player.sendMessage("§6║ §a✓ Bound to you");
            } else if (ownerId != null) {
                Player owner = Bukkit.getPlayer(ownerId);
                player.sendMessage("§6║ §c✗ Bound to: " + (owner != null ? owner.getName() : "unknown"));
            }
        } else {
            player.sendMessage("§6║ §cYou don't have a katana!");
        }
        player.sendMessage("§6╚════════════════════════════════════╝");
    }
    
    private void handleRemove(CommandSender sender, String[] args) {
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found: " + args[1]);
            return;
        }
        
        katanaManager.removeKatanaFromPlayer(target);
        sender.sendMessage("§c✅ Removed katana from " + target.getName());
        target.sendMessage("§cYour katana has been removed by an admin!");
    }
    
    private void handleRemoveAll(CommandSender sender) {
        katanaManager.removeAllKatanas();
        Bukkit.broadcast(Component.text("§c§l✦ All katanas have been removed by an admin! ✦"));
        sender.sendMessage("§c✅ Removed all katanas");
    }
    
    private void handleReload(CommandSender sender) {
        // Reload configuration if needed
        plugin.reloadConfig();
        sender.sendMessage("§a✅ Katana system reloaded!");
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6§l╔═══════════ KATANA ADMIN COMMANDS ═══════════╗");
        sender.sendMessage("§6║ §e/katanadmin give <player> [katana]");
        sender.sendMessage("§6║ §7  - Give specific/random katana");
        sender.sendMessage("§6║ §e/katanadmin giveall [katana]");
        sender.sendMessage("§6║ §7  - Give katana to all players");
        sender.sendMessage("§6║ §e/katanadmin giverandom <player>");
        sender.sendMessage("§6║ §7  - Give random katana");
        sender.sendMessage("§6║ §e/katanadmin giverandomall");
        sender.sendMessage("§6║ §7  - Give random to all");
        sender.sendMessage("§6║ §e/katanadmin spin <player>");
        sender.sendMessage("§6║ §7  - Start cinematic spin");
        sender.sendMessage("§6║ §e/katanadmin spinall");
        sender.sendMessage("§6║ §7  - Spin for all players");
        sender.sendMessage("§6║ §e/katanadmin list");
        sender.sendMessage("§6║ §7  - List all katanas");
        sender.sendMessage("§6║ §e/katanadmin info <player>");
        sender.sendMessage("§6║ §7  - Check player's katana");
        sender.sendMessage("§6║ §e/katanadmin check");
        sender.sendMessage("§6║ §7  - Check your katana");
        sender.sendMessage("§6║ §e/katanadmin remove <player>");
        sender.sendMessage("§6║ §7  - Remove katana from player");
        sender.sendMessage("§6║ §e/katanadmin removeall");
        sender.sendMessage("§6║ §7  - Remove all katanas");
        sender.sendMessage("§6║ §e/katanadmin reload");
        sender.sendMessage("§6║ §7  - Reload configuration");
        sender.sendMessage("§6╚══════════════════════════════════════════════╝");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!sender.hasPermission("mythicabilities.katana.admin")) {
            return completions;
        }
        
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            List<String> commands = Arrays.asList(
                "give", "giveall", "giverandom", "giverandomall",
                "spin", "spinall", "list", "info", "check",
                "remove", "removeall", "reload"
            );
            
            for (String cmd : commands) {
                if (cmd.startsWith(partial)) {
                    completions.add(cmd);
                }
            }
            
        } else if (args.length == 2) {
            String partial = args[1].toLowerCase();
            
            if (args[0].equalsIgnoreCase("give") || 
                args[0].equalsIgnoreCase("info") || 
                args[0].equalsIgnoreCase("remove") ||
                args[0].equalsIgnoreCase("spin") ||
                args[0].equalsIgnoreCase("giverandom")) {
                
                // Suggest player names
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(partial)) {
                        completions.add(p.getName());
                    }
                }
                
            } else if (args[0].equalsIgnoreCase("giveall")) {
                // Suggest katana names
                for (String katana : katanaManager.getKatanaNames()) {
                    if (katana.toLowerCase().startsWith(partial)) {
                        completions.add(katana);
                    }
                }
            }
            
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            String partial = args[2].toLowerCase();
            
            // Suggest katana names
            for (String katana : katanaManager.getKatanaNames()) {
                if (katana.toLowerCase().startsWith(partial)) {
                    completions.add(katana);
                }
            }
        }
        
        return completions;
    }
}
