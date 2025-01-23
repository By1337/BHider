package dev.by1337.hider.network.packet;

import dev.by1337.hider.util.LazyLoad;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BlockUpdatePacket implements Packet {
    private final FriendlyByteBuf in;
    private FriendlyByteBuf out;

    private final LazyLoad<Integer> packetId;
    private final LazyLoad<BlockPos> pos;
    private final LazyLoad<Integer> block;


    public BlockUpdatePacket(final FriendlyByteBuf in, FriendlyByteBuf out) {
        this.in = in;
        this.out = out;
        packetId = new LazyLoad<>(in::readVarInt_, null);

        pos = new LazyLoad<>(in::readBlockPos, packetId);
        block = new LazyLoad<>(in::readVarInt_, pos);
    }

    @Override
    public FriendlyByteBuf writeOut() {
        in.resetReaderIndex();
        out.writeBytes(in);
        return out;
    }

    @Override
    public void setOut(FriendlyByteBuf out) {
        this.out = out;
    }

    @Override
    public FriendlyByteBuf getOut() {
        return out;
    }

    public BlockPos getPos() {
        return pos.get();
    }

    public int getBlock() {
        return block.get();
    }
}
