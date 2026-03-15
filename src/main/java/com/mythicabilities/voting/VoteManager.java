package com.mythicabilities.voting;

import com.mythicabilities.MythicAbilities;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class VoteManager {
    
    private final MythicAbilities plugin;
    private boolean votingActive = false;
    private final Map<UUID, Integer> votes = new HashMap<>();
    private final Map<UUID, List<UUID>> voterMap = new HashMap<>();
    private final Map<UUID, Team> teams = new HashMap<>();
    
    public VoteManager(MythicAbilities plugin) {
        this.plugin = plugin;
    }
    
    public void startVoting() {
        votingActive = true;
        votes.clear();
        voterMap.clear();
        
        Bukkit.broadcast(Component.text("§6§l╔════════════════════════════════════╗"));
        Bukkit.broadcast(Component.text("§6§l║     §e🗳️ VOTING HAS STARTED! 🗳️     §6§l║"));
        Bukkit.broadcast(Component.text("§6§l║                                    §6§l║"));
        Bukkit.broadcast(Component.text("§6§l║  §fVote for your leader with:      §6§l║"));
        Bukkit.broadcast(Component.text("§6§l║  §e/vote <playername>              §6§l║"));
        Bukkit.broadcast(Component.text("§6§l║                                    §6§l║"));
        Bukkit.broadcast(Component.text("§6§l║  §7First player with 3 votes wins! §6§l║"));
        Bukkit.broadcast(Component.text("§6§l╚════════════════════════════════════╝"));
        
        // Auto-end after 5 minutes
        new BukkitRunnable() {
            @Override
            public void run() {
                if (votingActive) {
                    endVoting(null);
                }
            }
        }.runTaskLater(plugin, 6000); // 5 minutes
    }
    
    public boolean vote(Player voter, Player target) {
        if (!votingActive) {
            voter.sendMessage("§cVoting is not active!");
            return false;
        }
        
        if (voter.equals(target)) {
            voter.sendMessage("§cYou cannot vote for yourself!");
            return false;
        }
        
        UUID targetId = target.getUniqueId();
        List<UUID> voters = voterMap.getOrDefault(targetId, new ArrayList<>());
        
        if (voters.contains(voter.getUniqueId())) {
            voter.sendMessage("§cYou already voted for " + target.getName());
            return false;
        }
        
        voters.add(voter.getUniqueId());
        voterMap.put(targetId, voters);
        
        int voteCount = voters.size();
        votes.put(targetId, voteCount);
        
        // Broadcast vote
        Bukkit.broadcast(Component.text("§e" + voter.getName() + " §fvoted for §e" + target.getName() + 
            " §7(§a" + voteCount + "§7/3 votes)"));
        
        // Check if target has 3 votes
        if (voteCount >= 3) {
            endVoting(target);
        }
        
        return true;
    }
    
    private void endVoting(Player winner) {
        votingActive = false;
        
        if (winner == null) {
            // No winner - pick random from top votes
            winner = getRandomTopVoter();
        }
        
        if (winner != null) {
            createTeam(winner);
            
            Bukkit.broadcast(Component.text("§6§l╔════════════════════════════════════╗"));
            Bukkit.broadcast(Component.text("§6§l║     §e👑 LEADER ELECTED! 👑       §6§l║"));
            Bukkit.broadcast(Component.text("§6§l║                                    §6§l║"));
            Bukkit.broadcast(Component.text("§6§l║   §fThe new leader is:            §6§l║"));
            Bukkit.broadcast(Component.text("§6§l║   §e§l" + winner.getName() + "            §6§l║"));
            Bukkit.broadcast(Component.text("§6§l║                                    §6§l║"));
            Bukkit.broadcast(Component.text("§6§l║  §7They received a legendary      §6§l║"));
            Bukkit.broadcast(Component.text("§6§l║  §7Katana!                        §6§l║"));
            Bukkit.broadcast(Component.text("§6§l╚════════════════════════════════════╝"));
        } else {
            Bukkit.broadcast("§cNo leader elected! Try again later.");
        }
    }
    
    private Player getRandomTopVoter() {
        // Find players with votes
        List<Map.Entry<UUID, Integer>> sorted = new ArrayList<>(votes.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        
        if (!sorted.isEmpty()) {
            UUID topId = sorted.get(0).getKey();
            return Bukkit.getPlayer(topId);
        }
        return null;
    }
    
    private void createTeam(Player leader) {
        Team team = new Team(leader, voterMap.get(leader.getUniqueId()));
        teams.put(leader.getUniqueId(), team);
    }
    
    public Team getTeam(Player player) {
        for (Team team : teams.values()) {
            if (team.isMember(player)) {
                return team;
            }
        }
        return null;
    }
}
