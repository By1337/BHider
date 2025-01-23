package dev.by1337.hider.network.packet;

import dev.by1337.hider.util.LazyLoad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.FriendlyByteBuf;
import org.by1337.blib.util.Pair;

import java.util.function.BiConsumer;

public class SectionBlocksUpdatePacket extends Packet {
    private final FriendlyByteBuf in;
    private FriendlyByteBuf out;

    private final LazyLoad<Integer> packetId;

    private final LazyLoad<SectionPos> sectionPos;
    private final LazyLoad<Pair<short[], int[]>> states;
    private final LazyLoad<Boolean> suppressLightUpdates;


    public SectionBlocksUpdatePacket(final FriendlyByteBuf in, FriendlyByteBuf out) {
        this.in = in;
        this.out = out;
        packetId = new LazyLoad<>(in::readVarInt_, null);
        sectionPos = new LazyLoad<>(() -> SectionPos.of(in.readLong()), packetId);
        suppressLightUpdates = new LazyLoad<>(in::readBoolean, sectionPos);
        states = new LazyLoad<>(() -> {
            int i = in.readVarInt_();
            short[] positions = new short[i];
            var states = new int[i];

            for (int j = 0; j < i; ++j) {
                long k = in.readVarLong();
                positions[j] = (short) ((int) (k & 4095L));
                states[j] = (int) (k >>> 12);
            }
            return Pair.of(positions, states);
        }, suppressLightUpdates);

    }

    @Override
    protected FriendlyByteBuf writeOut() {
        in.resetReaderIndex();
        out.writeBytes(in);
        return out;
    }

    @Override
    public void setOut(FriendlyByteBuf out) {
        this.out = out;
    }

    @Override
    protected FriendlyByteBuf getOut() {
        return out;
    }

    public SectionPos getSectionPos() {
        return sectionPos.get();
    }

    public Pair<short[], int[]> getStates() {
        return states.get();
    }

    public boolean getSuppressLightUpdates() {
        return suppressLightUpdates.get();
    }

    public void runUpdates(BiConsumer<BlockPos, Integer> biconsumer) {
        BlockPos.MutableBlockPos blockposition_mutableblockposition = new BlockPos.MutableBlockPos();

        SectionPos sectionpos = getSectionPos();
        var pair = getStates();
        short[] positions = pair.getLeft();
        int[] states = pair.getRight();

        for (int i = 0; i < positions.length; ++i) {
            short short0 = positions[i];
            blockposition_mutableblockposition.set(sectionpos.relativeToBlockX(short0), sectionpos.relativeToBlockY(short0), sectionpos.relativeToBlockZ(short0));
            biconsumer.accept(blockposition_mutableblockposition, states[i]);
        }

    }
}
