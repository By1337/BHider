package dev.by1337.hider.world;

import io.netty.handler.codec.DecoderException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.BitStorage;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class VirtualChunkSection {
    public static final int BLOCK_COUNT = 16 * 16 * 16;
    private final VirtualBlock[] blockStates = new VirtualBlock[BLOCK_COUNT];

    public void read(FriendlyByteBuf buffer) {
        short nonEmptyBlockCount = buffer.readShort();
        byte bits = buffer.readByte();

        BlockState[] palette;

        if (bits < 9) {
            int paletteSize = buffer.readVarInt();
            palette = new BlockState[paletteSize];
            for (int i = 0; i < paletteSize; i++) {
                palette[i] = Block.REGISTRY_ID.fromId(buffer.readVarInt());
            }
        } else {
            palette = null;
        }

        BitStorage storage = new BitStorage(bits, BLOCK_COUNT);

        readLongArray(storage.getRaw(), buffer);


        for (int index = 0; index < BLOCK_COUNT; index++) {
            int paletteIndex = storage.get(index); // Индекс в палете
            BlockState state = palette == null ? Blocks.AIR.getBlockData() : palette[paletteIndex];
            blockStates[index] = new VirtualBlock(state);
        }
    }

    private static int index(int x, int y, int z) {
        return (y << 8) | (z << 4) | x;
    }

    @Nullable
    public VirtualBlock getBlockState(int x, int y, int z) {
        if (x < 0 || x >= 16 || y < 0 || y >= 16 || z < 0 || z >= 16) {
            return null;
        }
        return blockStates[index(x, y, z)];
    }

    public void setBlockState(int x, int y, int z, BlockState state) {
        if (x < 0 || x >= 16 || y < 0 || y >= 16 || z < 0 || z >= 16) {
            throw new IllegalArgumentException("Coordinates out of bounds: " + x + ", " + y + ", " + z);
        }
        blockStates[index(x, y, z)] = new VirtualBlock(state);
    }

    public long[] readLongArray(long @Nullable [] var1, FriendlyByteBuf buf) {
        return this.readLongArray(var1, buf.readableBytes() / 8, buf);
    }

    public long[] readLongArray(long @Nullable [] var1, int var2, FriendlyByteBuf buf) {
        int var3 = buf.readVarInt();
        if (var1 == null || var1.length != var3) {
            if (var3 > var2) {
                throw new DecoderException("LongArray with size " + var3 + " is bigger than allowed " + var2);
            }

            var1 = new long[var3];
        }

        for(int var4 = 0; var4 < var1.length; ++var4) {
            var1[var4] = buf.readLong();
        }

        return var1;
    }

}
