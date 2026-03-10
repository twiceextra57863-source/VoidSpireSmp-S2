package com.mythicabilities.utils;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class ParticleEffects {
    
    public static void createSpiral(Location center, Particle particle, int count, double radius) {
        World world = center.getWorld();
        if (world == null) return;
        
        for (int i = 0; i < 360; i += 360 / count) {
            double angle = Math.toRadians(i);
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);
            
            Location loc = new Location(world, x, center.getY(), z);
            world.spawnParticle(particle, loc, 1, 0, 0, 0, 0);
        }
    }
    
    public static void createBeam(Location start, Location end, Particle particle) {
        World world = start.getWorld();
        if (world == null) return;
        
        Vector direction = end.toVector().subtract(start.toVector()).normalize();
        double distance = start.distance(end);
        
        for (double d = 0; d < distance; d += 0.5) {
            Location point = start.clone().add(direction.clone().multiply(d));
            world.spawnParticle(particle, point, 1, 0, 0, 0, 0);
        }
    }
    
    public static void createSphere(Location center, Particle particle, double radius) {
        World world = center.getWorld();
        if (world == null) return;
        
        for (int i = 0; i < 50; i++) {
            double u = Math.random();
            double v = Math.random();
            
            double theta = 2 * Math.PI * u;
            double phi = Math.acos(2 * v - 1);
            
            double x = radius * Math.sin(phi) * Math.cos(theta);
            double y = radius * Math.sin(phi) * Math.sin(theta);
            double z = radius * Math.cos(phi);
            
            Location loc = center.clone().add(x, y, z);
            world.spawnParticle(particle, loc, 1, 0, 0, 0, 0);
        }
    }
}
