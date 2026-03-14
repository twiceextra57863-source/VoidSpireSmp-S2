package com.mythicabilities;

import com.mythicabilities.abilities.*;
import com.mythicabilities.admin.AdminCommand;
import com.mythicabilities.admin.AdminPanelGUI;
import com.mythicabilities.admin.GiveAbilityCommand;
import com.mythicabilities.admin.SMPManager;
import com.mythicabilities.admin.WorldBorderManager;
import com.mythicabilities.listeners.PlayerJoinListener;
import com.mythicabilities.listeners.AbilityUseListener;
import com.mythicabilities.listeners.WorldBorderListener;
import com.mythicabilities.gui.AbilitySpinGUI;
import com.mythicabilities.utils.CooldownManager;
import com.mythicabilities.utils.TitleBar;
import org.bukkit.plugin.java.JavaPlugin;

public class MythicAbilities extends JavaPlugin {
    
    private static MythicAbilities instance;
    private AbilityManager abilityManager;
    private CooldownManager cooldownManager;
    private AdminCommand adminCommand;
    private SMPManager smpManager;
    private WorldBorderManager worldBorderManager;
    private AbilitySpinGUI spinGUI;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Save default config
        saveDefaultConfig();
        
        // Initialize managers
        this.cooldownManager = new CooldownManager(this); // Pass plugin instance for cooldown display
        this.abilityManager = new AbilityManager();
        this.smpManager = new SMPManager(this);
        this.worldBorderManager = new WorldBorderManager(this);
        this.adminCommand = new AdminCommand(this);
        this.spinGUI = new AbilitySpinGUI(this);
        
        // Register abilities
        registerAbilities();
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new AbilityUseListener(this), this);
        getServer().getPluginManager().registerEvents(new WorldBorderListener(this), this);
        getServer().getPluginManager().registerEvents(new AdminPanelGUI(this, adminCommand), this);
        getServer().getPluginManager().registerEvents(spinGUI, this); // Register spin GUI listener
        
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
        
        getLogger().info("MythicAbilities has been enabled successfully!");
        getLogger().info("Supporting Minecraft 1.21.11 with latest features!");
        getLogger().info("Loaded " + abilityManager.getAllAbilities().size() + " abilities!");
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
        if (abilityManager != null) {
            // Additional cleanup if needed
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
}
