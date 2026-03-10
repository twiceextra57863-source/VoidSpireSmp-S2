package com.mythicabilities.admin;

import com.mythicabilities.MythicAbilities;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class WorldBorderManager {
    
    private final MythicAbilities plugin;
    private double currentSize = 20000; // Default 20k
    private Location center;
    
    public WorldBorderManager(MythicAbilities plugin) {
        this.plugin = plugin;
    }
    
    public void setBorder(double size, Location center) {
        this.currentSize = size;
        this.center = center;
        
        World world = center.getWorld();
        if (world == null) return;
        
        WorldBorder border = world.getWorldBorder();
        border.setCenter(center);
        border.setSize(size);
        border.setWarningDistance(10);
        border.setDamageBuffer(5);
        border.setDamageAmount(0.2);
        
        // Animate border change
        animateBorder(world, size);
    }
    
    private void animateBorder(World world, double targetSize) {
        WorldBorder border = world.getWorldBorder();
        double current = border.getSize();
        double step = (targetSize - current) / 20; // 1 second animation
        
        new BukkitRunnable() {
            int ticks = 0;
            double newSize = current;
            
            @Override
            public void run() {
                if (ticks >= 20) {
                    border.setSize(targetSize);
                    this.cancel();
                    return;
                }
                
                newSize += step;
                border.setSize(newSize);
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    public void resetBorder() {
        World world = Bukkit.getWorlds().get(0);
        WorldBorder border = world.getWorldBorder();
        border.reset();
        currentSize = border.getSize();
    }
    
    public boolean isOutsideBorder(Player player) {
        Location loc = player.getLocation();
        double borderSize = currentSize / 2;
        
        return Math.abs(loc.getX() - center.getX()) > borderSize ||
               Math.abs(loc.getZ() - center.getZ()) > borderSize;
    }
    
    public void teleportInsideBorder(Player player) {
        if (isOutsideBorder(player)) {
            player.teleport(center);
            player.sendMessage(Component.text("§cYou were teleported inside the border!"));
        }
    }
    
    public double getCurrentSize() { return currentSize; }
    public Location getCenter() { return center; }
}
