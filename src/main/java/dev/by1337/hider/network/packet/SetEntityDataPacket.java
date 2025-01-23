package dev.by1337.hider.network.packet;

import dev.by1337.hider.util.LazyLoad;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;

import java.io.IOException;
import java.util.List;

public class SetEntityDataPacket extends Packet {
    private final FriendlyByteBuf in;
    private FriendlyByteBuf out;

    private final LazyLoad<Integer> packetId;
    private final LazyLoad<Integer> entityId;
    private final LazyLoad<List<SynchedEntityData.DataItem<?>>> packedItems;

    public SetEntityDataPacket(FriendlyByteBuf in, FriendlyByteBuf out) {
        this.in = in;
        this.out = out;
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
    }

    public void setEntityId(int id) {
        entityId.set(id);
    }

    public void setPackedItems(List<SynchedEntityData.DataItem<?>> items) {
        packedItems.set(items);
    }

    @Override
    protected FriendlyByteBuf writeOut() {
        if (!entityId.isModified() && !packedItems.isModified()) {
            in.resetReaderIndex();
            out.writeBytes(in);
            return out;
        }
        out.writeVarInt(packetId.get());
        out.writeVarInt(entityId.get());
        if (!packedItems.isModified()) {
            out.writeBytes(in);
        } else {
            try {
                SynchedEntityData.pack(this.packedItems.get(), out);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return out;
    }

    @Override
    protected FriendlyByteBuf getOut() {
        return out;
    }

    @Override
    public void setOut(FriendlyByteBuf out) {
        this.out = out;
    }
}
