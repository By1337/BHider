package dev.by1337.hider.world;

import dev.by1337.hider.network.packet.LevelChunkPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.by1337.blib.geom.Vec2i;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class VirtualWorld {
    public static final BlockState AIR = Blocks.AIR.getBlockData();
    private final Map<Vec2i, VirtualChunk> chunks = new HashMap<>();

    @Nullable
    public VirtualChunk getChunk(final Vec2i pos) {
        return chunks.get(pos);
    }

    public void unloadChunk(final Vec2i pos) {
        chunks.remove(pos);
    }

    public void readChunk(LevelChunkPacket packet) {
        Vec2i pos = new Vec2i(packet.x(), packet.z());
        VirtualChunk virtualChunk = new VirtualChunk(pos.x, pos.y);
        virtualChunk.replaceWithPacketData(
                new FriendlyByteBuf(Unpooled.wrappedBuffer(packet.buffer())),
                packet.availableSections()
        );
        chunks.put(pos, virtualChunk);
    }

    @Nullable
    public BlockState getBlock(int x, int y, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        VirtualChunk chunk = chunks.get(new Vec2i(chunkX, chunkZ));
        if (chunk == null) {
            return null;
        }
        return chunk.getBlock(x, y, z);
    }

    public void setBlock(int x, int y, int z, BlockState state) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        VirtualChunk chunk = chunks.computeIfAbsent(new Vec2i(chunkX, chunkZ), pos -> new VirtualChunk(pos.x, pos.y));
        chunk.setBlock(x, y, z, state);
    }
}
