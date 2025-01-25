package dev.by1337.hider.network.packet;

import dev.by1337.hider.util.LazyLoad;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public class AddPlayerPacket extends Packet {
    private final FriendlyByteBuf in;
    private FriendlyByteBuf out;

    private final LazyLoad<Integer> packetId;
    private final LazyLoad<Integer> entityId;
    private final LazyLoad<UUID> playerId;
    private final LazyLoad<Double> x;
    private final LazyLoad<Double> y;
    private final LazyLoad<Double> z;
    private final LazyLoad<Byte> yRot;
    private final LazyLoad<Byte> xRot;

    public AddPlayerPacket(final FriendlyByteBuf in, final FriendlyByteBuf out) {
        this.in = in;
        this.out = out;

        packetId = new LazyLoad<>(in::readVarInt_, null);
        this.entityId = new LazyLoad<>(in::readVarInt_, packetId);
        this.playerId = new LazyLoad<>(in::readUUID, entityId);
        this.x = new LazyLoad<>(in::readDouble, playerId);
        this.y = new LazyLoad<>(in::readDouble, x);
        this.z = new LazyLoad<>(in::readDouble, y);
        this.yRot = new LazyLoad<>(in::readByte, z);
        this.xRot = new LazyLoad<>(in::readByte, yRot);
    }

    @Override
    protected FriendlyByteBuf writeOut() {
        if (!entityId.isModified() && !playerId.isModified() && !x.isModified() && !y.isModified() &&
                !z.isModified() && !yRot.isModified() && !xRot.isModified()) {
            in.resetReaderIndex();
            out.writeBytes(in);
            return out;
        }
        out.writeVarInt(packetId.get());
        out.writeVarInt(entityId.get());
        out.writeUUID(playerId.get());
        out.writeDouble(x.get());
        out.writeDouble(y.get());
        out.writeDouble(z.get());
        out.writeByte(yRot.get());
        out.writeByte(xRot.get());
        return out;
    }

    @Override
    public int getEntity() {
        return entityId.get();
    }

    @Override
    public void setOut(FriendlyByteBuf out) {
        this.out = out;
    }

    @Override
    protected FriendlyByteBuf getOut() {
        return out;
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

    public void setPacketId(final int id) {
        packetId.set(id);
    }

    public void setEntityId(final int id) {
        entityId.set(id);
    }

    public void setPlayerId(final UUID id) {
        playerId.set(id);
    }

    public void setX(final double x) {
        this.x.set(x);
    }

    public void setY(final double y) {
        this.y.set(y);
    }

    public void setZ(final double z) {
        this.z.set(z);
    }

    public void setYRot(final byte yRot) {
        this.yRot.set(yRot);
    }

    public void setXRot(final byte xRot) {
        this.xRot.set(xRot);
    }
}
