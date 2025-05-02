package dev.by1337.hider.world.sync;

import dev.by1337.hider.util.UnsafeUtil;
import dev.by1337.hider.world.memory.MemoryChunk;
import dev.by1337.hider.world.memory.MemoryWorld;
import net.minecraft.util.BitStorage;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftChunk;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.Plugin;
import org.by1337.blib.util.invoke.LambdaMetafactoryUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class ChunkDataSynchronizer implements Listener {
    private static final Function<LevelChunk, Map<Heightmap.Types, Heightmap>> HEIGHT_MAP_GETTER;
    private final Plugin plugin;
    private final MemoryWorld memoryWorld;
    private final World world;

    public ChunkDataSynchronizer(Plugin plugin, World world) {
        this.plugin = plugin;
        this.world = world;
        memoryWorld = new MemoryWorld();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        for (@NotNull Chunk loadedChunk : world.getLoadedChunks()) {
            LevelChunk chunk = ((CraftChunk) loadedChunk).getHandle();
            hook(chunk);
        }
    }

    public long sizeOf() {
        return memoryWorld.sizeOf();
    }

    public int chunkCount() {
        return memoryWorld.chunkCount();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onChunkLoad(ChunkLoadEvent event) {
        if (!event.getChunk().getWorld().getName().equals(world.getName())) return;
        LevelChunk chunk = ((CraftChunk) event.getChunk()).getHandle();
        if (chunk == null) return;
        hook(chunk);
    }

    private void hook(LevelChunk chunk) {
        MemoryChunk memoryChunk = memoryWorld.getOrAllocateChunk(chunk.locX, chunk.locZ);
        Map<Heightmap.Types, Heightmap> map = HEIGHT_MAP_GETTER.apply(chunk);
        Heightmap heightmap = map.get(Heightmap.Types.MOTION_BLOCKING);
        ProxyHeightmap proxy;
        if (heightmap == null) {
            proxy = new ProxyHeightmap(chunk, Heightmap.Types.MOTION_BLOCKING);
        } else {
            proxy = ProxyHeightmap.mirrorOf(heightmap, chunk, Heightmap.Types.MOTION_BLOCKING, this);
        }
        map.put(Heightmap.Types.MOTION_BLOCKING, proxy);
        int chunkX = chunk.locX;
        int chunkZ = chunk.locZ;

        if (memoryChunk != null) {
            for (int y = 0; y < 256; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        BlockState blockState = chunk.getBlockData(chunkX << 4 | x, y, chunkZ << 4 | z);
                      //  memoryWorld.setBlockState(chunkX << 4 | x, y, chunkZ << 4 | z, Block.getCombinedId(blockState));
                        memoryChunk.setBlockByLocalXZ(x, y, z, Block.getCombinedId(blockState));
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    void onChunkUnload(ChunkUnloadEvent event) {
        if (!event.getChunk().getWorld().getName().equals(world.getName())) return;
        LevelChunk chunk = ((CraftChunk) event.getChunk()).getHandle();
        if (chunk == null) return;
        unhook(chunk);
    }

    private void unhook(LevelChunk chunk) {
        memoryWorld.freeChunk(chunk.locX, chunk.locZ);
        Map<Heightmap.Types, Heightmap> map = HEIGHT_MAP_GETTER.apply(chunk);
        Heightmap heightmap = map.get(Heightmap.Types.MOTION_BLOCKING);
        if (heightmap instanceof ProxyHeightmap proxy) {
            map.put(Heightmap.Types.MOTION_BLOCKING, proxy.createNormal());
        }
    }

    public void close() {
        memoryWorld.free();
        HandlerList.unregisterAll(this);
        for (@NotNull Chunk loadedChunk : world.getLoadedChunks()) {
            LevelChunk chunk = ((CraftChunk) loadedChunk).getHandle();
            unhook(chunk);
        }
    }

    public MemoryWorld memoryWorld() {
        return memoryWorld;
    }

    @Override
    public String toString() {
        return "ChunkDataSynchronizer{" +
                "memoryWorld=" + memoryWorld +
                ", world=" + world.getName() +
                '}';
    }

    private class ProxyHeightmap extends Heightmap {
        private static final BiConsumer<Heightmap, BitStorage> DATA_SET;
        private static final Function<Heightmap, BitStorage> DATA_GET;
        private static final BiConsumer<Heightmap, Predicate<BlockState>> IS_OPAQUE_SET;
        private static final Function<Heightmap, Predicate<BlockState>> IS_OPAQUE_GET;
        private static final BiConsumer<Heightmap, ChunkAccess> CHUNK_SET;
        private static final Function<Heightmap, ChunkAccess> CHUNK_GET;
        private final LevelChunk levelChunk;
        private final int locX;
        private final int locZ;
        private MemoryChunk lastChunk;

        public ProxyHeightmap(LevelChunk chunkAccess, Types types) {
            super(chunkAccess, types);
            this.levelChunk = chunkAccess;
            locX = levelChunk.locX;
            locZ = levelChunk.locZ;
        }

        public static ProxyHeightmap mirrorOf(Heightmap heightmap, LevelChunk chunkAccess, Types types, ChunkDataSynchronizer chunkDataSynchronizer) {
            ProxyHeightmap proxy = chunkDataSynchronizer.new ProxyHeightmap(chunkAccess, types);
            DATA_SET.accept(proxy, DATA_GET.apply(heightmap));
            return proxy;
        }

        @Override
        public boolean update(int x, int y, int z, BlockState block) {
            if (lastChunk == null || lastChunk.released() || lastChunk.x != locX || lastChunk.z != locZ){
                lastChunk = memoryWorld.getChunk(locX, locZ);
            }
            if (lastChunk != null){
                lastChunk.setBlockByLocalXZ(x, y, z, Block.getCombinedId(block));
            }
          //  memoryWorld.setBlockState(locX << 4 | x, y, locZ << 4 | z, Block.getCombinedId(block));
            return super.update(x, y, z, block);
        }

        public Heightmap createNormal() {
            try {
                Heightmap heightmap = (Heightmap) UnsafeUtil.getUnsafe().allocateInstance(Heightmap.class);
                DATA_SET.accept(heightmap, DATA_GET.apply(this));
                IS_OPAQUE_SET.accept(heightmap, IS_OPAQUE_GET.apply(this));
                CHUNK_SET.accept(heightmap, CHUNK_GET.apply(this));
                return heightmap;
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            }
        }

        static {
            try {
                {
                    var f = Heightmap.class.getDeclaredField("c");//data
                    f.setAccessible(true);
                    DATA_SET = LambdaMetafactoryUtil.setterOf(f);
                    DATA_GET = LambdaMetafactoryUtil.getterOf(f);
                }
                {
                    var f = Heightmap.class.getDeclaredField("d");//isOpaque
                    f.setAccessible(true);
                    IS_OPAQUE_SET = LambdaMetafactoryUtil.setterOf(f);
                    IS_OPAQUE_GET = LambdaMetafactoryUtil.getterOf(f);
                }
                {
                    var f = Heightmap.class.getDeclaredField("e");//chunk
                    f.setAccessible(true);
                    CHUNK_SET = LambdaMetafactoryUtil.setterOf(f);
                    CHUNK_GET = LambdaMetafactoryUtil.getterOf(f);
                }
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
    }

    static {
        try {
            var f = LevelChunk.class.getDeclaredField("heightMap");
            f.setAccessible(true);
            HEIGHT_MAP_GETTER = LambdaMetafactoryUtil.getterOf(f);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
