package com.mythicabilities.katana;

import com.mythicabilities.MythicAbilities;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class KatanaAdminCommand implements CommandExecutor, TabCompleter {
    
    private final MythicAbilities plugin;
    private final KatanaManager katanaManager;
    private final KatanaSpinGUI spinGUI;
    
    public KatanaAdminCommand(MythicAbilities plugin) {
        this.plugin = plugin;
        this.katanaManager = plugin.getKatanaManager();
        this.spinGUI = new KatanaSpinGUI(plugin);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        
        if (!sender.hasPermission("mythicabilities.admin")) {
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
                    sender.sendMessage("§cUsage: /katana give <player> [katana]");
                    return true;
                }
                handleGive(sender, args);
                break;
                
            case "giveall":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /katana giveall [katana]");
                    return true;
                }
                handleGiveAll(sender, args);
                break;
                
            case "giverandom":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /katana giverandom <player>");
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
                    sender.sendMessage("§cUsage: /katana spin <player>");
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
                    sender.sendMessage("§cUsage: /katana info <player>");
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
                    sender.sendMessage("§cUsage: /katana remove <player>");
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
            if (!KatanaData.isValidKatana(katanaName)) {
                sender.sendMessage("§cInvalid katana name! Use /katana list");
                return;
            }
            
            if (katanaManager.giveKatanaToPlayer(target, katanaName)) {
                sender.sendMessage("§a✅ Gave " + KatanaData.getDisplayName(katanaName) + " to " + target.getName());
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
            if (!KatanaData.isValidKatana(katanaName)) {
                sender.sendMessage("§cInvalid katana name! Use /katana list");
                return;
            }
            
            katanaManager.giveKatanaToAll(katanaName);
            sender.sendMessage("§a✅ Gave " + KatanaData.getDisplayName(katanaName) + " to all players");
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
        
        spinGUI.startVisualSpin(target, true); // true = admin spin
        sender.sendMessage("§a✅ Started spin for " + target.getName());
    }
    
    private void handleSpinAll(CommandSender sender) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            spinGUI.startVisualSpin(p, true);
        }
        sender.sendMessage("§a✅ Started spin for all players");
    }
    
    private void handleList(CommandSender sender) {
        sender.sendMessage("§6§l╔═══════════ AVAILABLE KATANAS ═══════════╗");
        
        List<String> katanas = KatanaData.getAllKatanaNames();
        for (int i = 0; i < katanas.size(); i++) {
            String katana = katanas.get(i);
            String display = KatanaData.getDisplayName(katana);
            sender.sendMessage("§6║ §f" + String.format("%2d", (i+1)) + ". " + display);
        }
        
        sender.sendMessage("§6╚═══════════════════════════════════════════╝");
    }
    
    private void handleInfo(CommandSender sender, String[] args) {
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found: " + args[1]);
            return;
        }
        
        KatanaData.KatanaInfo info = KatanaData.getPlayerKatana(target);
        
        sender.sendMessage("§6§l╔═══════════ PLAYER KATANA INFO ═══════════╗");
        sender.sendMessage("§6║ §ePlayer: §f" + target.getName());
        
        if (info != null) {
            sender.sendMessage("§6║ §eKatana: " + info.displayName);
            sender.sendMessage("§6║ §eModel: §f" + info.modelData);
            sender.sendMessage("§6║ §eObtained: §f" + new Date(info.obtainedTime));
        } else {
            sender.sendMessage("§6║ §cNo katana found!");
        }
        
        sender.sendMessage("§6╚═══════════════════════════════════════════╝");
    }
    
    private void handleCheck(Player player) {
        KatanaData.KatanaInfo info = KatanaData.getPlayerKatana(player);
        
        player.sendMessage("§6§l╔═══════════ YOUR KATANA ═══════════╗");
        if (info != null) {
            player.sendMessage("§6║ " + info.displayName);
            player.sendMessage("§6║ §eModel: §f" + info.modelData);
            player.sendMessage("§6║ §eObtained: §f" + new Date(info.obtainedTime));
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
        
        // Remove from inventory
        for (ItemStack item : target.getInventory().getContents()) {
            if (item != null && katanaManager.isBoundKatana(item)) {
                target.getInventory().remove(item);
            }
        }
        
        // Remove from data
        KatanaData.removePlayerKatana(target);
        
        sender.sendMessage("§c✅ Removed katana from " + target.getName());
        target.sendMessage("§cYour katana has been removed by an admin!");
    }
    
    private void handleRemoveAll(CommandSender sender) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            for (ItemStack item : p.getInventory().getContents()) {
                if (item != null && katanaManager.isBoundKatana(item)) {
                    p.getInventory().remove(item);
                }
            }
            KatanaData.removePlayerKatana(p);
        }
        
        Bukkit.broadcast(Component.text("§c§l✦ All katanas have been removed by an admin! ✦"));
        sender.sendMessage("§c✅ Removed all katanas");
    }
    
    private void handleReload(CommandSender sender) {
        // Reload configuration
        plugin.reloadConfig();
        sender.sendMessage("§a✅ Katana system reloaded!");
    }
    
    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6§l╔═══════════ KATANA ADMIN COMMANDS ═══════════╗");
        sender.sendMessage("§6║ §e/katana give <player> [katana]");
        sender.sendMessage("§6║ §7  - Give specific/random katana");
        sender.sendMessage("§6║ §e/katana giveall [katana]");
        sender.sendMessage("§6║ §7  - Give katana to all players");
        sender.sendMessage("§6║ §e/katana giverandom <player>");
        sender.sendMessage("§6║ §7  - Give random katana");
        sender.sendMessage("§6║ §e/katana giverandomall");
        sender.sendMessage("§6║ §7  - Give random to all");
        sender.sendMessage("§6║ §e/katana spin <player>");
        sender.sendMessage("§6║ §7  - Start visual spin");
        sender.sendMessage("§6║ §e/katana spinall");
        sender.sendMessage("§6║ §7  - Spin for all players");
        sender.sendMessage("§6║ §e/katana list");
        sender.sendMessage("§6║ §7  - List all katanas");
        sender.sendMessage("§6║ §e/katana info <player>");
        sender.sendMessage("§6║ §7  - Check player's katana");
        sender.sendMessage("§6║ §e/katana check");
        sender.sendMessage("§6║ §7  - Check your katana");
        sender.sendMessage("§6║ §e/katana remove <player>");
        sender.sendMessage("§6║ §7  - Remove katana from player");
        sender.sendMessage("§6║ §e/katana removeall");
        sender.sendMessage("§6║ §7  - Remove all katanas");
        sender.sendMessage("§6║ §e/katana reload");
        sender.sendMessage("§6║ §7  - Reload configuration");
        sender.sendMessage("§6╚══════════════════════════════════════════════╝");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!sender.hasPermission("mythicabilities.admin")) {
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
                args[0].equalsIgnoreCase("spin")) {
                
                // Suggest player names
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(partial)) {
                        completions.add(p.getName());
                    }
                }
                
            } else if (args[0].equalsIgnoreCase("giveall")) {
                // Suggest katana names
                for (String katana : KatanaData.getAllKatanaNames()) {
                    if (katana.toLowerCase().startsWith(partial)) {
                        completions.add(katana);
                    }
                }
            }
            
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            String partial = args[2].toLowerCase();
            
            // Suggest katana names
            for (String katana : KatanaData.getAllKatanaNames()) {
                if (katana.toLowerCase().startsWith(partial)) {
                    completions.add(katana);
                }
            }
        }
        
        return completions;
    }
          }
