package com.mythicabilities.admin;

import com.mythicabilities.MythicAbilities;
import com.mythicabilities.utils.TitleBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AdminCommand implements CommandExecutor {
    
    private final MythicAbilities plugin;
    private final SMPManager smpManager;
    private final WorldBorderManager borderManager;
    private final Map<UUID, Boolean> adminPanelOpen = new HashMap<>();
    
    private boolean smpActive = false;
    private int graceTimeLeft = 0;
    private BukkitTask smpTask = null; // Changed from int to BukkitTask
    private boolean pvpEnabled = false;
    
    public AdminCommand(MythicAbilities plugin) {
        this.plugin = plugin;
        this.smpManager = new SMPManager(plugin);
        this.borderManager = new WorldBorderManager(plugin);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }
        
        if (!player.hasPermission("mythicabilities.admin")) {
            player.sendMessage(Component.text("§cYou don't have permission!"));
            return true;
        }
        
        if (args.length == 0) {
            openAdminPanel(player);
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "start":
                startSMP(player);
                break;
                
            case "stop":
                stopSMP(player);
                break;
                
            case "status":
                showStatus(player);
                break;
                
            case "set":
                if (args.length < 3) {
                    player.sendMessage("§cUsage: /smp set <pvp|border|grace> <value>");
                    return true;
                }
                handleSetCommand(player, args[1], args[2]);
                break;
                
            case "border":
                if (args.length == 1) {
                    showBorderMenu(player);
                } else if (args[1].equalsIgnoreCase("set")) {
                    if (args.length == 3) {
                        try {
                            int size = Integer.parseInt(args[2]);
                            setWorldBorder(player, size);
                        } catch (NumberFormatException e) {
                            player.sendMessage("§cInvalid size! Use numbers.");
                        }
                    }
                }
                break;
                
            case "pvp":
                togglePVP(player);
                break;
                
            case "grace":
                if (args.length == 2) {
                    try {
                        int seconds = Integer.parseInt(args[1]);
                        setGracePeriod(player, seconds);
                    } catch (NumberFormatException e) {
                        player.sendMessage("§cInvalid time!");
                    }
                } else {
                    toggleGrace(player);
                }
                break;
                
            default:
                player.sendMessage("§cUnknown command! Use: /smp <start|stop|status|set|border|pvp|grace>");
                break;
        }
        
        return true;
    }
    
    private void openAdminPanel(Player player) {
        AdminPanelGUI gui = new AdminPanelGUI(plugin, this);
        gui.openMainPanel(player);
    }
    
    private void startSMP(Player player) {
        if (smpActive) {
            player.sendMessage("§cSMP is already active!");
            return;
        }
        
        smpActive = true;
        graceTimeLeft = 60; // 60 seconds grace period
        
        // Teleport all players to spawn
        World world = player.getWorld();
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.teleport(world.getSpawnLocation());
        }
        
        // Set initial 20x20 border
        borderManager.setBorder(20, world.getSpawnLocation());
        
        // Start countdown timer
        startSMPScheduler(player);
        
        // Broadcast start message
        Bukkit.broadcast(Component.text("§6§l✦✦✦ SMP HAS STARTED! ✦✦✦"));
        Bukkit.broadcast(Component.text("§eGrace Period: §a60 seconds"));
        Bukkit.broadcast(Component.text("§eWorld Border: §a20x20"));
        Bukkit.broadcast(Component.text("§ePVP: §cDisabled"));
        
        // Personal admin message
        player.sendMessage(Component.text("§a✅ SMP Started Successfully!"));
        player.sendMessage(Component.text("§7Use §e/admin §7to control settings"));
    }
    
    private void startSMPScheduler(Player admin) {
        smpTask = new BukkitRunnable() {
            int timeLeft = graceTimeLeft;
            
            @Override
            public void run() {
                if (timeLeft <= 0) {
                    // Grace period ended - expand border
                    expandWorldBorder();
                    this.cancel();
                    return;
                }
                
                // Update title bar for all players
                for (Player p : Bukkit.getOnlinePlayers()) {
                    TitleBar.sendTimerBar(p, timeLeft);
                }
                
                // Special messages at certain times
                if (timeLeft == 30) {
                    Bukkit.broadcast(Component.text("§e§l⚠ 30 seconds until border expands!"));
                } else if (timeLeft == 10) {
                    Bukkit.broadcast(Component.text("§c§l⚠ 10 seconds remaining!"));
                } else if (timeLeft == 5) {
                    Bukkit.broadcast(Component.text("§4§l⚠ 5... 4... 3... 2... 1..."));
                }
                
                timeLeft--;
            }
        }.runTaskTimer(plugin, 0, 20); // Update every second
    }
    
    private void expandWorldBorder() {
        smpActive = true;
        pvpEnabled = true;
        
        // Set new 20k x 20k border
        borderManager.setBorder(20000, Bukkit.getWorlds().get(0).getSpawnLocation());
        
        // Broadcast expansion
        Bukkit.broadcast(Component.text("§6§l✦✦✦ WORLD BORDER EXPANDED! ✦✦✦"));
        Bukkit.broadcast(Component.text("§eNew Border: §a20,000 x 20,000"));
        Bukkit.broadcast(Component.text("§ePVP: §aENABLED"));
        
        // Show title to all players
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.showTitle(Title.title(
                Component.text("§c§lBORDER EXPANDED!"),
                Component.text("§ePVP is now enabled"),
                Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(3), Duration.ofSeconds(1))
            ));
        }
    }
    
    private void stopSMP(Player player) {
        if (!smpActive) {
            player.sendMessage("§cSMP is not active!");
            return;
        }
        
        smpActive = false;
        if (smpTask != null) {
            smpTask.cancel();
            smpTask = null;
        }
        
        // Reset border
        borderManager.resetBorder();
        
        Bukkit.broadcast(Component.text("§c§l✦✦✦ SMP HAS BEEN STOPPED! ✦✦✦"));
        player.sendMessage("§a✅ SMP Stopped Successfully!");
    }
    
    private void showStatus(Player player) {
        player.sendMessage("§6§l╔════════════ SMP STATUS ════════════╗");
        player.sendMessage("§6║ §eStatus: " + (smpActive ? "§aACTIVE" : "§cINACTIVE"));
        player.sendMessage("§6║ §eGrace Period: " + (graceTimeLeft > 0 ? "§a" + graceTimeLeft + "s" : "§cEnded"));
        player.sendMessage("§6║ §ePVP: " + (pvpEnabled ? "§aENABLED" : "§cDISABLED"));
        player.sendMessage("§6║ §eWorld Border: " + (smpActive ? "§a" + borderManager.getCurrentSize() : "§cDefault"));
        player.sendMessage("§6║ §eOnline Players: §a" + Bukkit.getOnlinePlayers().size());
        player.sendMessage("§6╚══════════════════════════════════════╝");
    }
    
    private void handleSetCommand(Player player, String setting, String value) {
        switch (setting.toLowerCase()) {
            case "pvp":
                pvpEnabled = Boolean.parseBoolean(value);
                player.sendMessage("§a✅ PVP set to: " + (pvpEnabled ? "§aENABLED" : "§cDISABLED"));
                Bukkit.broadcast(Component.text("§ePVP has been " + (pvpEnabled ? "§aenabled" : "§cdisabled")));
                break;
                
            case "border":
                try {
                    int size = Integer.parseInt(value);
                    setWorldBorder(player, size);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cInvalid border size!");
                }
                break;
                
            case "grace":
                try {
                    int seconds = Integer.parseInt(value);
                    setGracePeriod(player, seconds);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cInvalid grace time!");
                }
                break;
                
            default:
                player.sendMessage("§cUnknown setting! Use: pvp, border, grace");
                break;
        }
    }
    
    private void setWorldBorder(Player player, int size) {
        World world = player.getWorld();
        borderManager.setBorder(size, world.getSpawnLocation());
        player.sendMessage("§a✅ World border set to: §e" + size + " x " + size);
        Bukkit.broadcast(Component.text("§eWorld border has been set to §a" + size + "x" + size));
    }
    
    private void showBorderMenu(Player player) {
        player.sendMessage("§6§l╔═══════════ BORDER CONTROLS ═══════════╗");
        player.sendMessage("§6║ §e/border set <size> §7- Set border size");
        player.sendMessage("§6║ §e/border center §7- Show border center");
        player.sendMessage("§6║ §e/border reset §7- Reset to default");
        player.sendMessage("§6║ §eCurrent: §a" + borderManager.getCurrentSize() + " blocks");
        player.sendMessage("§6╚════════════════════════════════════════╝");
    }
    
    private void togglePVP(Player player) {
        pvpEnabled = !pvpEnabled;
        player.sendMessage("§a✅ PVP " + (pvpEnabled ? "§aENABLED" : "§cDISABLED"));
        Bukkit.broadcast(Component.text("§ePVP has been " + (pvpEnabled ? "§aenabled" : "§cdisabled")));
    }
    
    private void setGracePeriod(Player player, int seconds) {
        this.graceTimeLeft = seconds;
        player.sendMessage("§a✅ Grace period set to: §e" + seconds + " seconds");
        
        if (smpActive) {
            // Restart timer with new grace period
            if (smpTask != null) {
                smpTask.cancel();
            }
            startSMPScheduler(player);
        }
    }
    
    private void toggleGrace(Player player) {
        if (graceTimeLeft > 0) {
            graceTimeLeft = 0;
            player.sendMessage("§cGrace period ended manually");
            expandWorldBorder();
        } else {
            graceTimeLeft = 60;
            player.sendMessage("§aGrace period activated: 60 seconds");
        }
    }
    
    // Getters
    public boolean isSMPActive() { return smpActive; }
    public boolean isPVPEnabled() { return pvpEnabled; }
    public int getGraceTimeLeft() { return graceTimeLeft; }
    }
