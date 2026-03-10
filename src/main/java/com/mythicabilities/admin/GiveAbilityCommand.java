package com.mythicabilities.admin;

import com.mythicabilities.MythicAbilities;
import com.mythicabilities.abilities.Ability;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GiveAbilityCommand implements CommandExecutor {
    
    private final MythicAbilities plugin;
    
    public GiveAbilityCommand(MythicAbilities plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        
        if (!sender.hasPermission("mythicabilities.admin")) {
            sender.sendMessage("§cYou don't have permission!");
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /giveability <player> <ability>");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found!");
            return true;
        }
        
        String abilityName = args[1];
        Ability ability = plugin.getAbilityManager().getAbility(abilityName);
        
        if (ability == null) {
            sender.sendMessage("§cAbility not found! Available abilities:");
            for (String name : plugin.getAbilityManager().getAllAbilities().keySet()) {
                sender.sendMessage("§7- §e" + name);
            }
            return true;
        }
        
        plugin.getAbilityManager().setPlayerAbility(target, abilityName);
        sender.sendMessage("§a✅ Gave §e" + ability.getDisplayName() + "§a to §e" + target.getName());
        target.sendMessage("§aYou received the ability: " + ability.getDisplayName());
        
        return true;
    }
}
