package com.mythicabilities.scoreboard;

import com.mythicabilities.MythicAbilities;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ScoreboardCommand implements CommandExecutor {
    
    private final MythicAbilities plugin;
    
    public ScoreboardCommand(MythicAbilities plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use scoreboard commands!");
            return true;
        }
        
        if (args.length == 0) {
            plugin.getScoreboardManager().toggleScoreboard(player);
            return true;
        }
        
        if (args[0].equalsIgnoreCase("on")) {
            if (plugin.getScoreboardManager().isScoreboardEnabled(player)) {
                player.sendMessage("§cScoreboard is already enabled!");
            } else {
                plugin.getScoreboardManager().toggleScoreboard(player);
            }
            return true;
        }
        
        if (args[0].equalsIgnoreCase("off")) {
            if (!plugin.getScoreboardManager().isScoreboardEnabled(player)) {
                player.sendMessage("§cScoreboard is already disabled!");
            } else {
                plugin.getScoreboardManager().toggleScoreboard(player);
            }
            return true;
        }
        
        player.sendMessage("§cUsage: /scoreboard [on|off]");
        return true;
    }
}
