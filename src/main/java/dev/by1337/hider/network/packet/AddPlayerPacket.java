package dev.by1337.hider.network.packet;

import dev.by1337.hider.network.PacketIds;
import dev.by1337.hider.util.LazyLoad;
import dev.by1337.hider.util.ValueHolder;
import dev.by1337.hider.util.WrappedValueHolder;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class AddPlayerPacket extends Packet {
    private static final ValueHolder<Integer> PACKET_ID_HOLDER = WrappedValueHolder.of(PacketIds.ADD_PLAYER);
    private final @Nullable FriendlyByteBuf in;

    private final ValueHolder<Integer> packetId;
    private final ValueHolder<Integer> entityId;
    private final ValueHolder<UUID> playerId;
    private final ValueHolder<Double> x;
    private final ValueHolder<Double> y;
    private final ValueHolder<Double> z;
    private final ValueHolder<Byte> yRot;
    private final ValueHolder<Byte> xRot;
    private boolean modified;

    public AddPlayerPacket(final FriendlyByteBuf in) {
        this.in = in;

        packetId = new LazyLoad<>(in::readVarInt_, null);
        this.entityId = new LazyLoad<>(in::readVarInt_, packetId);
        this.playerId = new LazyLoad<>(in::readUUID, entityId);
        this.x = new LazyLoad<>(in::readDouble, playerId);
        this.y = new LazyLoad<>(in::readDouble, x);
        this.z = new LazyLoad<>(in::readDouble, y);
        this.yRot = new LazyLoad<>(in::readByte, z);
        this.xRot = new LazyLoad<>(in::readByte, yRot);
    }

    public AddPlayerPacket(int entityId, UUID playerId, double x, double y, double z, byte yRot, byte xRot) {
        in = null;
        packetId = PACKET_ID_HOLDER;
        this.entityId = WrappedValueHolder.of(entityId);
        this.playerId = WrappedValueHolder.of(playerId);
        this.x = WrappedValueHolder.of(x);
        this.y = WrappedValueHolder.of(y);
        this.z = WrappedValueHolder.of(z);
        this.yRot = WrappedValueHolder.of(yRot);
        this.xRot = WrappedValueHolder.of(xRot);
    }

    @Override
    protected void write0(FriendlyByteBuf out) {
        if (in != null && !modified) {
            in.resetReaderIndex();
            out.writeBytes(in);
            return;
        }
        out.writeVarInt(packetId.get());
        out.writeVarInt(entityId.get());
        out.writeUUID(playerId.get());
        out.writeDouble(x.get());
        out.writeDouble(y.get());
        out.writeDouble(z.get());
        out.writeByte(yRot.get());
        out.writeByte(xRot.get());
    }

    @Override
    public int getEntity() {
        return entityId.get();
    }

    public int packetId() {
        return packetId.get();
    }

    public int entityId() {
        return entityId.get();
    }

    public UUID playerId() {
        return playerId.get();
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

    public void setEntityId(final int id) {
        entityId.set(id);
        modified = true;
    }

    public void setPlayerId(final UUID id) {
        playerId.set(id);
        modified = true;
    }

    public void setX(final double x) {
        this.x.set(x);
        modified = true;
    }

    public void setY(final double y) {
        this.y.set(y);
        modified = true;
    }

    public void setZ(final double z) {
        this.z.set(z);
        modified = true;
    }

    public void setYRot(final byte yRot) {
        this.yRot.set(yRot);
        modified = true;
    }

    public void setXRot(final byte xRot) {
        this.xRot.set(xRot);
        modified = true;
    }
}
