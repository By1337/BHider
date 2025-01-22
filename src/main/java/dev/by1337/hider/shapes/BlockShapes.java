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
    private static final EnumSet<Material> SKIP_TYPES;
    private static final BlockBox[] BOXES;

    public static void load() {
        System.out.println("BlockShapes.load");
    }

    public static BlockBox getBox(BlockState state) {
        return BOXES[Block.REGISTRY_ID.getId(state)];
    }

    static {
        SKIP_TYPES = EnumSet.noneOf(Material.class);
        for (Material value : Material.values()) {
            if (value.isLegacy()) continue;
            if (value.isAir() || value.name().contains("GLASS")) {
                SKIP_TYPES.add(value);
            }
        }
        SKIP_TYPES.add(Material.LAVA);
        SKIP_TYPES.add(Material.WATER);
        SKIP_TYPES.add(Material.BUBBLE_COLUMN);


        BOXES = new BlockBox[Block.REGISTRY_ID.size()];

        for (BlockState state : Block.REGISTRY_ID) {
            if (SKIP_TYPES.contains(state.getBukkitMaterial())) {
                BOXES[Block.REGISTRY_ID.getId(state)] = BlockBox.EMPTY;
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
                BOXES[Block.REGISTRY_ID.getId(state)] = BlockBox.EMPTY;
            } else {
                var aabbs = sp.toAabbs();
                if (aabbs.size() == 1) {
                    BOXES[Block.REGISTRY_ID.getId(state)] = fromAABB(aabbs.get(0));
                } else if (aabbs.size() > 1) {
                    List<DefaultBlockBox> boxes = new ArrayList<>();
                    aabbs.forEach(aabb -> boxes.add(fromAABB(aabb)));
                    BOXES[Block.REGISTRY_ID.getId(state)] = new ListBlockBox(boxes);
                } else {
                    BOXES[Block.REGISTRY_ID.getId(state)] = BlockBox.EMPTY;
                }
            }
        }
    }

    private static DefaultBlockBox fromAABB(AABB aabb) {
        return new DefaultBlockBox(
                new Vec3d(aabb.minX, aabb.minY, aabb.minZ),
                new Vec3d(aabb.maxX, aabb.maxY, aabb.maxZ)
        );
    }
}
