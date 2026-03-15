package com.mythicabilities.voting;

import com.mythicabilities.MythicAbilities;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class VoteCommand implements CommandExecutor {
    
    private final MythicAbilities plugin;
    
    public VoteCommand(MythicAbilities plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player voter)) {
            sender.sendMessage("Only players can vote!");
            return true;
        }
        
        if (args.length != 1) {
            voter.sendMessage("§cUsage: /vote <playername>");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            voter.sendMessage("§cPlayer not found: " + args[0]);
            return true;
        }
        
        plugin.getVoteManager().vote(voter, target);
        return true;
    }
}
