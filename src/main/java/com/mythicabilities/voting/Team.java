package com.mythicabilities.voting;

import com.mythicabilities.MythicAbilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;

public class Team {
    private final Player leader;
    private final List<UUID> members;
    private String teamName;
    private ChatColor teamColor;
    private String bannerType;
    private String katanaType;
    private final Random random = new Random();
    
    // 15 Katana types
    public static final String[] KATANA_NAMES = {
        "§bStorm Breaker",
        "§cFlame Dragon",
        "§dShadow Dancer",
        "§aWind Cutter",
        "§6Earth Shaker",
        "§9Frost Bite",
        "§5Void Walker",
        "§eSun Slash",
        "§2Nature's Fang",
        "§7Stone Edge",
        "§3Wave Splitter",
        "§4Blood Moon",
        "§dSoul Reaper",
        "§6Thunder God",
        "§bCelestial Blade"
    };
    
    public Team(Player leader, List<UUID> voters) {
        this.leader = leader;
        this.members = new ArrayList<>();
        this.members.add(leader.getUniqueId());
        if (voters != null) {
            this.members.addAll(voters);
        }
        this.teamName = leader.getName() + "'s Kingdom";
        this.teamColor = getRandomColor();
        this.bannerType = getRandomBanner();
        this.katanaType = KATANA_NAMES[random.nextInt(KATANA_NAMES.length)];
        
        // Give leader katana
        giveLeaderKatana();
    }
    
    private ChatColor getRandomColor() {
        ChatColor[] colors = {
            ChatColor.RED, ChatColor.BLUE, ChatColor.GREEN, 
            ChatColor.YELLOW, ChatColor.LIGHT_PURPLE, ChatColor.AQUA
        };
        return colors[random.nextInt(colors.length)];
    }
    
    private String getRandomBanner() {
        String[] banners = {
            "CREEPER", "SKULL", "FLOWER", "MOJANG", "PIGLIN"
        };
        return banners[random.nextInt(banners.length)];
    }
    
    private void giveLeaderKatana() {
        // This will be handled by KatanaManager
        // Give custom katana item with abilities
    }
    
    public boolean isMember(Player player) {
        return members.contains(player.getUniqueId());
    }
    
    public boolean isLeader(Player player) {
        return leader.getUniqueId().equals(player.getUniqueId());
    }
    
    public void setTeamName(String name) {
        this.teamName = name;
        broadcast("§aTeam name changed to: §e" + name);
    }
    
    public void setTeamColor(ChatColor color) {
        this.teamColor = color;
        broadcast("§aTeam color changed!");
    }
    
    public void setBanner(String banner) {
        this.bannerType = banner;
        broadcast("§aTeam banner changed!");
    }
    
    private void broadcast(String message) {
        for (UUID uuid : members) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.sendMessage(teamColor + "[" + teamName + "] §f" + message);
            }
        }
    }
    
    // Getters
    public Player getLeader() { return leader; }
    public String getTeamName() { return teamName; }
    public ChatColor getTeamColor() { return teamColor; }
    public String getBannerType() { return bannerType; }
    public String getKatanaType() { return katanaType; }
    public List<Player> getMembers() {
        List<Player> onlineMembers = new ArrayList<>();
        for (UUID uuid : members) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) onlineMembers.add(p);
        }
        return onlineMembers;
    }
}
