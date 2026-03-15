package com.mythicabilities.voting;

import com.mythicabilities.MythicAbilities;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class KatanaCommand implements CommandExecutor {
    
    private final MythicAbilities plugin;
    
    public KatanaCommand(MythicAbilities plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use katana commands!");
            return true;
        }
        
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            player.sendMessage("§6§l╔═══════════ 15 LEGENDARY KATANAS ═══════════╗");
            player.sendMessage("§6║  §b1. Storm Breaker     (Lightning)        §6║");
            player.sendMessage("§6║  §c2. Flame Dragon      (Fire)             §6║");
            player.sendMessage("§6║  §83. Shadow Dancer     (Dark)             §6║");
            player.sendMessage("§6║  §a4. Wind Cutter       (Wind)             §6║");
            player.sendMessage("§6║  §65. Earth Shaker      (Earth)            §6║");
            player.sendMessage("§6║  §36. Frost Bite        (Ice)              §6║");
            player.sendMessage("§6║  §57. Void Walker       (Void)             §6║");
            player.sendMessage("§6║  §e8. Sun Slash         (Light)            §6║");
            player.sendMessage("§6║  §29. Nature's Fang     (Nature)           §6║");
            player.sendMessage("§6║  §710. Stone Edge        (Rock)             §6║");
            player.sendMessage("§6║  §911. Wave Splitter     (Water)            §6║");
            player.sendMessage("§6║  §412. Blood Moon        (Blood)            §6║");
            player.sendMessage("§6║  §d13. Soul Reaper       (Soul)             §6║");
            player.sendMessage("§6║  §614. Thunder God       (Thunder)          §6║");
            player.sendMessage("§6║  §b15. Celestial Blade   (Divine)           §6║");
            player.sendMessage("§6╚══════════════════════════════════════════════╝");
            return true;
        }
        
        if (args[0].equalsIgnoreCase("info")) {
            Team team = plugin.getVoteManager().getTeam(player);
            if (team == null || !team.isLeader(player)) {
                player.sendMessage("§cOnly leaders have katanas!");
                return true;
            }
            
            player.sendMessage("§6§l╔═══════════ YOUR KATANA ═══════════╗");
            player.sendMessage("§6║ §eType: " + team.getKatanaType());
            player.sendMessage("§6║ §eDeaths: " + plugin.getKatanaManager().getDeathCount(player) + "/3");
            long cooldown = plugin.getKatanaManager().getTeamCooldown(player);
            if (cooldown > 0) {
                player.sendMessage("§6║ §eCooldown: §c" + cooldown + " hours");
            }
            player.sendMessage("§6╚════════════════════════════════════╝");
            return true;
        }
        
        return true;
    }
}
