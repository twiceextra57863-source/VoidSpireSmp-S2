package com.mythicabilities.voting;

import com.mythicabilities.MythicAbilities;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class Team {
    private final MythicAbilities plugin;
    private final Player leader;
    private final List<UUID> members;
    private String teamName;
    private ChatColor teamColor;
    private String bannerType;
    private String katanaType;
    private final Random random = new Random();
    
    public static final String[] KATANA_NAMES = {
        "Storm Breaker", "Flame Dragon", "Shadow Dancer", "Wind Cutter",
        "Earth Shaker", "Frost Bite", "Void Walker", "Sun Slash",
        "Nature's Fang", "Stone Edge", "Wave Splitter", "Blood Moon",
        "Soul Reaper", "Thunder God", "Celestial Blade"
    };
    
    public Team(MythicAbilities plugin, Player leader, List<UUID> voters) {
        this.plugin = plugin;
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
        
        giveLeaderKatana();
    }
    
    private ChatColor getRandomColor() {
        ChatColor[] colors = {ChatColor.RED, ChatColor.BLUE, ChatColor.GREEN, 
                              ChatColor.YELLOW, ChatColor.LIGHT_PURPLE, ChatColor.AQUA};
        return colors[random.nextInt(colors.length)];
    }
    
    private String getRandomBanner() {
        String[] banners = {"CREEPER", "SKULL", "FLOWER", "MOJANG", "PIGLIN"};
        return banners[random.nextInt(banners.length)];
    }
    
    private void giveLeaderKatana() {
        // Use votingKatanaManager
        ItemStack katana = plugin.getVotingKatanaManager().createBoundKatana(leader, katanaType);
        leader.getInventory().addItem(katana);
        leader.sendMessage(Component.text("§aYou received: " + getKatanaDisplayName()));
    }
    
    private String getKatanaDisplayName() {
        switch (katanaType) {
            case "Storm Breaker": return "§b§l⚡ Storm Breaker ⚡";
            case "Flame Dragon": return "§c§l🔥 Flame Dragon 🔥";
            case "Shadow Dancer": return "§8§l🌑 Shadow Dancer 🌑";
            case "Wind Cutter": return "§a§l💨 Wind Cutter 💨";
            case "Earth Shaker": return "§6§l🌍 Earth Shaker 🌍";
            case "Frost Bite": return "§3§l❄️ Frost Bite ❄️";
            case "Void Walker": return "§5§l🌌 Void Walker 🌌";
            case "Sun Slash": return "§e§l☀️ Sun Slash ☀️";
            case "Nature's Fang": return "§2§l🌿 Nature's Fang 🌿";
            case "Stone Edge": return "§7§l⛰️ Stone Edge ⛰️";
            case "Wave Splitter": return "§9§l🌊 Wave Splitter 🌊";
            case "Blood Moon": return "§4§l🌙 Blood Moon 🌙";
            case "Soul Reaper": return "§d§l💀 Soul Reaper 💀";
            case "Thunder God": return "§6§l⚡ Thunder God ⚡";
            case "Celestial Blade": return "§b§l✨ Celestial Blade ✨";
            default: return "§fKatana";
        }
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
