package dev.by1337.hider.network.packet;

import dev.by1337.hider.network.PacketIds;
import dev.by1337.hider.util.LazyLoad;
import dev.by1337.hider.util.ValueHolder;
import dev.by1337.hider.util.WrappedValueHolder;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.Nullable;

public class RemoveEntitiesPacket extends Packet {
    private static final ValueHolder<Integer> PACKET_ID_HOLDER = WrappedValueHolder.of(PacketIds.REMOVE_ENTITIES_PACKET);
    private final @Nullable FriendlyByteBuf in;

    private final ValueHolder<Integer> packetId;
    private final ValueHolder<int[]> entityIds;
    private boolean modified;

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
        this.packetId = PACKET_ID_HOLDER;
        this.entityIds = WrappedValueHolder.of(ids);
    }

    @Override
    protected void write0(FriendlyByteBuf out) {
        if (in == null || modified) {
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
    }

    public int[] getEntityIds() {
        return entityIds.get();
    }

    public void setEntityIds(int... ids) {
        entityIds.set(ids);
        modified = true;
    }

}
