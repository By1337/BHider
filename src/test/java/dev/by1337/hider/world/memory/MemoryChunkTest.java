package dev.by1337.hider.world.memory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MemoryChunkTest {
    private static final int AIR = 0;
    private static final int STONE = 1;
    private static final int DIRT = 2;

    @Test
    public void testSetAndGetBlock() {
        MemoryChunk memoryChunk = new MemoryChunk(0, 0);

        memoryChunk.setBlock(100, 100, 100, STONE);
        assertEquals(STONE, memoryChunk.getBlock(100, 100, 100));
        assertEquals(AIR, memoryChunk.getBlock(0, 0, 0));
        memoryChunk.free();
    }

    @Test
    public void testOverrideBlock() {
        MemoryChunk memoryChunk = new MemoryChunk(0, 0);

        memoryChunk.setBlock(1, 2, 3, DIRT);
        assertEquals(DIRT, memoryChunk.getBlock(1, 2, 3));

        memoryChunk.setBlock(1, 2, 3, STONE);
        assertEquals(STONE, memoryChunk.getBlock(1, 2, 3));
        memoryChunk.free();
    }

    @Test
    public void testClear() {
        MemoryChunk memoryChunk = new MemoryChunk(0, 0);

        memoryChunk.setBlock(1, 2, 3, STONE);
        assertEquals(STONE, memoryChunk.getBlock(1, 2, 3));

        memoryChunk.clear();
        assertEquals(AIR, memoryChunk.getBlock(1, 2, 3));
        memoryChunk.free();
    }

    @Test
    public void testOutOfChunkWriteStillWorks() {
        MemoryChunk memoryChunk = new MemoryChunk(0, 0);

        memoryChunk.setBlock(100, 100, 100, STONE);
        assertEquals(STONE, memoryChunk.getBlock(100, 100, 100));

        assertEquals(AIR, memoryChunk.getBlock(99, 100, 100));
        memoryChunk.free();
    }

    @Test
    public void testFree() {
        MemoryChunk memoryChunk = new MemoryChunk(0, 0);
        memoryChunk.setBlock(0, 0, 0, STONE);
        memoryChunk.free();
        assertThrows(IllegalStateException.class, () -> memoryChunk.setBlock(0, 0, 0, DIRT));
    }
}