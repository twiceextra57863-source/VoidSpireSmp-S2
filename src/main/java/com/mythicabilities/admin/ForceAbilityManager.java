package com.mythicabilities.admin;

import com.mythicabilities.MythicAbilities;
import com.mythicabilities.abilities.Ability;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.*;

public class ForceAbilityManager {
    
    private final MythicAbilities plugin;
    private boolean forceEnabled;
    private boolean giveOnFirstJoin;
    private boolean eventActive;
    private int eventTimer;
    private String eventAbility;
    private boolean eventBroadcast;
    private BukkitTask eventTask;
    private final Random random = new Random();
    
    public ForceAbilityManager(MythicAbilities plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    private void loadConfig() {
        this.forceEnabled = plugin.getConfig().getBoolean("force_ability.enabled", true);
        this.giveOnFirstJoin = plugin.getConfig().getBoolean("force_ability.give_on_first_join", true);
        this.eventActive = plugin.getConfig().getBoolean("force_ability.give_on_event", false);
        this.eventTimer = plugin.getConfig().getInt("force_ability.event_timer", 0);
        this.eventAbility = plugin.getConfig().getString("force_ability.event_ability", "random");
        this.eventBroadcast = plugin.getConfig().getBoolean("force_ability.event_broadcast", true);
    }
    
    public void reloadConfig() {
        plugin.reloadConfig();
        loadConfig();
    }
    
    // Check if player should get ability on first join
    public boolean shouldGiveOnFirstJoin() {
        return forceEnabled && giveOnFirstJoin;
    }
    
    // Give ability to player (with or without spin)
    public void giveAbilityToPlayer(Player player, String abilityName, boolean useSpin) {
        if (useSpin) {
            // Use visual spin
            plugin.getSpinGUI().startVisualSpin(player);
        } else {
            // Direct give without spin
            giveAbilityDirect(player, abilityName);
        }
    }
    
    // Direct ability give without spin
    private void giveAbilityDirect(Player player, String abilityName) {
        String finalAbility = abilityName;
        
        if (abilityName.equalsIgnoreCase("random")) {
            // Get random ability
            List<String> abilities = new ArrayList<>(plugin.getAbilityManager().getAllAbilities().keySet());
            if (abilities.isEmpty()) {
                player.sendMessage(Component.text("§cNo abilities available!"));
                return;
            }
            finalAbility = abilities.get(random.nextInt(abilities.size()));
        }
        
        Ability ability = plugin.getAbilityManager().getAbility(finalAbility);
        if (ability == null) {
            player.sendMessage(Component.text("§cAbility not found: " + finalAbility));
            return;
        }
        
        // Give ability
        plugin.getAbilityManager().setPlayerAbility(player, finalAbility);
        
        // Effects
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
        player.spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 50, 0.5, 0.5, 0.5, 0.5);
        
        player.sendMessage(Component.text("§a§l✦ ABILITY RECEIVED! ✦"));
        player.sendMessage(Component.text("§eYou received: " + ability.getDisplayName()));
    }
    
    // Start event timer
    public void startEvent(int seconds, String abilityName) {
        // Cancel existing event
        if (eventTask != null) {
            eventTask.cancel();
        }
        
        this.eventTimer = seconds;
        this.eventAbility = abilityName;
        this.eventActive = true;
        
        // Save to config
        plugin.getConfig().set("force_ability.give_on_event", true);
        plugin.getConfig().set("force_ability.event_timer", seconds);
        plugin.getConfig().set("force_ability.event_ability", abilityName);
        plugin.saveConfig();
        
        // Broadcast event start
        if (eventBroadcast) {
            String abilityDisplay = abilityName.equalsIgnoreCase("random") ? "§dRANDOM ABILITY" : 
                plugin.getAbilityManager().getAbility(abilityName).getDisplayName();
            
            Bukkit.broadcast(Component.text("§6§l✦✦✦ ABILITY EVENT STARTED! ✦✦✦"));
            Bukkit.broadcast(Component.text("§eAll players will receive: " + abilityDisplay));
            Bukkit.broadcast(Component.text("§eTime remaining: §a" + formatTime(seconds)));
        }
        
        // Start countdown
        eventTask = new BukkitRunnable() {
            int timeLeft = seconds;
            
            @Override
            public void run() {
                if (timeLeft <= 0) {
                    // Event complete - give abilities
                    giveEventAbilities();
                    this.cancel();
                    return;
                }
                
                // Update action bar for all players
                for (Player p : Bukkit.getOnlinePlayers()) {
                    sendEventTimerBar(p, timeLeft, abilityName);
                }
                
                // Broadcast at specific intervals
                if (eventBroadcast) {
                    if (timeLeft == 60 || timeLeft == 30 || timeLeft == 10 || timeLeft == 5) {
                        Bukkit.broadcast(Component.text("§eEvent ends in: §a" + formatTime(timeLeft)));
                    } else if (timeLeft <= 3) {
                        Bukkit.broadcast(Component.text("§c" + timeLeft + "..."));
                    }
                }
                
                timeLeft--;
            }
        }.runTaskTimer(plugin, 0, 20);
    }
    
