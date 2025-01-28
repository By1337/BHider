package dev.by1337.hider.network.packet;

import dev.by1337.hider.network.PacketIds;
import dev.by1337.hider.util.LazyLoad;
import dev.by1337.hider.util.ValueHolder;
import dev.by1337.hider.util.WrappedValueHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class BlockUpdatePacket extends Packet {
    private static final ValueHolder<Integer> PACKET_ID_HOLDER = WrappedValueHolder.of(PacketIds.BLOCK_UPDATE_PACKET);
    private final @Nullable FriendlyByteBuf in;

    private final ValueHolder<Integer> packetId;
    private final ValueHolder<BlockPos> pos;
    private final ValueHolder<Integer> block;
    private boolean modified;


    public BlockUpdatePacket(final FriendlyByteBuf in) {
        this.in = in;
        packetId = new LazyLoad<>(in::readVarInt_, null);

        pos = new LazyLoad<>(in::readBlockPos, packetId);
        block = new LazyLoad<>(in::readVarInt_, pos);
    }

    public BlockUpdatePacket(BlockPos pos, int block) {
        in = null;
        packetId = PACKET_ID_HOLDER;
        this.pos = WrappedValueHolder.of(pos);
        this.block = WrappedValueHolder.of(block);
    }

    @Override
    protected void write0(FriendlyByteBuf out) {
        if (in != null && !modified) {
            in.resetReaderIndex();
            out.writeBytes(in);
        } else {
            out.writeVarInt(packetId.get());
            out.writeBlockPos(pos.get());
            out.writeVarInt(block.get());
        }
    }

    public BlockPos getPos() {
        return pos.get();
    }

    public int getBlock() {
        return block.get();
    }

    public BlockState getBlockState() {
        return Block.REGISTRY_ID.fromId(block.get());
    }

    public void setBlock(final int block) {
        this.block.set(block);
        modified = true;
    }

    public void setBlock(final BlockState block) {
        this.block.set(Block.getCombinedId(block));
        modified = true;
    }

    public void setPos(final BlockPos pos) {
        this.pos.set(pos);
        modified = true;
    }
}
