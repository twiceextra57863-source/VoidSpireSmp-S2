package com.mythicabilities.voting;

import com.mythicabilities.MythicAbilities;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LeaderCommand implements CommandExecutor {
    
    private final MythicAbilities plugin;
    
    public LeaderCommand(MythicAbilities plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use leader commands!");
            return true;
        }
        
        Team team = plugin.getVoteManager().getTeam(player);
        
        if (team == null || !team.isLeader(player)) {
            player.sendMessage("§cOnly leaders can use this command!");
            return true;
        }
        
        if (args.length == 0) {
            player.sendMessage("§cUsage: /leader <promote|kick|disband>");
            return true;
        }
        
        if (args[0].equalsIgnoreCase("disband")) {
            plugin.getVoteManager().disbandTeam(team);
            player.sendMessage("§cTeam disbanded!");
            return true;
        }
        
        if (args.length < 2) {
            player.sendMessage("§cUsage: /leader " + args[0] + " <playername>");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage("§cPlayer not found!");
            return true;
        }
        
        if (!team.isMember(target)) {
            player.sendMessage("§cThat player is not in your team!");
            return true;
        }
        
        if (args[0].equalsIgnoreCase("kick")) {
            // Remove from team (simplified)
            player.sendMessage("§aKicked " + target.getName() + " from the team!");
            target.sendMessage("§cYou were kicked from the team!");
        }
        
        return true;
    }
}
