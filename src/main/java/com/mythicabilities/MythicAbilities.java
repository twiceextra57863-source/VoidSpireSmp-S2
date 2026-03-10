package com.mythicabilities;

import com.mythicabilities.abilities.AbilityManager;
import com.mythicabilities.abilities.InfernoTouch;
import com.mythicabilities.abilities.FrostWalker;
import com.mythicabilities.admin.AdminCommand;
import com.mythicabilities.admin.AdminPanelGUI;
import com.mythicabilities.admin.SMPManager;
import com.mythicabilities.admin.WorldBorderManager;
import com.mythicabilities.listeners.PlayerJoinListener;
import com.mythicabilities.listeners.AbilityUseListener;
import com.mythicabilities.listeners.WorldBorderListener;
import com.mythicabilities.gui.AbilitySpinGUI;
import com.mythicabilities.utils.CooldownManager;
import org.bukkit.plugin.java.JavaPlugin;

public class MythicAbilities extends JavaPlugin {
    
    private static MythicAbilities instance;
    private AbilityManager abilityManager;
    private CooldownManager cooldownManager;
    private AdminCommand adminCommand;
    private SMPManager smpManager;
    private WorldBorderManager worldBorderManager;
    
    @Override
    public void onEnable() {
        instance = this;
        
        // Save default config
        saveDefaultConfig();
        
        // Initialize managers
        this.cooldownManager = new CooldownManager();
        this.abilityManager = new AbilityManager();
        this.smpManager = new SMPManager(this);
        this.worldBorderManager = new WorldBorderManager(this);
        this.adminCommand = new AdminCommand(this);
        
        // Register abilities
        registerAbilities();
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new AbilityUseListener(this), this);
        getServer().getPluginManager().registerEvents(new WorldBorderListener(this), this);
        getServer().getPluginManager().registerEvents(new AdminPanelGUI(this, adminCommand), this);
        
        // Register commands
        getCommand("abilities").setExecutor(new AbilitySpinGUI(this));
        getCommand("smp").setExecutor(adminCommand);
        getCommand("admin").setExecutor(adminCommand);
        getCommand("giveability").setExecutor(new GiveAbilityCommand(this));
        
        getLogger().info("MythicAbilities has been enabled successfully!");
        getLogger().info("Supporting Minecraft 1.21.11 with latest features!");
    }
    
    private void registerAbilities() {
        abilityManager.registerAbility(new InfernoTouch(this));
        abilityManager.registerAbility(new FrostWalker(this));
        // More abilities will be added here
    }
    
    @Override
    public void onDisable() {
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
}
