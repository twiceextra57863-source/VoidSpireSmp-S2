package com.mythicabilities.admin;

import com.mythicabilities.MythicAbilities;
import com.mythicabilities.abilities.Ability;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GiveAbilityCommand implements CommandExecutor, TabCompleter {
    
    private final MythicAbilities plugin;
    
    public GiveAbilityCommand(MythicAbilities plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        
        // Check permission
        if (!sender.hasPermission("mythicabilities.admin")) {
            sender.sendMessage("§cYou don't have permission!");
            return true;
        }
        
        // Handle different command formats
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }
        
        // Check if first argument is "list" to show all abilities
        if (args[0].equalsIgnoreCase("list")) {
            listAllAbilities(sender);
            return true;
        }
        
        // Give ability to self
        if (args.length == 1 && sender instanceof Player) {
            String abilityName = args[0];
            Player player = (Player) sender;
            giveAbilityToPlayer(sender, player, abilityName);
            return true;
        }
        
        // Give ability to specific player
        if (args.length == 2) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found: " + args[0]);
                return true;
            }
            
            String abilityName = args[1];
            giveAbilityToPlayer(sender, target, abilityName);
            return true;
        }
        
        // Invalid format
        sendHelpMessage(sender);
        return true;
    }
    
    private void giveAbilityToPlayer(CommandSender sender, Player target, String abilityName) {
        // Check if ability exists
        Ability ability = plugin.getAbilityManager().getAbility(abilityName);
        
        if (ability == null) {
            sender.sendMessage("§cAbility not found: " + abilityName);
            sender.sendMessage("§7Use §e/giveability list §7to see all abilities");
            return;
        }
        
        // Give ability to player
        plugin.getAbilityManager().setPlayerAbility(target, abilityName);
        
        // Success messages
        if (sender instanceof Player && sender.equals(target)) {
            sender.sendMessage(Component.text("§a✅ You gave yourself the ability: " + ability.getDisplayName()));
        } else {
            sender.sendMessage(Component.text("§a✅ Gave " + ability.getDisplayName() + " §ato §e" + target.getName()));
            target.sendMessage(Component.text("§aYou received the ability: " + ability.getDisplayName() + " §afrom an admin!"));
        }
        
        // Play effect for target
        target.playSound(target.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
        target.spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, target.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.5);
    }
    
    private void listAllAbilities(CommandSender sender) {
        sender.sendMessage("§6§l╔═══════════ AVAILABLE ABILITIES ═══════════╗");
        
        int count = 1;
        for (Ability ability : plugin.getAbilityManager().getAllAbilities().values()) {
            String cooldownColor = ability.getCooldown() > 30 ? "§c" : (ability.getCooldown() > 20 ? "§e" : "§a");
            sender.sendMessage("§6║ §f" + String.format("%2d", count) + ". " + 
                              ability.getDisplayName() + " §7- " + 
                              cooldownColor + ability.getCooldown() + "s §7cooldown");
            count++;
        }
        
        sender.sendMessage("§6╚═══════════════════════════════════════════╝");
        sender.sendMessage("§7Use §e/giveability <player> <ability> §7to give an ability");
    }
    
    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage("§6§l╔═══════════ GIVEABILITY COMMANDS ═══════════╗");
        sender.sendMessage("§6║ §e/giveability list §7- Show all abilities");
        sender.sendMessage("§6║ §e/giveability <ability> §7- Give ability to yourself");
        sender.sendMessage("§6║ §e/giveability <player> <ability> §7- Give ability to player");
        sender.sendMessage("§6║ ");
        sender.sendMessage("§6║ §7Example: §e/giveability §aPlayer1 §bvoid_walker");
        sender.sendMessage("§6╚═══════════════════════════════════════════╝");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!sender.hasPermission("mythicabilities.admin")) {
            return completions;
        }
        
        if (args.length == 1) {
            // First argument - suggest "list" or player names or ability names
            String partial = args[0].toLowerCase();
            
            // Add "list" if it matches
            if ("list".startsWith(partial)) {
                completions.add("list");
            }
            
            // Add online player names
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partial)) {
                    completions.add(player.getName());
                }
            }
            
            // Add ability names
            for (String abilityName : plugin.getAbilityManager().getAllAbilities().keySet()) {
                if (abilityName.toLowerCase().startsWith(partial)) {
                    completions.add(abilityName);
                }
            }
            
        } else if (args.length == 2) {
            // Second argument - suggest ability names
            String partial = args[1].toLowerCase();
            
            // Check if first argument is a valid player
            Player target = Bukkit.getPlayer(args[0]);
            if (target != null) {
                // Suggest ability names
                for (String abilityName : plugin.getAbilityManager().getAllAbilities().keySet()) {
                    if (abilityName.toLowerCase().startsWith(partial)) {
                        completions.add(abilityName);
                    }
                }
            }
        }
        
        return completions;
    }
}
