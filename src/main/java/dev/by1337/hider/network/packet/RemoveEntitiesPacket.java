package dev.by1337.hider.network.packet;

import dev.by1337.hider.network.PacketIds;
import dev.by1337.hider.util.LazyLoad;
import net.minecraft.network.FriendlyByteBuf;

public class RemoveEntitiesPacket extends Packet {
    private final FriendlyByteBuf in;

    private final LazyLoad<Integer> packetId;
    private final LazyLoad<int[]> entityIds;


    public RemoveEntitiesPacket(final FriendlyByteBuf in) {
        this.in = in;

        packetId = new LazyLoad<>(in::readVarInt_, null);
        entityIds = new LazyLoad<>(() -> {
            var count = in.readVarInt_();
            var entityIds = new int[count];

            for (int var1 = 0; var1 < count; ++var1) {
                entityIds[var1] = in.readVarInt_();
            }
            return entityIds;
        }, packetId);
    }

    public RemoveEntitiesPacket(int... ids) {
        in = null;
        this.packetId = new LazyLoad<>(() -> PacketIds.REMOVE_ENTITIES_PACKET, null, true);
        this.entityIds = new LazyLoad<>(() -> ids, packetId, true);
    }

    @Override
    protected void write0(FriendlyByteBuf out) {
        if (packetId.isModified() || entityIds.isModified()) {
            out.writeVarInt(packetId.get());
            int[] ids = entityIds.get();
            out.writeVarInt(ids.length);
            for (int id : ids) {
                out.writeVarInt(id);
            }
        } else {
            in.resetReaderIndex();
            out.writeBytes(in);
        }

        return;
    }

    public int[] getEntityIds() {
        return entityIds.get();
    }

}
