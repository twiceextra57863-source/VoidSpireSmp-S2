package com.mythicabilities.voting;

import com.mythicabilities.MythicAbilities;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TeamCommand implements CommandExecutor {
    
    private final MythicAbilities plugin;
    
    public TeamCommand(MythicAbilities plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use team commands!");
            return true;
        }
        
        Team team = plugin.getVoteManager().getTeam(player);
        
        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            if (team == null) {
                player.sendMessage("§cYou are not in a team!");
                return true;
            }
            
            player.sendMessage("§6§l╔═══════════ TEAM INFO ═══════════╗");
            player.sendMessage("§6║ §eName: §f" + team.getTeamName());
            player.sendMessage("§6║ §eLeader: §f" + team.getLeader().getName());
            player.sendMessage("§6║ §eMembers: §f" + team.getMembers().size());
            player.sendMessage("§6║ §eKatana: " + team.getKatanaType());
            player.sendMessage("§6║ §eColor: " + team.getTeamColor() + "■");
            player.sendMessage("§6╚══════════════════════════════════╝");
            return true;
        }
        
        if (args[0].equalsIgnoreCase("members")) {
            if (team == null) {
                player.sendMessage("§cYou are not in a team!");
                return true;
            }
            
            player.sendMessage("§6§l╔═══════════ TEAM MEMBERS ═══════════╗");
            for (Player member : team.getMembers()) {
                String prefix = team.isLeader(member) ? "§c§l[LEADER] " : "§7[MEMBER] ";
                player.sendMessage("§6║ " + prefix + member.getName());
            }
            player.sendMessage("§6╚══════════════════════════════════════╝");
            return true;
        }
        
        if (args[0].equalsIgnoreCase("setname") && args.length >= 2) {
            if (team == null || !team.isLeader(player)) {
                player.sendMessage("§cOnly leaders can change team name!");
                return true;
            }
            
            String newName = args[1];
            team.setTeamName(newName);
            return true;
        }
        
        if (args[0].equalsIgnoreCase("setcolor") && args.length >= 2) {
            if (team == null || !team.isLeader(player)) {
                player.sendMessage("§cOnly leaders can change team color!");
                return true;
            }
            
            try {
                ChatColor color = ChatColor.valueOf(args[1].toUpperCase());
                team.setTeamColor(color);
            } catch (IllegalArgumentException e) {
                player.sendMessage("§cInvalid color! Use: RED, BLUE, GREEN, YELLOW, LIGHT_PURPLE, AQUA");
            }
            return true;
        }
        
        player.sendMessage("§cUsage: /team <info|members|setname|setcolor>");
        return true;
    }
}
