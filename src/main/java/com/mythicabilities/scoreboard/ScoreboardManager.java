package com.mythicabilities.scoreboard;

import com.mythicabilities.MythicAbilities;
import com.mythicabilities.voting.Team;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ScoreboardManager {
    
    private final MythicAbilities plugin;
    private final Map<UUID, PlayerStats> playerStats = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> scoreboardEnabled = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> animationTasks = new ConcurrentHashMap<>();
    private org.bukkit.scoreboard.Scoreboard scoreboard; // FIXED: Use fully qualified name
    private Objective objective;
    private Player currentAnimatingPlayer = null;
    private boolean animationInProgress = false;
    
    public ScoreboardManager(MythicAbilities plugin) {
        this.plugin = plugin;
        setupScoreboard();
        startScoreboardUpdater();
    }
    
    private void setupScoreboard() {
        org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager(); // FIXED: Use fully qualified name
        scoreboard = manager.getNewScoreboard(); // FIXED: Now works with correct type
        objective = scoreboard.registerNewObjective("teamrank", "dummy", 
            Component.text("§6§l✦ TEAM RANKINGS ✦"));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }
    
    private void startScoreboardUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                updateAllScoreboards();
            }
        }.runTaskTimer(plugin, 0, 20); // Update every second
    }
    
    private void updateAllScoreboards() {
        // Get all teams with their scores
        List<TeamScore> teamScores = calculateTeamScores();
        
        // Update scoreboard for each player who has it enabled
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (scoreboardEnabled.getOrDefault(player.getUniqueId(), true)) {
                updatePlayerScoreboard(player, teamScores);
            }
        }
    }
    
    private List<TeamScore> calculateTeamScores() {
        Map<String, TeamScore> teamScoreMap = new HashMap<>();
        
        // Collect stats from all players
        for (PlayerStats stats : playerStats.values()) {
            Player player = Bukkit.getPlayer(stats.playerId);
            if (player == null) continue;
            
            Team team = plugin.getVoteManager().getTeam(player);
            if (team == null) continue;
            
            String teamName = team.getTeamName();
            TeamScore teamScore = teamScoreMap.getOrDefault(teamName, 
                new TeamScore(team, 0, new ArrayList<>()));
            
            // Calculate individual score
            double individualScore = (stats.kills * 3) + (stats.assists * 1.5) - (stats.deaths * 2);
            stats.score = individualScore;
            
            // Add to team total
            teamScore.totalScore += individualScore;
            teamScore.members.add(stats);
            
            teamScoreMap.put(teamName, teamScore);
        }
        
        // Sort and get top 3 teams
        List<TeamScore> sortedTeams = new ArrayList<>(teamScoreMap.values());
        sortedTeams.sort((a, b) -> Double.compare(b.totalScore, a.totalScore));
        
        return sortedTeams.size() > 3 ? sortedTeams.subList(0, 3) : sortedTeams;
    }
    
    private void updatePlayerScoreboard(Player player, List<TeamScore> topTeams) {
        // Clear existing entries
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }
        
        // Add header
        objective.getScore("§8§m-------------------").setScore(15);
        objective.getScore("§6§l⚔ TOP TEAMS ⚔").setScore(14);
        objective.getScore("§8§m-------------------").setScore(13);
        
        int position = 12;
        int rank = 1;
        
        // Display top 3 teams
        for (TeamScore teamScore : topTeams) {
            Team team = teamScore.team;
            ChatColor teamColor = team.getTeamColor();
            
            // Team header with rank
            String rankColor = rank == 1 ? "§6§l" : (rank == 2 ? "§e§l" : "§7§l");
            objective.getScore(rankColor + rank + ". " + teamColor + team.getTeamName()).setScore(position--);
            
            // Team total score
            objective.getScore("§7 Score: §f" + String.format("%.1f", teamScore.totalScore)).setScore(position--);
            
            // Team leader
            Player leader = team.getLeader();
            PlayerStats leaderStats = playerStats.get(leader.getUniqueId());
            if (leaderStats != null) {
                objective.getScore("§7 Leader: §e" + leader.getName()).setScore(position--);
                objective.getScore("§7  §8K: " + leaderStats.kills + " D: " + leaderStats.deaths + " A: " + leaderStats.assists).setScore(position--);
            }
            
            // Top 2 members
            List<PlayerStats> topMembers = teamScore.getTopMembers(2);
            for (int i = 0; i < topMembers.size(); i++) {
                PlayerStats memberStats = topMembers.get(i);
                Player memberPlayer = Bukkit.getPlayer(memberStats.playerId);
                if (memberPlayer != null && !memberPlayer.equals(leader)) {
                    String prefix = i == 0 ? "§7 1st: " : "§7 2nd: ";
                    objective.getScore(prefix + "§f" + memberPlayer.getName()).setScore(position--);
                    objective.getScore("§7   §8K: " + memberStats.kills + " D: " + memberStats.deaths + " A: " + memberStats.assists).setScore(position--);
                }
            }
            
            // Separator between teams
            if (rank < topTeams.size()) {
                objective.getScore("§8-------------------").setScore(position--);
            }
            
            rank++;
        }
        
        // Footer with player's own stats
        objective.getScore("§8§m-------------------").setScore(position--);
        PlayerStats ownStats = playerStats.get(player.getUniqueId());
        if (ownStats != null) {
            objective.getScore("§6Your Stats:").setScore(position--);
            objective.getScore("§7 Kills: §f" + ownStats.kills).setScore(position--);
            objective.getScore("§7 Deaths: §f" + ownStats.deaths).setScore(position--);
            objective.getScore("§7 Assists: §f" + ownStats.assists).setScore(position--);
            objective.getScore("§7 Score: §f" + String.format("%.1f", ownStats.score)).setScore(position--);
        }
        
        objective.getScore("§8§m-------------------").setScore(position);
        
        // Set scoreboard for player
        player.setScoreboard(scoreboard);
    }
    
    /**
     * Check if player should become new #1 and trigger animation
     */
    public void checkAndTriggerAnimation(Player player) {
        if (animationInProgress || currentAnimatingPlayer != null) return;
        
        PlayerStats stats = playerStats.get(player.getUniqueId());
        if (stats == null) return;
        
        // Calculate current rankings
        List<TeamScore> topTeams = calculateTeamScores();
        if (topTeams.isEmpty()) return;
        
        TeamScore topTeam = topTeams.get(0);
        if (topTeam == null) return;
        
        // Check if this player is in top team and has highest individual score
        Player topPlayer = getTopPlayerInTeam(topTeam);
        if (topPlayer != null && topPlayer.equals(player)) {
            // Check if player was not #1 before
            if (!stats.wasNumberOne) {
                startNumberOneAnimation(player);
                stats.wasNumberOne = true;
            }
        } else {
            // Player lost #1 position
            stats.wasNumberOne = false;
        }
    }
    
    private Player getTopPlayerInTeam(TeamScore teamScore) {
        if (teamScore.members.isEmpty()) return null;
        
        PlayerStats topStats = Collections.max(teamScore.members, 
            (a, b) -> Double.compare(a.score, b.score));
        
        return Bukkit.getPlayer(topStats.playerId);
    }
    
    /**
     * Start the #1 animation (15 seconds invincibility + effects)
     */
    private void startNumberOneAnimation(Player player) {
        animationInProgress = true;
        currentAnimatingPlayer = player;
        
        Location originalLoc = player.getLocation();
        
        // Make player invincible
        player.setInvulnerable(true);
        
        // Freeze player
        player.setWalkSpeed(0);
        player.setAllowFlight(true);
        player.setFlying(true);
        
        // Broadcast message
        Bukkit.broadcast(Component.text("§6§l╔════════════════════════════════════╗"));
        Bukkit.broadcast(Component.text("§6§l║     §e⚡ NEW #1 PLAYER! ⚡         §6§l║"));
        Bukkit.broadcast(Component.text("§6§l║                                    §6§l║"));
        Bukkit.broadcast(Component.text("§6§l║   §f" + player.getName() + " has reached    §6§l║"));
        Bukkit.broadcast(Component.text("§6§l║   §fthe top of the leaderboard!   §6§l║"));
        Bukkit.broadcast(Component.text("§6§l╚════════════════════════════════════╝"));
        
        // Title for the player
        player.showTitle(Title.title(
            Component.text("§6§l⚡ NUMBER ONE! ⚡"),
            Component.text("§eYou are now the top player!"),
            Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(3), Duration.ofSeconds(1))
        ));
        
        // Start animation effects
        new BukkitRunnable() {
            int ticks = 0;
            
            @Override
            public void run() {
                if (ticks >= 300) { // 15 seconds
                    endNumberOneAnimation(player);
                    this.cancel();
                    return;
                }
                
                // Spiral up particles
                double angle = ticks * 0.3;
                double radius = 2;
                double yOffset = ticks * 0.05; // FIXED: Renamed to yOffset to avoid conflict
                
                for (int i = 0; i < 4; i++) {
                    double offsetAngle = angle + (i * 90 * Math.PI / 180);
                    double x = originalLoc.getX() + radius * Math.cos(offsetAngle);
                    double z = originalLoc.getZ() + radius * Math.sin(offsetAngle);
                    
                    Location particleLoc = new Location(player.getWorld(), x, originalLoc.getY() + yOffset, z);
                    
                    // Alternate particles
                    if (ticks % 10 < 5) {
                        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, particleLoc, 5, 0.1, 0.1, 0.1, 0.5);
                        player.getWorld().spawnParticle(Particle.END_ROD, particleLoc, 3, 0.1, 0.1, 0.1, 0.02);
                    } else {
                        player.getWorld().spawnParticle(Particle.FIREWORK, particleLoc, 5, 0.1, 0.1, 0.1, 0.02);
                        player.getWorld().spawnParticle(Particle.ENCHANT, particleLoc, 3, 0.1, 0.1, 0.1, 0.3);
                    }
                }
                
                // Beam of light
                for (double y = 0; y < 10; y += 0.5) {
                    Location beamLoc = originalLoc.clone().add(0, y, 0);
                    player.getWorld().spawnParticle(Particle.END_ROD, beamLoc, 2, 0.1, 0.1, 0.1, 0.01);
                }
                
                // Floating crown effect
                Location crownLoc = originalLoc.clone().add(0, 3 + Math.sin(ticks * 0.2), 0);
                player.getWorld().spawnParticle(Particle.GLOW, crownLoc, 3, 0.2, 0.2, 0.2, 0);
                
                // Play level up sound at intervals
                if (ticks % 40 == 0) {
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
                    player.getWorld().playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.5f, 1.2f);
                }
                
                ticks += 2;
            }
        }.runTaskTimer(plugin, 0, 2);
        
        // Schedule task to track animation end
        int taskId = new BukkitRunnable() {
            @Override
            public void run() {
                endNumberOneAnimation(player);
            }
        }.runTaskLater(plugin, 300).getTaskId();
        
        animationTasks.put(player.getUniqueId(), taskId);
    }
    
    private void endNumberOneAnimation(Player player) {
        if (!player.equals(currentAnimatingPlayer)) return;
        
        // Remove invincibility
        player.setInvulnerable(false);
        
        // Unfreeze player
        player.setWalkSpeed(0.2f);
        player.setAllowFlight(false);
        player.setFlying(false);
        
        // Final explosion
        player.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, player.getLocation().add(0, 2, 0), 2);
        player.getWorld().spawnParticle(Particle.FLASH, player.getLocation().add(0, 2, 0), 3);
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 2, 0), 100, 2, 2, 2, 0.5);
        
        // Victory sound
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 1);
        
        // Clear animation state
        animationInProgress = false;
        currentAnimatingPlayer = null;
        animationTasks.remove(player.getUniqueId());
        
        // Update scoreboard immediately
        updateAllScoreboards();
    }
    
    /**
     * Record a kill
     */
    public void recordKill(Player killer, Player victim) {
        PlayerStats killerStats = getOrCreateStats(killer);
        killerStats.kills++;
        killerStats.score = (killerStats.kills * 3) + (killerStats.assists * 1.5) - (killerStats.deaths * 2);
        
        PlayerStats victimStats = getOrCreateStats(victim);
        victimStats.deaths++;
        victimStats.score = (victimStats.kills * 3) + (victimStats.assists * 1.5) - (victimStats.deaths * 2);
        
        // Check for #1 animation
        checkAndTriggerAnimation(killer);
    }
    
    /**
     * Record an assist
     */
    public void recordAssist(Player assistant) {
        PlayerStats stats = getOrCreateStats(assistant);
        stats.assists++;
        stats.score = (stats.kills * 3) + (stats.assists * 1.5) - (stats.deaths * 2);
    }
    
    private PlayerStats getOrCreateStats(Player player) {
        return playerStats.computeIfAbsent(player.getUniqueId(), 
            k -> new PlayerStats(player.getUniqueId()));
    }
    
    /**
     * Toggle scoreboard for player
     */
    public void toggleScoreboard(Player player) {
        boolean current = scoreboardEnabled.getOrDefault(player.getUniqueId(), true);
        scoreboardEnabled.put(player.getUniqueId(), !current);
        
        if (!current) {
            // Enable - show scoreboard
            updateAllScoreboards();
            player.sendMessage("§aScoreboard enabled!");
        } else {
            // Disable - clear scoreboard
            player.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
            player.sendMessage("§cScoreboard disabled!");
        }
    }
    
    /**
     * Check if scoreboard is enabled for player
     */
    public boolean isScoreboardEnabled(Player player) {
        return scoreboardEnabled.getOrDefault(player.getUniqueId(), true);
    }
    
    // Inner class for player statistics
    private class PlayerStats {
        UUID playerId;
        int kills = 0;
        int deaths = 0;
        int assists = 0;
        double score = 0;
        boolean wasNumberOne = false;
        
        PlayerStats(UUID playerId) {
            this.playerId = playerId;
        }
    }
    
    // Inner class for team scores
    private class TeamScore {
        Team team;
        double totalScore;
        List<PlayerStats> members;
        
        TeamScore(Team team, double totalScore, List<PlayerStats> members) {
            this.team = team;
            this.totalScore = totalScore;
            this.members = members;
        }
        
        List<PlayerStats> getTopMembers(int count) {
            members.sort((a, b) -> Double.compare(b.score, a.score));
            return members.size() > count ? members.subList(0, count) : members;
        }
    }
            }
