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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class BlockShapes {
    private  final EnumSet<Material> skipTypes;
    private  final BlockBox[] boxes;

    public BlockShapes(List<Material> ignoreTypes) {
        skipTypes = EnumSet.noneOf(Material.class);
        skipTypes.addAll(ignoreTypes);


        boxes = new BlockBox[Block.REGISTRY_ID.size()];

        for (BlockState state : Block.REGISTRY_ID) {
            if (skipTypes.contains(state.getBukkitMaterial())) {
                boxes[Block.REGISTRY_ID.getId(state)] = BlockBox.EMPTY;
                continue;
            }

            VoxelShape sp = state.getCollisionShape(new BlockGetter() {
                @Override
                public @Nullable BlockEntity getTileEntity(BlockPos blockPos) {
                    return null;
                }

                @Override
                public BlockState getType(BlockPos blockPos) {
                    return Blocks.AIR.getBlockData();
                }

                @Override
                public BlockState getTypeIfLoaded(BlockPos blockPos) {
                    return Blocks.AIR.getBlockData();
                }

                @Override
                public FluidState getFluidIfLoaded(BlockPos blockPos) {
                    return Blocks.AIR.getBlockData().getFluid();
                }

                @Override
                public FluidState getFluid(BlockPos blockPos) {
                    return Blocks.AIR.getBlockData().getFluid();
                }
            }, new BlockPos(0, 0, 0));

            if (sp.isEmpty()) {
                boxes[Block.REGISTRY_ID.getId(state)] = BlockBox.EMPTY;
            } else {
                var aabbs = sp.toAabbs();
                if (aabbs.size() == 1) {
                    boxes[Block.REGISTRY_ID.getId(state)] = fromAABB(aabbs.get(0));
                } else if (aabbs.size() > 1) {
                    List<DefaultBlockBox> boxes = new ArrayList<>();
                    aabbs.forEach(aabb -> boxes.add(fromAABB(aabb)));
                    this.boxes[Block.REGISTRY_ID.getId(state)] = new ListBlockBox(boxes);
                } else {
                    boxes[Block.REGISTRY_ID.getId(state)] = BlockBox.EMPTY;
                }
            }
        }
    }

    public  BlockBox getBox(BlockState state) {
        return boxes[Block.REGISTRY_ID.getId(state)];
    }

    public  BlockBox getBox(int x) {
        return x < 0 || x >= boxes.length ? BlockBox.EMPTY : boxes[x];
    }

    private static DefaultBlockBox fromAABB(AABB aabb) {
        return new DefaultBlockBox(
                new Vec3d(aabb.minX, aabb.minY, aabb.minZ),
                new Vec3d(aabb.maxX, aabb.maxY, aabb.maxZ)
        );
    }
}
