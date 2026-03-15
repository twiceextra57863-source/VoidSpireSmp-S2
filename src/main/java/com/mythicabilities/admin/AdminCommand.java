package com.mythicabilities.admin;

import com.mythicabilities.MythicAbilities;
import com.mythicabilities.abilities.Ability;
import com.mythicabilities.utils.TitleBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.*;

public class AdminCommand implements CommandExecutor, TabCompleter {
    
    private final MythicAbilities plugin;
    private final SMPManager smpManager;
    private final WorldBorderManager borderManager;
    private final Map<UUID, Boolean> adminPanelOpen = new HashMap<>();
    
    private boolean smpActive = false;
    private int graceTimeLeft = 0;
    private BukkitTask smpTask = null;
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
                } else if (args[1].equalsIgnoreCase("center")) {
                    showBorderCenter(player);
                } else if (args[1].equalsIgnoreCase("reset")) {
                    resetWorldBorder(player);
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
                
            case "event":
                if (args.length == 1) {
                    showEventHelp(player);
                } else if (args[1].equalsIgnoreCase("start")) {
                    if (args.length >= 3) {
                        try {
                            int seconds = Integer.parseInt(args[2]);
                            String ability = (args.length >= 4) ? args[3] : "random";
                            startAbilityEvent(player, seconds, ability);
                        } catch (NumberFormatException e) {
                            player.sendMessage("§cInvalid time format!");
                        }
                    } else {
                        player.sendMessage("§cUsage: /smp event start <seconds> [ability]");
                    }
                } else if (args[1].equalsIgnoreCase("stop")) {
                    stopAbilityEvent(player);
                } else if (args[1].equalsIgnoreCase("status")) {
                    showEventStatus(player);
                }
                break;
                
            case "force":
                if (args.length == 1) {
                    showForceHelp(player);
                } else if (args[1].equalsIgnoreCase("on")) {
                    setForceGive(player, true);
                } else if (args[1].equalsIgnoreCase("off")) {
                    setForceGive(player, false);
                } else if (args[1].equalsIgnoreCase("status")) {
                    showForceStatus(player);
                }
                break;
                
            case "testcombo":
                testCombo(player);
                break;
                
            case "help":
                showHelp(player);
                break;
                
            default:
                player.sendMessage("§cUnknown command! Use §e/smp help §cfor commands.");
                break;
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (!(sender instanceof Player) || !sender.hasPermission("mythicabilities.admin")) {
            return completions;
        }
        
        if (args.length == 1) {
            // First argument - main commands
            String partial = args[0].toLowerCase();
            List<String> commands = Arrays.asList(
                "start", "stop", "status", "set", "border", "pvp", 
                "grace", "event", "force", "testcombo", "help"
            );
            
            for (String cmd : commands) {
                if (cmd.startsWith(partial)) {
                    completions.add(cmd);
                }
            }
            
        } else if (args.length == 2) {
            // Second argument - subcommands
            if (args[0].equalsIgnoreCase("set")) {
                String partial = args[1].toLowerCase();
                List<String> settings = Arrays.asList("pvp", "border", "grace");
                
                for (String setting : settings) {
                    if (setting.startsWith(partial)) {
                        completions.add(setting);
                    }
                }
                
            } else if (args[0].equalsIgnoreCase("border")) {
                String partial = args[1].toLowerCase();
                List<String> borderCmds = Arrays.asList("set", "center", "reset");
                
                for (String cmd : borderCmds) {
                    if (cmd.startsWith(partial)) {
                        completions.add(cmd);
                    }
                }
                
            } else if (args[0].equalsIgnoreCase("grace")) {
                // Suggest time values
                String partial = args[1].toLowerCase();
                List<String> times = Arrays.asList("30", "60", "120", "300");
                
                for (String time : times) {
                    if (time.startsWith(partial)) {
                        completions.add(time);
                    }
                }
                
            } else if (args[0].equalsIgnoreCase("event")) {
                String partial = args[1].toLowerCase();
                List<String> eventCmds = Arrays.asList("start", "stop", "status");
                
                for (String cmd : eventCmds) {
                    if (cmd.startsWith(partial)) {
                        completions.add(cmd);
                    }
                }
                
            } else if (args[0].equalsIgnoreCase("force")) {
                String partial = args[1].toLowerCase();
                List<String> forceCmds = Arrays.asList("on", "off", "status");
                
                for (String cmd : forceCmds) {
                    if (cmd.startsWith(partial)) {
                        completions.add(cmd);
                    }
                }
            }
            
        } else if (args.length == 3) {
            // Third argument - values
            if (args[0].equalsIgnoreCase("set") && args[1].equalsIgnoreCase("pvp")) {
                String partial = args[2].toLowerCase();
                List<String> bools = Arrays.asList("true", "false");
                
                for (String bool : bools) {
                    if (bool.startsWith(partial)) {
                        completions.add(bool);
                    }
                }
                
            } else if (args[0].equalsIgnoreCase("border") && args[1].equalsIgnoreCase("set")) {
                // Suggest border sizes
                String partial = args[2].toLowerCase();
                List<String> sizes = Arrays.asList("20", "100", "500", "1000", "5000", "10000", "20000");
                
                for (String size : sizes) {
                    if (size.startsWith(partial)) {
                        completions.add(size);
                    }
                }
                
            } else if (args[0].equalsIgnoreCase("event") && args[1].equalsIgnoreCase("start")) {
                // Suggest ability names
                String partial = args[2].toLowerCase();
                completions.add("random");
                
                for (String abilityName : plugin.getAbilityManager().getAllAbilities().keySet()) {
                    if (abilityName.toLowerCase().startsWith(partial)) {
                        completions.add(abilityName);
                    }
                }
            }
        }
        
        return completions;
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
    
    private void resetWorldBorder(Player player) {
        borderManager.resetBorder();
        player.sendMessage("§a✅ World border reset to default!");
    }
    
    private void showBorderCenter(Player player) {
        Location center = borderManager.getCenter();
        if (center != null) {
            player.sendMessage("§aBorder Center: §eX: " + center.getBlockX() + " Z: " + center.getBlockZ());
        } else {
            player.sendMessage("§cBorder center not set!");
        }
    }
    
    private void showBorderMenu(Player player) {
        player.sendMessage("§6§l╔═══════════ BORDER CONTROLS ═══════════╗");
        player.sendMessage("§6║ §e/smp border set <size> §7- Set border size");
        player.sendMessage("§6║ §e/smp border center §7- Show border center");
        player.sendMessage("§6║ §e/smp border reset §7- Reset to default");
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
            
            if (smpActive) {
                if (smpTask != null) {
                    smpTask.cancel();
                }
                startSMPScheduler(player);
            }
        }
    }
    
    // Event Methods
    private void startAbilityEvent(Player player, int seconds, String abilityName) {
        // Check if ability exists (if not random)
        if (!abilityName.equalsIgnoreCase("random")) {
            Ability ability = plugin.getAbilityManager().getAbility(abilityName);
            if (ability == null) {
                player.sendMessage("§cAbility not found: " + abilityName);
                player.sendMessage("§7Use §e/giveability list §7to see all abilities");
                return;
            }
        }
        
        // Start event
        plugin.getForceAbilityManager().startEvent(seconds, abilityName);
        
        player.sendMessage("§a✅ Ability event started!");
        player.sendMessage("§eTime: §a" + seconds + " seconds");
        player.sendMessage("§eAbility: §a" + (abilityName.equalsIgnoreCase("random") ? "Random" : abilityName));
    }
    
    private void stopAbilityEvent(Player player) {
        plugin.getForceAbilityManager().stopEvent();
        player.sendMessage("§c✅ Ability event stopped!");
    }
    
    private void showEventStatus(Player player) {
        ForceAbilityManager fam = plugin.getForceAbilityManager();
        
        player.sendMessage("§6§l╔═══════════ EVENT STATUS ═══════════╗");
        player.sendMessage("§6║ §eActive: " + (fam.isEventActive() ? "§aYES" : "§cNO"));
        if (fam.isEventActive()) {
            player.sendMessage("§6║ §eTime Left: §a" + formatTime(fam.getEventTimer()));
            String abilityDisplay = fam.getEventAbility().equalsIgnoreCase("random") ? "§dRandom" : 
                plugin.getAbilityManager().getAbility(fam.getEventAbility()).getDisplayName();
            player.sendMessage("§6║ §eAbility: " + abilityDisplay);
        }
        player.sendMessage("§6╚════════════════════════════════════╝");
    }
    
    private void setForceGive(Player player, boolean enabled) {
        plugin.getConfig().set("force_ability.enabled", enabled);
        plugin.getConfig().set("force_ability.give_on_first_join", enabled);
        plugin.saveConfig();
        plugin.getForceAbilityManager().reloadConfig();
        
        player.sendMessage("§a✅ Force ability give " + (enabled ? "ENABLED" : "DISABLED"));
        player.sendMessage("§ePlayers will " + (enabled ? "now" : "not") + " receive abilities on first join");
    }
    
    private void showForceStatus(Player player) {
        ForceAbilityManager fam = plugin.getForceAbilityManager();
        
        player.sendMessage("§6§l╔═══════════ FORCE GIVE STATUS ═══════════╗");
        player.sendMessage("§6║ §eEnabled: " + (fam.shouldGiveOnFirstJoin() ? "§aYES" : "§cNO"));
        player.sendMessage("§6║ §eFirst Join Give: " + (fam.shouldGiveOnFirstJoin() ? "§aYES" : "§cNO"));
        player.sendMessage("§6╚══════════════════════════════════════════╝");
    }
    
    // TEST COMBO METHOD - New method to test ability system
    private void testCombo(Player player) {
        String abilityName = plugin.getAbilityManager().getPlayerAbility(player);
        
        player.sendMessage("§6§l╔═══════════ COMBO TEST ═══════════╗");
        
        if (abilityName == null) {
            player.sendMessage("§6║ §c❌ You don't have an ability!");
            player.sendMessage("§6║ §eUse §a/giveability <ability> §eto get one");
            player.sendMessage("§6╚══════════════════════════════════╝");
            return;
        }
        
        Ability ability = plugin.getAbilityManager().getAbility(abilityName);
        int currentCombo = plugin.getAbilityManager().getCombo(player);
        boolean onCooldown = plugin.getCooldownManager().isOnCooldown(player, abilityName);
        long remainingCooldown = plugin.getCooldownManager().getRemainingCooldown(player, abilityName);
        
        player.sendMessage("§6║ §eYour Ability: " + ability.getDisplayName());
        player.sendMessage("§6║ §eAbility Name: §f" + abilityName);
        player.sendMessage("§6║ §eCooldown: §f" + ability.getCooldown() + " seconds");
        player.sendMessage("§6║ ");
        player.sendMessage("§6║ §eCurrent Combo: §f" + currentCombo + "§e/3");
        player.sendMessage("§6║ §eOn Cooldown: " + (onCooldown ? "§cYES" : "§aNO"));
        if (onCooldown) {
            player.sendMessage("§6║ §eRemaining: §f" + remainingCooldown + "s");
        }
        player.sendMessage("§6║ ");
        
        // Test manual trigger
        player.sendMessage("§6║ §eTesting manual trigger...");
        if (onCooldown) {
            player.sendMessage("§6║ §c❌ Cannot trigger - on cooldown");
        } else {
            player.sendMessage("§6║ §a✅ Triggering ability now!");
            ability.onTrigger(player);
            plugin.getCooldownManager().setCooldown(player, abilityName, ability.getCooldown());
            plugin.getAbilityManager().resetCombo(player);
        }
        
        player.sendMessage("§6╚══════════════════════════════════╝");
    }
    
    private void showEventHelp(Player player) {
        player.sendMessage("§6§l╔═══════════ EVENT COMMANDS ═══════════╗");
        player.sendMessage("§6║ §e/smp event start <seconds> [ability]");
        player.sendMessage("§6║ §7  - Start ability event");
        player.sendMessage("§6║ §7  Example: §e/smp event start 60 random");
        player.sendMessage("§6║ §7  Example: §e/smp event start 120 void_walker");
        player.sendMessage("§6║ §e/smp event stop §7- Stop current event");
        player.sendMessage("§6║ §e/smp event status §7- Show event status");
        player.sendMessage("§6╚══════════════════════════════════════════╝");
    }
    
    private void showForceHelp(Player player) {
        player.sendMessage("§6§l╔═══════════ FORCE GIVE COMMANDS ═══════════╗");
        player.sendMessage("§6║ §e/smp force on §7- Enable force ability give");
        player.sendMessage("§6║ §e/smp force off §7- Disable force ability give");
        player.sendMessage("§6║ §e/smp force status §7- Show current status");
        player.sendMessage("§6╚══════════════════════════════════════════════╝");
    }
    
    private void showHelp(Player player) {
        player.sendMessage("§6§l╔═══════════ SMP ADMIN COMMANDS ═══════════╗");
        player.sendMessage("§6║ §e/smp start §7- Start SMP with 20x20 border");
        player.sendMessage("§6║ §e/smp stop §7- Stop SMP");
        player.sendMessage("§6║ §e/smp status §7- Show current status");
        player.sendMessage("§6║ §e/smp set pvp <true/false> §7- Set PVP");
        player.sendMessage("§6║ §e/smp set border <size> §7- Set border");
        player.sendMessage("§6║ §e/smp set grace <seconds> §7- Set grace period");
        player.sendMessage("§6║ §e/smp border set <size> §7- Change border");
        player.sendMessage("§6║ §e/smp border center §7- Show border center");
        player.sendMessage("§6║ §e/smp border reset §7- Reset border");
        player.sendMessage("§6║ §e/smp pvp §7- Toggle PVP");
        player.sendMessage("§6║ §e/smp grace <seconds> §7- Set grace time");
        player.sendMessage("§6║ §e/smp event §7- Ability event commands");
        player.sendMessage("§6║ §e/smp force §7- Force give controls");
        player.sendMessage("§6║ §e/smp testcombo §7- Test combo system");
        player.sendMessage("§6║ §e/admin §7- Open admin GUI");
        player.sendMessage("§6║ §e/giveability <player> <ability> §7- Give ability");
        player.sendMessage("§6╚═══════════════════════════════════════════╝");
    }
    
    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }
    
    // Getters
    public boolean isSMPActive() { return smpActive; }
    public boolean isPVPEnabled() { return pvpEnabled; }
    public int getGraceTimeLeft() { return graceTimeLeft; }
}
