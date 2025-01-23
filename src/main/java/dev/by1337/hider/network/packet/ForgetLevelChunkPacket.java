package dev.by1337.hider.network.packet;

import dev.by1337.hider.util.LazyLoad;
import net.minecraft.network.FriendlyByteBuf;

public class ForgetLevelChunkPacket extends Packet {
    private final FriendlyByteBuf in;
    private FriendlyByteBuf out;

    private final LazyLoad<Integer> packetId;
    private final LazyLoad<Integer> x;
    private final LazyLoad<Integer> y;


    public ForgetLevelChunkPacket(final FriendlyByteBuf in, FriendlyByteBuf out) {
        this.in = in;
        this.out = out;
        packetId = new LazyLoad<>(in::readVarInt_, null);
        x = new LazyLoad<>(in::readInt, packetId);
        y = new LazyLoad<>(in::readInt, x);
    }

    @Override
    protected FriendlyByteBuf writeOut() {
        in.resetReaderIndex();
        out.writeBytes(in);
        return out;
    }

    @Override
    public void setOut(FriendlyByteBuf out) {
        this.out = out;
    }

    @Override
    protected FriendlyByteBuf getOut() {
        return out;
    }

    public int x() {
        return x.get();
    }

    public int y() {
        return y.get();
    }
}
