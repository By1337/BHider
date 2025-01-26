package dev.by1337.hider.network.packet;

import dev.by1337.hider.util.LazyLoad;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.chunk.ChunkBiomeContainer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class LevelChunkPacket extends Packet {
    private final FriendlyByteBuf in;

    private final LazyLoad<Integer> packetId;
    private final LazyLoad<Integer> x;
    private final LazyLoad<Integer> z;
    private final LazyLoad<Integer> availableSections;
    private final LazyLoad<CompoundTag> heightmaps;
    private final LazyLoad<int @Nullable []> biomes;
    private final LazyLoad<byte[]> buffer;
    private final LazyLoad<List<CompoundTag>> blockEntitiesTags;
    private final LazyLoad<Boolean> fullChunk;

    public LevelChunkPacket(final FriendlyByteBuf in) {
        this.in = in;

        packetId = new LazyLoad<>(in::readVarInt_, null);
        x = new LazyLoad<>(in::readInt, packetId);
        z = new LazyLoad<>(in::readInt, x);
        fullChunk = new LazyLoad<>(in::readBoolean, z);
        availableSections = new LazyLoad<>(in::readVarInt_, fullChunk);
        heightmaps = new LazyLoad<>(in::readNbt, availableSections);
        biomes = new LazyLoad<>(() -> {
            if (fullChunk.get()) {
                return in.readVarIntArray(ChunkBiomeContainer.BIOMES_SIZE);
            }
            return null;
        }, heightmaps);

        buffer = new LazyLoad<>(() -> {
            int i = in.readVarInt_();
            byte[] b = new byte[i];
            in.readBytes(b);
            return b;
        }, biomes);
        blockEntitiesTags = new LazyLoad<>(() -> {
            int j = in.readVarInt_();
            List<CompoundTag> list = new ArrayList<>();
            for (int k = 0; k < j; ++k) {
                list.add(in.readNbt());
            }
            return list;
        }, buffer);
    }


    @Override
    protected void write0(FriendlyByteBuf out) {
        in.resetReaderIndex();
        out.writeBytes(in);
        // этот пакет ломается если сначала его прочитать, а потом писать.
        // Поэтому мы сначала его пишем, а потом читаем для этого здесь нужен resetReaderIndex
        // todo если сообщение выше не актуально то удали отсюда in.resetReaderIndex();
        in.resetReaderIndex();
    }

    public int packetId() {
        return packetId.get();
    }

    public int x() {
        return x.get();
    }

    public int z() {
        return z.get();
    }

    public int availableSections() {
        return availableSections.get();
    }

    public CompoundTag heightmaps() {
        return heightmaps.get();
    }

    public int @Nullable [] biomes() {
        return biomes.get();
    }

    public byte[] buffer() {
        return buffer.get();
    }

    public List<CompoundTag> blockEntitiesTags() {
        return blockEntitiesTags.get();
    }

    public boolean fullChunk() {
        return fullChunk.get();
    }

    public void setPacketId(int packetId) {
        this.packetId.set(packetId);
    }


}
