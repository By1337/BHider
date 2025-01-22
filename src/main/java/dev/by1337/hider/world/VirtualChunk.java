package dev.by1337.hider.world;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class VirtualChunk {
    private final int x;
    private final int z;
    private final VirtualChunkSection[] sections = new VirtualChunkSection[16];

    public VirtualChunk(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public void replaceWithPacketData(FriendlyByteBuf buffer, int mask) {
        for (int sectionIndex = 0; sectionIndex < this.sections.length; sectionIndex++) {
            if ((mask & (1 << sectionIndex)) == 0) {
                // Если эта секция не включена в пакет, пропускаем её
                this.sections[sectionIndex] = null;
            } else {
                if (this.sections[sectionIndex] == null) {
                    this.sections[sectionIndex] = new VirtualChunkSection();
                }
                // Читаем данные секции из пакета
                this.sections[sectionIndex].read(buffer);
            }
        }
    }

    @Nullable
    public BlockState getBlock(int worldX, int worldY, int worldZ) {
        int localX = worldX & 15;
        int localY = worldY & 15;
        int localZ = worldZ & 15;
        int sectionIndex = worldY >> 4;

        VirtualChunkSection section = sections[sectionIndex];
        if (section == null) {
            return null; // Если секция не загружена
        }
        return section.getBlockState(localX, localY, localZ);
    }

    public void setBlock(int worldX, int worldY, int worldZ, BlockState state) {
        int localX = worldX & 15;
        int localY = worldY & 15;
        int localZ = worldZ & 15;
        int sectionIndex = worldY >> 4;

        if (sections[sectionIndex] == null) {
            sections[sectionIndex] = new VirtualChunkSection();
        }
        sections[sectionIndex].setBlockState(localX, localY, localZ, state);
    }
}
