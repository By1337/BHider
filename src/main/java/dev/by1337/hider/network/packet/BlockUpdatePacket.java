package dev.by1337.hider.network.packet;

import dev.by1337.hider.util.LazyLoad;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public class BlockUpdatePacket extends Packet {
    private final FriendlyByteBuf in;

    private final LazyLoad<Integer> packetId;
    private final LazyLoad<BlockPos> pos;
    private final LazyLoad<Integer> block;


    public BlockUpdatePacket(final FriendlyByteBuf in) {
        this.in = in;
        packetId = new LazyLoad<>(in::readVarInt_, null);

        pos = new LazyLoad<>(in::readBlockPos, packetId);
        block = new LazyLoad<>(in::readVarInt_, pos);
    }

    @Override
    protected void write0(FriendlyByteBuf out) {
        in.resetReaderIndex();
        out.writeBytes(in);
    }

    public BlockPos getPos() {
        return pos.get();
    }

    public int getBlock() {
        return block.get();
    }
}
