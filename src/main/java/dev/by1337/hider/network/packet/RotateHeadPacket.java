package dev.by1337.hider.network.packet;

import dev.by1337.hider.network.PacketIds;
import dev.by1337.hider.util.LazyLoad;
import dev.by1337.hider.util.ValueHolder;
import dev.by1337.hider.util.WrappedValueHolder;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.Nullable;

public class RotateHeadPacket extends Packet {
    private static final ValueHolder<Integer> PACKET_ID_HOLDER = WrappedValueHolder.of(PacketIds.ROTATE_HEAD_PACKET);
    private final @Nullable FriendlyByteBuf in;

    private final ValueHolder<Integer> packetId;
    private final ValueHolder<Integer> entityId;
    private final ValueHolder<Byte> yHeadRot;
    private boolean modified;


    public RotateHeadPacket(final FriendlyByteBuf in) {
        this.in = in;
        packetId = new LazyLoad<>(in::readVarInt_, null);

        entityId = new LazyLoad<>(in::readVarInt_, packetId);
        yHeadRot = new LazyLoad<>(in::readByte, entityId);
    }

    public RotateHeadPacket(int entityId, byte yHeadRot) {
        in = null;
        packetId = PACKET_ID_HOLDER;
        this.entityId = WrappedValueHolder.of(entityId);
        this.yHeadRot = WrappedValueHolder.of(yHeadRot);
    }

    @Override
    protected void write0(FriendlyByteBuf out) {
        if (in != null && !modified) {
            in.resetReaderIndex();
            out.writeBytes(in);
        } else {
            out.writeVarInt(packetId.get());
            out.writeVarInt(entityId.get());
            out.writeByte(yHeadRot.get());
        }
    }
    public int getEntityId() {
        return entityId.get();
    }
    public byte getYHeadRot() {
        return yHeadRot.get();
    }
    public void setYHeadRot(byte yHeadRot) {
        this.yHeadRot.set(yHeadRot);
        modified = true;
    }
    public void setEntityId(int entityId) {
        this.entityId.set(entityId);
        modified = true;
    }

    @Override
    public int getEntity() {
        return getEntityId();
    }
}
