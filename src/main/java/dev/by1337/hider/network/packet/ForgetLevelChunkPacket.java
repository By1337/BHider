package dev.by1337.hider.network.packet;

import dev.by1337.hider.util.LazyLoad;
import net.minecraft.network.FriendlyByteBuf;

public class ForgetLevelChunkPacket extends Packet {
    private final FriendlyByteBuf in;

    private final LazyLoad<Integer> packetId;
    private final LazyLoad<Integer> x;
    private final LazyLoad<Integer> y;


    public ForgetLevelChunkPacket(final FriendlyByteBuf in) {
        this.in = in;

        packetId = new LazyLoad<>(in::readVarInt_, null);
        x = new LazyLoad<>(in::readInt, packetId);
        y = new LazyLoad<>(in::readInt, x);
    }

    @Override
    protected void write0(FriendlyByteBuf out) {
        in.resetReaderIndex();
        out.writeBytes(in);
    }

    public int x() {
        return x.get();
    }

    public int y() {
        return y.get();
    }
}
