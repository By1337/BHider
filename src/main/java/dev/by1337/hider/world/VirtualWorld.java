package dev.by1337.hider.world;

import dev.by1337.hider.network.packet.LevelChunkPacket;
import dev.by1337.hider.shapes.BlockBox;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.by1337.blib.BLib;
import org.by1337.blib.geom.Vec2i;
import org.by1337.blib.geom.Vec3d;
import org.by1337.blib.profiler.Profiler;
import org.jetbrains.annotations.NotNull;
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

    @NotNull
    public BlockState getBlock(int x, int y, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        VirtualChunk chunk = chunks.get(new Vec2i(chunkX, chunkZ));
        if (chunk == null) {
            return AIR;
        }
        var v = chunk.getBlock(x, y, z);
        return v == null ? AIR : v.state();
    }

    @Nullable
    public VirtualBlock getVirtualBlock(int x, int y, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        VirtualChunk chunk = chunks.get(new Vec2i(chunkX, chunkZ));
        if (chunk == null) {
            return null;
        }
        return chunk.getBlock(x, y, z);
    }

    @NotNull
    public BlockBox getBlockBox(int x, int y, int z) {
        var v = getVirtualBlock(x, y, z);
        return v == null ? BlockBox.EMPTY : v.box(new Vec3d(x, y, z));
    }

    public void setBlock(int x, int y, int z, BlockState state) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        VirtualChunk chunk = chunks.computeIfAbsent(new Vec2i(chunkX, chunkZ), pos -> new VirtualChunk(pos.x, pos.y));
        chunk.setBlock(x, y, z, state);
    }

    public @Nullable BlockState rayTrace(Vec3d rayOrigin, Vec3d rayEnd) {
        Vec3d rayDirection = rayEnd.sub(rayOrigin).normalize();

        double x = rayOrigin.x;
        double y = rayOrigin.y;
        double z = rayOrigin.z;

        double step = 0.1;
        double maxDistance = rayOrigin.distance(rayEnd);
        double distance = 0;

        while (distance < maxDistance) {
            int blockX = (int) Math.floor(x);
            int blockY = (int) Math.floor(y);
            int blockZ = (int) Math.floor(z);

            VirtualBlock block = getVirtualBlock(blockX, blockY, blockZ);

            if (block != null) {
                BlockBox box = block.box(new Vec3d(blockX, blockY, blockZ));
                if (box != null && box.rayIntersects(rayOrigin, rayDirection)) {
                    return block.state();
                }
            }

            x += rayDirection.x * step;
            y += rayDirection.y * step;
            z += rayDirection.z * step;
            distance += step;
        }
        return null;
    }


}