    private void sendEventTimerBar(Player player, int seconds, String abilityName) {
        // Create progress bar
        int total = eventTimer;
        int progress = (int) ((seconds * 20) / total);
        
        StringBuilder bar = new StringBuilder("§8[");
        for (int i = 0; i < 20; i++) {
            if (i < progress) {
                if (seconds > total/2) bar.append("§a█");
                else if (seconds > total/4) bar.append("§e█");
                else bar.append("§c█");
            } else {
                bar.append("§7█");
            }
        }
        bar.append("§8]");
        
        String abilityDisplay = abilityName.equalsIgnoreCase("random") ? "§dRANDOM" : 
            plugin.getAbilityManager().getAbility(abilityName).getDisplayName();
        
        player.sendActionBar(Component.text(
            "§6Event: " + abilityDisplay + " §7" + bar.toString() + " §e" + formatTime(seconds)
        ));
    }
    
    private void giveEventAbilities() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Check if player already has ability
            String currentAbility = plugin.getAbilityManager().getPlayerAbility(player);
            
            if (currentAbility == null) {
                // Player doesn't have ability - give new one
                giveAbilityDirect(player, eventAbility);
            } else {
                // Player already has ability - optionally give new one or skip
                if (eventAbility.equalsIgnoreCase("random")) {
                    // Give random ability (might be same)
                    giveAbilityDirect(player, eventAbility);
                } else {
                    // Replace with event ability
                    giveAbilityDirect(player, eventAbility);
                }
            }
        }
        
        // Event complete broadcast
        if (eventBroadcast) {
            String abilityDisplay = eventAbility.equalsIgnoreCase("random") ? "§dRANDOM ABILITY" : 
                plugin.getAbilityManager().getAbility(eventAbility).getDisplayName();
            
            Bukkit.broadcast(Component.text("§6§l✦✦✦ EVENT COMPLETE! ✦✦✦"));
            Bukkit.broadcast(Component.text("§aAll players received: " + abilityDisplay));
            
            // Show title to all players
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.showTitle(Title.title(
                    Component.text("§6§lABILITY EVENT!"),
                    Component.text("§aYou received an ability!"),
                    Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(1))
                ));
            }
        }
        
        // Reset event
        eventActive = false;
        eventTimer = 0;
        
        // Update config
        plugin.getConfig().set("force_ability.give_on_event", false);
        plugin.getConfig().set("force_ability.event_timer", 0);
        plugin.saveConfig();
    }
    
    public void stopEvent() {
        if (eventTask != null) {
            eventTask.cancel();
            eventTask = null;
        }
        
        eventActive = false;
        eventTimer = 0;
        
        // Update config
        plugin.getConfig().set("force_ability.give_on_event", false);
        plugin.getConfig().set("force_ability.event_timer", 0);
        plugin.saveConfig();
        
        if (eventBroadcast) {
            Bukkit.broadcast(Component.text("§c§l✦ EVENT CANCELLED ✦"));
        }
    }
    
    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }
    
    public boolean isEventActive() { return eventActive; }
    public int getEventTimer() { return eventTimer; }
    public String getEventAbility() { return eventAbility; }
          }
