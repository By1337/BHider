package dev.by1337.hider.shapes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.bukkit.Material;
import org.by1337.blib.geom.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class BlockShapes {
    private final EnumSet<Material> skipTypes;
    private final List<BlockBox> blockBoxes = new ArrayList<>();
    private final byte[] blockIdToBlockBox;

    public BlockShapes(List<Material> ignoreTypes) {
        skipTypes = EnumSet.noneOf(Material.class);
        skipTypes.addAll(ignoreTypes);

        blockBoxes.add(BlockBox.EMPTY);


        blockIdToBlockBox = new byte[Block.REGISTRY_ID.size()];

        Map<List<AABB>, Integer> rawBoxToId = new HashMap<>();

        for (BlockState state : Block.REGISTRY_ID) {

            if (skipTypes.contains(state.getBukkitMaterial())) {
                blockIdToBlockBox[Block.REGISTRY_ID.getId(state)] = 0;
                continue;
            }

            VoxelShape sp = state.getCollisionShape(FakeBlockGetter.INSTANCE, new BlockPos(0, 0, 0));

            if (sp.isEmpty()) {
                blockIdToBlockBox[Block.REGISTRY_ID.getId(state)] = 0;
            } else {
                List<AABB> aabbs = sp.toAabbs();
                Integer id = rawBoxToId.get(aabbs);
                if (id != null) {
                    blockIdToBlockBox[Block.REGISTRY_ID.getId(state)] = id.byteValue();
                } else {
                    BlockBox box;
                    if (aabbs.size() == 1) {
                        box = fromAABB(aabbs.get(0));
                    } else {
                        List<DefaultBlockBox> boxes = new ArrayList<>();
                        aabbs.forEach(aabb -> boxes.add(fromAABB(aabb)));
                        box = new ListBlockBox(boxes);
                    }
                    int pos = blockBoxes.size();
                    blockBoxes.add(box);
                    rawBoxToId.put(aabbs, pos);
                    blockIdToBlockBox[Block.REGISTRY_ID.getId(state)] = (byte) pos;

                    if (pos > 255) {
                        throw new IllegalStateException("Достигнут лимит в 256 блоков!");
                    }
                }
            }
        }
    }
    public byte toBlockBox(int block){
        return blockIdToBlockBox[block];
    }

    public BlockBox getBlockBox(byte box){
        return blockBoxes.get(box & 0xFF);
    }

    private static DefaultBlockBox fromAABB(AABB aabb) {
        return new DefaultBlockBox(
                new Vec3d(aabb.minX, aabb.minY, aabb.minZ),
                new Vec3d(aabb.maxX, aabb.maxY, aabb.maxZ)
        );
    }

    private static class FakeBlockGetter implements BlockGetter {
        private static final FakeBlockGetter INSTANCE = new FakeBlockGetter();

        @Override
        public @Nullable BlockEntity getTileEntity(BlockPos pos) {
            return null;
        }

        @Override
        public BlockState getType(BlockPos pos) {
            return Blocks.AIR.getBlockData();
        }

        @Override
        public BlockState getTypeIfLoaded(BlockPos pos) {
            return Blocks.AIR.getBlockData();
        }

        @Override
        public FluidState getFluidIfLoaded(BlockPos pos) {
            return Blocks.AIR.getBlockData().getFluid();
        }

        @Override
        public FluidState getFluid(BlockPos pos) {
            return Blocks.AIR.getBlockData().getFluid();
        }
    }
}
