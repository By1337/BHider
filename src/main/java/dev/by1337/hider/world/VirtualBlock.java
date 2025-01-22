package dev.by1337.hider.world;

import dev.by1337.hider.shapes.BlockBox;
import dev.by1337.hider.shapes.BlockShapes;
import net.minecraft.world.level.block.state.BlockState;
import org.by1337.blib.geom.Vec3d;

public class VirtualBlock {
    private final BlockState state;
    private BlockBox box;

    public VirtualBlock(final BlockState state) {
        this.state = state;
    }

    public BlockBox box(Vec3d pos) {
        if (box != null) return box;
        return box = BlockShapes.getBox(state).offset(pos);
    }

    public BlockState state() {
        return state;
    }
}
