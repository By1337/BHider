package dev.by1337.hider.world;

import dev.by1337.hider.network.packet.LevelChunkPacket;
import dev.by1337.hider.shapes.BlockBox;
import dev.by1337.hider.shapes.BlockShapes;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.network.FriendlyByteBuf;
import org.by1337.blib.geom.Vec2i;
import org.jetbrains.annotations.Nullable;

public class VirtualWorld {

    private final Long2ObjectOpenHashMap<VirtualChunk> chunks = new Long2ObjectOpenHashMap<>(8192, 0.5F);
    private final VirtualChunk[] lastLoadedChunks = new VirtualChunk[16];

    public final BlockShapes blockShapes;

    public VirtualWorld(BlockShapes blockShapes) {
        this.blockShapes = blockShapes;
    }

    public long sizeOf() {
        long size = 16L + 8L; // Ссылки на массив lastLoadedChunks и blockShapes

        // Размер lastLoadedChunks
        size += 16 * 8L; // 16 ссылок на VirtualChunk

        // Размер chunks (хэш-таблица)
        size += 32L + chunks.size() * (8L + 8L); // Примерный размер хэш-таблицы

        for (VirtualChunk chunk : chunks.values()) {
            if (chunk != null) {
                size += 8L + 8L + 16 * 8L; // x, z, массив sections

                for (VirtualChunkSection section : chunk.sections) {
                    if (section != null) {
                        size += 8L + VirtualChunkSection.BLOCK_COUNT; // Ссылка + массив blockStates
                    }
                }
            }
        }
        return size;
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
                packet.availableSections(),
                blockShapes
        );
        chunks.put(pair(pos.x, pos.y), virtualChunk);
    }

    public BlockBox getBlockBox(int x, int y, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        VirtualChunk chunk = getChunk(chunkX, chunkZ);
        if (chunk == null) {
            return BlockBox.EMPTY;
        }
        return blockShapes.getBlockBox(chunk.getBlockBox(x, y, z));
    }
    public byte getBlockBoxId(int x, int y, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        VirtualChunk chunk = getChunk(chunkX, chunkZ);
        if (chunk == null) {
            return 0;
        }
        return chunk.getBlockBox(x, y, z);
    }


    public void setBlockState(int x, int y, int z, int state) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        VirtualChunk chunk = getChunk(chunkX, chunkZ);
        if (chunk != null)
            chunk.setBlockBox(x, y, z, blockShapes.toBlockBox(state));
    }
}
