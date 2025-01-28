package dev.by1337.hider.world;

import dev.by1337.hider.network.packet.LevelChunkPacket;
import dev.by1337.hider.shapes.BlockBox;
import dev.by1337.hider.shapes.BlockShapes;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.by1337.blib.geom.Vec2i;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VirtualWorld {
    private static final BlockState AIR = Blocks.AIR.getBlockData();

    private final Long2ObjectOpenHashMap<VirtualChunk> chunks = new Long2ObjectOpenHashMap<>(8192, 0.5F);
    private final VirtualChunk[] lastLoadedChunks = new VirtualChunk[16];

    private final BlockShapes blockShapes;

    public VirtualWorld(BlockShapes blockShapes) {
        this.blockShapes = blockShapes;
    }

    @Nullable
    public VirtualChunk getChunk(int x, int z) {
        int cacheKey = getChunkCacheKey(x, z);
        VirtualChunk chunk = lastLoadedChunks[cacheKey];
        if (chunk != null && chunk.x == x && chunk.z == z) {
            return chunk;
        }
        return lastLoadedChunks[cacheKey] = this.chunks.get(pair(x, z));
    }

    public VirtualChunk getChunk(final Vec2i pos) {
        return getChunk(pos.x, pos.y);
    }

    public static long pair(int i, int j) {
        return (long) i & 4294967295L | ((long) j & 4294967295L) << 32;
    }

    private static int getChunkCacheKey(int x, int z) {
        return x & 3 | (z & 3) << 2;
    }

    public void unloadChunk(int x, int z) {
        chunks.remove(pair(x, z));
    }

    public void readChunk(LevelChunkPacket packet) {
        Vec2i pos = new Vec2i(packet.x(), packet.z());
        VirtualChunk virtualChunk = new VirtualChunk(pos.x, pos.y);
        virtualChunk.replaceWithPacketData(
                new FriendlyByteBuf(Unpooled.wrappedBuffer(packet.buffer())),
                packet.availableSections()
        );
        chunks.put(pair(pos.x, pos.y), virtualChunk);
    }

    @NotNull
    public BlockState getBlock(int x, int y, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        VirtualChunk chunk = getChunk(chunkX, chunkZ);
        if (chunk == null) {
            return AIR;
        }
        var v = chunk.getBlock(x, y, z);
        return v == null ? AIR : v.state();
    }

    @Nullable
    public VirtualBlock getVirtualBlock(int x, int y, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        VirtualChunk chunk = getChunk(chunkX, chunkZ);
        if (chunk == null) {
            return null;
        }
        return chunk.getBlock(x, y, z);
    }

    @NotNull
    public BlockBox getBlockBox(int x, int y, int z) {
        var v = getVirtualBlock(x, y, z);
        return v == null ? BlockBox.EMPTY : v.box(x, y, z, blockShapes);
    }

    public void setBlock(int x, int y, int z, BlockState state) {
        setBlock(x, y, z, Block.REGISTRY_ID.getId_(state));
    }

    public void setBlock(int x, int y, int z, int state) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        VirtualChunk chunk = getChunk(chunkX, chunkZ);
        if (chunk != null)
            chunk.setBlock(x, y, z, state);
    }
}
