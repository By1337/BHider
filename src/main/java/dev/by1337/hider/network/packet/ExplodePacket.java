package dev.by1337.hider.network.packet;

import dev.by1337.hider.util.LazyLoad;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class ExplodePacket implements Packet {
    private final FriendlyByteBuf in;
    private FriendlyByteBuf out;

    private final LazyLoad<Integer> packetId;
    private final LazyLoad<Double> x;
    private final LazyLoad<Double> y;
    private final LazyLoad<Double> z;
    private final LazyLoad<Float> power;
    private final LazyLoad<List<BlockPos>> toBlow;
    private final LazyLoad<Float> knockbackX;
    private final LazyLoad<Float> knockbackY;
    private final LazyLoad<Float> knockbackZ;


    public ExplodePacket(final FriendlyByteBuf in, FriendlyByteBuf out) {
        this.in = in;
        this.out = out;
        packetId = new LazyLoad<>(in::readVarInt_, null);
        x = new LazyLoad<>(() -> (double) in.readFloat(), packetId);
        y = new LazyLoad<>(() -> (double) in.readFloat(), x);
        z = new LazyLoad<>(() -> (double) in.readFloat(), y);
        power = new LazyLoad<>(in::readFloat, z);
        toBlow = new LazyLoad<>(() -> {
            int var1 = in.readInt();
            List<BlockPos> blockPos = new ArrayList<>();
            int var2 = Mth.floor(x());
            int var3 = Mth.floor(y());
            int var4 = Mth.floor(z());
            for (int var5 = 0; var5 < var1; ++var5) {
                int var6 = in.readByte() + var2;
                int var7 = in.readByte() + var3;
                int var8 = in.readByte() + var4;
                blockPos.add(new BlockPos(var6, var7, var8));
            }
            return blockPos;
        }, power);
        knockbackX = new LazyLoad<>(in::readFloat, toBlow);
        knockbackY = new LazyLoad<>(in::readFloat, knockbackX);
        knockbackZ = new LazyLoad<>(in::readFloat, knockbackY);
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

    public double x() {
        return x.get();
    }

    public double y() {
        return y.get();
    }

    public double z() {
        return z.get();
    }

    public float power() {
        return power.get();
    }

    public List<BlockPos> toBlow() {
        return toBlow.get();
    }
    public float knockbackX() {
        return knockbackX.get();
    }
    public float knockbackY() {
        return knockbackY.get();
    }
    public float knockbackZ() {
        return knockbackZ.get();
    }
}
