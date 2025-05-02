package dev.by1337.hider.world.sync;

import dev.by1337.hider.world.memory.MemoryWorld;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class WorldSynchronizer implements Listener {
    private final Plugin plugin;
    private final Map<String, ChunkDataSynchronizer> memoryWorlds;

    public WorldSynchronizer(final Plugin plugin) {
        this.plugin = plugin;
        memoryWorlds = new HashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        for (World world : Bukkit.getWorlds()) {
            addWorld(world);
        }
    }

    public long sizeOf() {
        long size = 0;
        for (ChunkDataSynchronizer value : memoryWorlds.values()) {
            size += value.sizeOf();
        }
        return size;
    }
    public int chunkCount(){
        int x = 0;
        for (ChunkDataSynchronizer value : memoryWorlds.values()) {
            x += value.chunkCount();
        }
        return x;
    }

    public Collection<ChunkDataSynchronizer> values(){
        return memoryWorlds.values();
    }

    @Nullable
    public MemoryWorld getMemoryWorld(final String worldName) {
        var v = memoryWorlds.get(worldName);
        if (v == null) {
            return null;
        }
        return v.memoryWorld();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onWorldLoadEvent(final WorldLoadEvent event) {
        addWorld(event.getWorld());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onWorldUnloadEvent(final WorldUnloadEvent event) {
        removeWorld(event.getWorld());
    }

    private void addWorld(World world){
        if (memoryWorlds.containsKey(world.getName()))return;
        memoryWorlds.put(world.getName(), new ChunkDataSynchronizer(plugin, world));
    }

    private void removeWorld(World world){
        var v = memoryWorlds.remove(world.getName());
        v.close();
    }

    public void close(){
        HandlerList.unregisterAll(this);
        memoryWorlds.values().forEach(ChunkDataSynchronizer::close);
        memoryWorlds.clear();
    }
}
