package dev.by1337.hider.network.packet;

import dev.by1337.hider.network.PacketIds;
import dev.by1337.hider.util.LazyLoad;
import dev.by1337.hider.util.ValueHolder;
import dev.by1337.hider.util.WrappedValueHolder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

public class SetEntityDataPacket extends Packet {
    private static final ValueHolder<Integer> PACKET_ID_HOLDER = WrappedValueHolder.of(PacketIds.SET_ENTITY_DATA_PACKET);
    private final @Nullable FriendlyByteBuf in;

    private final ValueHolder<Integer> packetId;
    private final ValueHolder<Integer> entityId;
    private final ValueHolder<List<SynchedEntityData.DataItem<?>>> packedItems;
    private boolean modified;


    public SetEntityDataPacket(FriendlyByteBuf in) {
        this.in = in;

        packetId = new LazyLoad<>(in::readVarInt_, null);
        entityId = new LazyLoad<>(in::readVarInt_, packetId);
        packedItems = new LazyLoad<>(() -> {
            try {
                return SynchedEntityData.unpack(in);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, entityId);
    }

    public SetEntityDataPacket(int entity, List<SynchedEntityData.DataItem<?>> packedItems) {
        in = null;
        packetId = PACKET_ID_HOLDER;
        entityId = WrappedValueHolder.of(entity);
        this.packedItems = WrappedValueHolder.of(packedItems);
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

    public List<SynchedEntityData.DataItem<?>> packedItems() {
        return packedItems.get();
    }

    public void setPacketId(int id) {
        packetId.set(id);
        modified = true;
    }

    public void setEntityId(int id) {
        entityId.set(id);
        modified = true;
    }

    public void setPackedItems(List<SynchedEntityData.DataItem<?>> items) {
        packedItems.set(items);
        modified = true;
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
        try {
            SynchedEntityData.pack(this.packedItems.get(), out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
