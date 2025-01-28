package dev.by1337.hider.network.packet;

import dev.by1337.hider.network.PacketIds;
import dev.by1337.hider.util.LazyLoad;
import dev.by1337.hider.util.ValueHolder;
import dev.by1337.hider.util.WrappedValueHolder;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.Nullable;

public class TeleportEntityPacket extends Packet {
    private static final ValueHolder<Integer> PACKET_ID_HOLDER = WrappedValueHolder.of(PacketIds.TELEPORT_ENTITY_PACKET);
    private final @Nullable FriendlyByteBuf in;

    private final ValueHolder<Integer> packetId;
    private final ValueHolder<Integer> entityId;
    private final ValueHolder<Double> x;
    private final ValueHolder<Double> y;
    private final ValueHolder<Double> z;
    private final ValueHolder<Byte> yRot;
    private final ValueHolder<Byte> xRot;
    private final ValueHolder<Boolean> onGround;


    public TeleportEntityPacket(final FriendlyByteBuf in) {
        this.in = in;
        packetId = new LazyLoad<>(in::readVarInt_, null);

        entityId = new LazyLoad<>(in::readVarInt_, packetId);
        x = new LazyLoad<>(in::readDouble, entityId);
        y = new LazyLoad<>(in::readDouble, x);
        z = new LazyLoad<>(in::readDouble, y);
        yRot = new LazyLoad<>(in::readByte, z);
        xRot = new LazyLoad<>(in::readByte, yRot);
        onGround = new LazyLoad<>(in::readBoolean, xRot);
    }

    public TeleportEntityPacket(int entityId, double x, double y, double z, byte yRot, byte xRot, boolean onGround) {
        in = null;
        packetId = PACKET_ID_HOLDER;
        this.entityId = WrappedValueHolder.of(entityId);
        this.x = WrappedValueHolder.of(x);
        this.y = WrappedValueHolder.of(y);
        this.z = WrappedValueHolder.of(z);
        this.yRot = WrappedValueHolder.of(yRot);
        this.xRot = WrappedValueHolder.of(xRot);
        this.onGround = WrappedValueHolder.of(onGround);
    }

    @Override
    protected void write0(FriendlyByteBuf out) {
        if (in != null) {
            in.resetReaderIndex();
            out.writeBytes(in);
        } else {
            out.writeVarInt(packetId.get());
            out.writeVarInt(entityId.get());
            out.writeDouble(this.x.get());
            out.writeDouble(this.y.get());
            out.writeDouble(this.z.get());
            out.writeByte(this.yRot.get());
            out.writeByte(this.xRot.get());
            out.writeBoolean(this.onGround.get());
        }
    }

    public int getEntityId() {
        return entityId.get();
    }

    @Override
    public int getEntity() {
        return getEntityId();
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

    public byte yRot() {
        return yRot.get();
    }

    public byte xRot() {
        return xRot.get();
    }

    public boolean isOnGround() {
        return onGround.get();
    }
}
