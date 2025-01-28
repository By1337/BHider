package dev.by1337.hider.network.packet;

import dev.by1337.hider.network.PacketIds;
import dev.by1337.hider.util.LazyLoad;
import dev.by1337.hider.util.ValueHolder;
import dev.by1337.hider.util.WrappedValueHolder;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.Nullable;

public class AnimatePacket extends Packet {
    private static final ValueHolder<Integer> PACKET_ID_HOLDER = WrappedValueHolder.of(PacketIds.ANIMATE_PACKET);
    private final @Nullable FriendlyByteBuf in;

    private final ValueHolder<Integer> packetId;
    private final ValueHolder<Integer> entityId;
    private final ValueHolder<Byte> action;
    private boolean modified;


    public AnimatePacket(final FriendlyByteBuf in) {
        this.in = in;
        packetId = new LazyLoad<>(in::readVarInt_, null);

        entityId = new LazyLoad<>(in::readVarInt_, packetId);
        action = new LazyLoad<>(in::readByte, entityId);
    }

    public AnimatePacket(int entityId, byte action) {
        in = null;
        packetId = PACKET_ID_HOLDER;
        this.entityId = WrappedValueHolder.of(entityId);
        this.action = WrappedValueHolder.of(action);
    }

    @Override
    protected void write0(FriendlyByteBuf out) {
        if (in != null && !modified) {
            in.resetReaderIndex();
            out.writeBytes(in);
        } else {
            out.writeVarInt(packetId.get());
            out.writeVarInt(entityId.get());
            out.writeByte(action.get());
        }
    }
    public int getEntityId() {
        return entityId.get();
    }
    public byte getAction() {
        return action.get();
    }
    public void setAction(byte yHeadRot) {
        this.action.set(yHeadRot);
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
