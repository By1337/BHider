package dev.by1337.hider.world;

import dev.by1337.hider.shapes.BlockBox;
import dev.by1337.hider.shapes.BlockShapes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class VirtualBlock {
    private final int id;
    private BlockState state;
    private BlockBox box;

    public VirtualBlock(final int id) {
        this.id = id;
    }

    public BlockBox box(int x, int y, int z, BlockShapes shapes) {
        if (box != null) return box;
        return box = shapes.getBox(id).offset(x, y, z);
    }

    public BlockState state() {
        if (state != null) return state;
        return state = Block.REGISTRY_ID.fromId(id);
    }
}
