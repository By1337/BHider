package dev.by1337.hider.network.packet;

import dev.by1337.hider.util.LazyLoad;
import net.minecraft.network.FriendlyByteBuf;

public class RemoveEntitiesPacket implements Packet {
    private final FriendlyByteBuf in;
    private FriendlyByteBuf out;

    private final LazyLoad<Integer> packetId;
    private final LazyLoad<int[]> entityIds;


    public RemoveEntitiesPacket(final FriendlyByteBuf in, FriendlyByteBuf out) {
        this.in = in;
        this.out = out;
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

    public int[] getEntityIds() {
        return entityIds.get();
    }

}
