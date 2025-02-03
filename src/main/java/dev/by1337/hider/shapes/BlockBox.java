package dev.by1337.hider.shapes;

import dev.by1337.hider.util.MutableVec3d;
import org.by1337.blib.geom.Vec3d;
import org.jetbrains.annotations.Contract;

public interface BlockBox {
    BlockBox EMPTY = new BlockBox() {

        @Override
        @Contract("_ -> false")
        public boolean intersect(BlockBox aabb) {
            return false;
        }

        @Override
        @Contract("_, _, _,_,_ -> false")
        public boolean rayIntersects(MutableVec3d rayOrigin, MutableVec3d rayDirection, int x, int y, int z) {
            return false;
        }

    };

    boolean intersect(BlockBox aabb);

    boolean rayIntersects(MutableVec3d rayOrigin, MutableVec3d rayDirection, int x, int y, int z);
}
