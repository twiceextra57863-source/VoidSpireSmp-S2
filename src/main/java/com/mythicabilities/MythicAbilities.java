package com.mythicabilities;

import com.mythicabilities.abilities.*;
import com.mythicabilities.admin.AdminCommand;
import com.mythicabilities.admin.AdminPanelGUI;
import com.mythicabilities.admin.ForceAbilityManager;
import com.mythicabilities.admin.GiveAbilityCommand;
import com.mythicabilities.admin.SMPManager;
import com.mythicabilities.admin.WorldBorderManager;
import com.mythicabilities.listeners.PlayerJoinListener;
import com.mythicabilities.listeners.AbilityUseListener;
import com.mythicabilities.listeners.WorldBorderListener;
import com.mythicabilities.gui.AbilitySpinGUI;
import com.mythicabilities.utils.CooldownManager;
import com.mythicabilities.utils.TitleBar;
import com.mythicabilities.voting.VoteManager;
import com.mythicabilities.voting.KatanaManager;
import com.mythicabilities.voting.VoteCommand;
import com.mythicabilities.voting.TeamCommand;
import com.mythicabilities.voting.KatanaCommand;
import com.mythicabilities.voting.LeaderCommand;
import com.mythicabilities.voting.KatanaListener;
import com.mythicabilities.scoreboard.ScoreboardManager;
import com.mythicabilities.scoreboard.ScoreboardCommand;
import com.mythicabilities.katana.KatanaManager; // NEW: Katana System
import com.mythicabilities.katana.KatanaAdminCommand; // NEW: Katana Admin Command
import com.mythicabilities.katana.KatanaData; // NEW: Katana Data
import org.bukkit.plugin.java.JavaPlugin;

public class MythicAbilities extends JavaPlugin {
    
    private static MythicAbilities instance;
    private AbilityManager abilityManager;
    private CooldownManager cooldownManager;
    private AdminCommand adminCommand;
    private SMPManager smpManager;
    private WorldBorderManager worldBorderManager;
    private AbilitySpinGUI spinGUI;
    private ForceAbilityManager forceAbilityManager;
    
    // Voting System Managers
    private VoteManager voteManager;
    private com.mythicabilities.voting.KatanaManager votingKatanaManager; // Voting katana manager
    
    // Scoreboard Manager
    private ScoreboardManager scoreboardManager;
    
    // NEW: Katana System Manager
    private com.mythicabilities.katana.KatanaManager katanaManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Save default config
        saveDefaultConfig();
        
        // Initialize managers
        this.cooldownManager = new CooldownManager(this);
        this.abilityManager = new AbilityManager();
        this.smpManager = new SMPManager(this);
        this.worldBorderManager = new WorldBorderManager(this);
        this.adminCommand = new AdminCommand(this);
        this.spinGUI = new AbilitySpinGUI(this);
        this.forceAbilityManager = new ForceAbilityManager(this);
        
        // Initialize Voting System
        this.voteManager = new VoteManager(this);
        this.votingKatanaManager = new com.mythicabilities.voting.KatanaManager(this);
        
        // Initialize Scoreboard System
        this.scoreboardManager = new ScoreboardManager(this);
        
        // NEW: Initialize Katana System
        this.katanaManager = new com.mythicabilities.katana.KatanaManager(this);
        
        // Register abilities
        registerAbilities();
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new AbilityUseListener(this), this);
        getServer().getPluginManager().registerEvents(new WorldBorderListener(this), this);
        getServer().getPluginManager().registerEvents(new AdminPanelGUI(this, adminCommand), this);
        getServer().getPluginManager().registerEvents(spinGUI, this);
        
        // Register Voting Listeners
        getServer().getPluginManager().registerEvents(new KatanaListener(this), this);
        
        // Register commands with tab completers
        getCommand("abilities").setExecutor(spinGUI);
        
        // Admin command with tab completer
        getCommand("smp").setExecutor(adminCommand);
        getCommand("smp").setTabCompleter(adminCommand);
        
        getCommand("admin").setExecutor(adminCommand);
        
        // GiveAbility command with tab completer
        GiveAbilityCommand giveAbilityCommand = new GiveAbilityCommand(this);
        getCommand("giveability").setExecutor(giveAbilityCommand);
        getCommand("giveability").setTabCompleter(giveAbilityCommand);
        
        // Register Voting Commands
        getCommand("vote").setExecutor(new VoteCommand(this));
        getCommand("team").setExecutor(new TeamCommand(this));
        getCommand("katana").setExecutor(new KatanaCommand(this)); // Voting katana command
        getCommand("leader").setExecutor(new LeaderCommand(this));
        
        // Register Scoreboard Command
        getCommand("scoreboard").setExecutor(new ScoreboardCommand(this));
        
        // NEW: Register Katana Admin Command
        KatanaAdminCommand katanaAdminCommand = new KatanaAdminCommand(this);
        getCommand("katanadmin").setExecutor(katanaAdminCommand);
        getCommand("katanadmin").setTabCompleter(katanaAdminCommand);
        
        getLogger().info("MythicAbilities has been enabled successfully!");
        getLogger().info("Supporting Minecraft 1.21.11 with latest features!");
        getLogger().info("Loaded " + abilityManager.getAllAbilities().size() + " abilities!");
        getLogger().info("Voting System initialized with 15 legendary katanas!");
        getLogger().info("Scoreboard System initialized with dynamic rankings!");
        getLogger().info("Katana Admin System initialized with 16 legendary katanas!"); // NEW
    }
    
    private void registerAbilities() {
        // Register all abilities
        abilityManager.registerAbility(new InfernoTouch(this));
        abilityManager.registerAbility(new FrostWalker(this));
        abilityManager.registerAbility(new VoidWalker(this));
        abilityManager.registerAbility(new MandelaEffect(this));
        abilityManager.registerAbility(new NaturesWrath(this));
        abilityManager.registerAbility(new AerialMace(this));
        abilityManager.registerAbility(new KagurasUmbrella(this));
        abilityManager.registerAbility(new SuyousTranscendence(this));
        abilityManager.registerAbility(new LingsShadow(this));
        abilityManager.registerAbility(new FirstOfStone(this));
        
        getLogger().info("Registered 10 abilities successfully!");
    }
    
    @Override
    public void onDisable() {
        // Clean up any active abilities
        if (forceAbilityManager != null) {
            forceAbilityManager.stopEvent();
        }
        
        getLogger().info("MythicAbilities has been disabled!");
    }
    
    public static MythicAbilities getInstance() {
        return instance;
    }
    
    public AbilityManager getAbilityManager() {
        return abilityManager;
    }
    
    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }
    
    public AdminCommand getAdminCommand() {
        return adminCommand;
    }
    
    public SMPManager getSmpManager() {
        return smpManager;
    }
    
    public WorldBorderManager getWorldBorderManager() {
        return worldBorderManager;
    }
    
    public AbilitySpinGUI getSpinGUI() {
        return spinGUI;
    }
    
    public ForceAbilityManager getForceAbilityManager() {
        return forceAbilityManager;
    }
    
    // Voting System Getters
    public VoteManager getVoteManager() {
        return voteManager;
    }
    
    public com.mythicabilities.voting.KatanaManager getVotingKatanaManager() {
        return votingKatanaManager;
    }
    
    // Scoreboard System Getter
    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }
    
    // NEW: Katana System Getter
    public com.mythicabilities.katana.KatanaManager getKatanaManager() {
        return katanaManager;
    }
}
