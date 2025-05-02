package dev.by1337.hider.world.memory;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

public class MemoryWorld {
    public static final Logger LOGGER = LoggerFactory.getLogger("BHider#MemoryWorld");
    public static int CHUNK_COUNT_LIMIT = 16_384; // 4 гига оперативы лимит
    public static int FREE_CHUNK_QUEUE_LIMIT = 32;

    private final Long2ObjectOpenHashMap<MemoryChunk> chunks = new Long2ObjectOpenHashMap<>(8192, 0.5F);
    private final MemoryChunk[] lastLoadedChunks = new MemoryChunk[16];
    private final Queue<MemoryChunk> freeChunks = new ArrayDeque<>(FREE_CHUNK_QUEUE_LIMIT);

    public long getChunkMemoryAddress(int x, int z){
        MemoryChunk chunk = getChunk(x, z);
        if (chunk == null) return 0;
        return chunk.memoryAddress();
    }

    @Nullable
    public MemoryChunk getOrAllocateChunk(int x, int z) {
        MemoryChunk chunk = getChunk(x, z);
        if (chunk != null) return chunk;
        chunk = freeChunks.poll();
        if (chunk != null) {
            chunk.x = x;
            chunk.z = z;
            chunks.put(pair(x, z), chunk);
            return chunk;
        }
        if (chunks.size() >= CHUNK_COUNT_LIMIT) {
            LOGGER.warn("Failed to allocate memory chunk at {} x {} chunk count limit. Chunks count {}", x, z, chunks.size());
            return null;
        }
        chunk = new MemoryChunk(x, z);
        LOGGER.info("allocated memory chunk at {} x {} chunk {}", x, z, chunks.size());
        chunks.put(pair(x, z), chunk);
        return chunk;
    }

    public void freeChunk(int x, int z) {
        MemoryChunk chunk = getChunk(x, z);
        if (chunk == null) return;
        chunks.remove(pair(x, z));
        LOGGER.info("free memory chunk at {} x {} chunk {}", x, z, chunks.size());
        int cacheKey = getChunkCacheKey(x, z);
        if (lastLoadedChunks[cacheKey] == chunk) {
            lastLoadedChunks[cacheKey] = null;
        }
        if (freeChunks.size() < FREE_CHUNK_QUEUE_LIMIT) {
            chunk.clear();
            freeChunks.offer(chunk);
        } else {
            chunk.free();
        }
    }

    public void setBlockState(int x, int y, int z, int state) {
        //LOGGER.info("Set block {} {} {} block:{}", x, y, z, state);
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        MemoryChunk chunk = getChunk(chunkX, chunkZ);
        if (chunk != null)
            chunk.setBlock(x, y, z, state);
    }

    public int getBlockState(int x, int y, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        MemoryChunk chunk = getChunk(chunkX, chunkZ);
        if (chunk != null) {
            return chunk.getBlock(x, y, z);
        }
        return 0;
    }

    @Nullable
    public MemoryChunk getChunk(int x, int z) {
        int cacheKey = getChunkCacheKey(x, z);
        MemoryChunk chunk = lastLoadedChunks[cacheKey];
        if (chunk != null && chunk.x == x && chunk.z == z) {
            return chunk;
        }
        return lastLoadedChunks[cacheKey] = this.chunks.get(pair(x, z));
    }

    public void free() {
        chunks.values().forEach(MemoryChunk::free);
        chunks.clear();
        Arrays.fill(lastLoadedChunks, null);
        MemoryChunk memoryChunk;
        while ((memoryChunk = freeChunks.poll()) != null) {
            memoryChunk.free();
        }
    }
    public int chunkCount() {
        return chunks.size();
    }

    public long sizeOf() {
        return (long) MemoryChunk.CHUNK_BYTES_SIZE * chunkCount();
    }

    public static long pair(int i, int j) {
        return (long) i & 4294967295L | ((long) j & 4294967295L) << 32;
    }

    private static int getChunkCacheKey(int x, int z) {
        return x & 3 | (z & 3) << 2;
    }
    @Override
    public String toString() {
        return "MemoryWorld{" +
                "chunks count=" + chunks.size() +
                ", queue size=" + freeChunks.size() +
                ", size=" + String.format("%.2f", (double) sizeOf() / (1024.0 * 1024.0)) + "mb" +
                '}';
    }
}
