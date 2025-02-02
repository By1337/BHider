package dev.by1337.hider.world;

import dev.by1337.hider.shapes.BlockShapes;
import net.minecraft.network.FriendlyByteBuf;

public class VirtualChunk {
    public final int x;
    public final int z;
    public final VirtualChunkSection[] sections = new VirtualChunkSection[16];

    public VirtualChunk(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public void replaceWithPacketData(FriendlyByteBuf buffer, int mask, BlockShapes shapes) {
        for (int sectionIndex = 0; sectionIndex < this.sections.length; sectionIndex++) {
            if ((mask & (1 << sectionIndex)) == 0) {
                this.sections[sectionIndex] = null;
            } else {
                if (this.sections[sectionIndex] == null) {
                    this.sections[sectionIndex] = new VirtualChunkSection();
                }
                this.sections[sectionIndex].read(buffer, shapes);
            }
        }
    }

    public byte getBlockBox(int worldX, int worldY, int worldZ) {
        int localX = worldX & 15;
        int localY = worldY & 15;
        int localZ = worldZ & 15;
        int sectionIndex = worldY >> 4;

        if (sectionIndex >= 16) return 0;

        VirtualChunkSection section = sections[sectionIndex];
        if (section == null) {
            return 0;
        }
        return section.getBlockState(localX, localY, localZ);
    }

    public void setBlockBox(int worldX, int worldY, int worldZ, byte state) {
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
