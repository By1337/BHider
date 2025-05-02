package dev.by1337.hider.world.memory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MemoryWorldTest {
    private static final int STONE = 1;
    private static final int AIR = 0;

    private MemoryWorld world;

    @BeforeEach
    void up() {
        MemoryWorld.CHUNK_COUNT_LIMIT = 32;
        MemoryWorld.FREE_CHUNK_QUEUE_LIMIT = 2;
        world = new MemoryWorld();
    }

    @AfterEach
    void tearDown() {
        world.free();
    }

    @Test
    public void freeTest() {
        MemoryChunk memoryChunk = world.getOrAllocateChunk(0, 0);
        assertNotNull(memoryChunk);
        assertEquals(1, world.chunkCount());
        world.free();
        assertEquals(0, world.chunkCount());
        assertTrue(memoryChunk.released());
    }

    @Test
    public void freeChunkTest() {
        world.getOrAllocateChunk(0, 0);
        assertNotNull(world.getChunk(0, 0));
        world.freeChunk(0, 0);
        assertNull(world.getChunk(0, 0));
    }

    @Test
    public void testChunkAllocationAndReuse() {
        MemoryChunk chunk = world.getOrAllocateChunk(0, 0);
        assertNotNull(chunk);
        chunk.setBlock(1, 1, 1, STONE);
        world.freeChunk(0, 0);

        MemoryChunk chunk1 = world.getOrAllocateChunk(0, 0);
        assertNotNull(chunk1);

        assertSame(chunk, chunk1);
        assertEquals(AIR, chunk1.getBlock(1, 1, 1));
        world.freeChunk(0, 0);
    }

    @Test
    public void getChunkTest() {
        world.getOrAllocateChunk(0, 0);
        MemoryChunk chunk = world.getChunk(0, 0);
        assertNotNull(chunk);
        assertSame(chunk, world.getChunk(0, 0));
        world.freeChunk(0, 0);
    }


    @Test
    public void testBlockSet() {
        world.getOrAllocateChunk(0, 0);
        world.setBlockState(1, 2, 3, STONE);
        assertEquals(STONE, world.getBlockState(1, 2, 3));
        world.freeChunk(0, 0);
    }

    @Test
    public void testBlockSet2() {
        world.getOrAllocateChunk(-105 >> 4, -67 >> 4);
        world.setBlockState(-105, 69, -67, STONE);
        assertEquals(STONE, world.getBlockState(-105, 69, -67));
        world.freeChunk(-105 >> 4, -67 >> 4);
    }

    @Test
    public void chunkLimitTest() {
        for (int i = 0; i < MemoryWorld.CHUNK_COUNT_LIMIT; i++) {
            assertNotNull(world.getOrAllocateChunk(0, i));
        }
        assertNull(world.getOrAllocateChunk(-1, -1));
        world.freeChunk(0, 0);
        assertNotNull(world.getOrAllocateChunk(-1, -1));
        world.free();
    }

}