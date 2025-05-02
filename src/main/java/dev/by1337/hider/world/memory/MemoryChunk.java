package dev.by1337.hider.world.memory;

public class MemoryChunk {
    public static final int BLOCKS_IN_SECTION = 16 * 16 * 16;
    public static final int SECTIONS_IN_CHUNK = 16;
    public static final int INT_SIZE = Integer.BYTES;
    public static final int SECTION_BYTES_SIZE = BLOCKS_IN_SECTION * INT_SIZE;
    public static final int CHUNK_BYTES_SIZE = SECTION_BYTES_SIZE * SECTIONS_IN_CHUNK;

    public int x;
    public int z;
    private final MemoryIntByteBuffer blocks;

    public MemoryChunk(int x, int z) {
        this.x = x;
        this.z = z;
        blocks = new SafeMemoryIntByteBuffer(CHUNK_BYTES_SIZE);
    }

    public void setBlockByLocal(int localX, int localY, int localZ, int sectionIndex, int blockState) {
        int offset = BLOCKS_IN_SECTION * sectionIndex;
        blocks.setInt(offset + index(localX, localY, localZ), blockState);
    }

    public void setBlockByLocalXZ(int localX, int worldY, int localZ, int blockState) {
        int localY = worldY & 15;
        int sectionIndex = worldY >> 4;

        int offset = BLOCKS_IN_SECTION * sectionIndex;
        blocks.setInt(offset + index(localX, localY, localZ), blockState);
    }

    public void setBlock(int worldX, int worldY, int worldZ, int blockState) {
        int localX = worldX & 15;
        int localY = worldY & 15;
        int localZ = worldZ & 15;
        int sectionIndex = worldY >> 4;

        int offset = BLOCKS_IN_SECTION * sectionIndex;
        blocks.setInt(offset + index(localX, localY & 15, localZ), blockState);
    }

    public int getBlock(int worldX, int worldY, int worldZ) {
        int localX = worldX & 15;
        int localY = worldY & 15;
        int localZ = worldZ & 15;
        int sectionIndex = worldY >> 4;
        int offset = BLOCKS_IN_SECTION * sectionIndex;
        return blocks.getInt(offset + index(localX, localY, localZ));
    }

    private static int index(int x, int y, int z) {
        return (y << 8) | (z << 4) | x;
    }

    public void clear() {
        blocks.clear();
    }

    public long memoryAddress() {
        return blocks.memoryAddress();
    }

    public void free() {
        blocks.free();
    }

    public boolean released() {
        return blocks.released();
    }

}
