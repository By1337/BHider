package dev.by1337.hider.shapes;

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
        @Contract("_, _ -> false")
        public boolean rayIntersects(Vec3d rayOrigin, Vec3d rayDirection) {
            return false;
        }

        @Override
        @Contract("_, _, _ -> this")
        public BlockBox offset(int x, int y, int z) {
            return EMPTY;
        }
    };

    boolean intersect(BlockBox aabb);

    boolean rayIntersects(Vec3d rayOrigin, Vec3d rayDirection);

    @Contract(value = "_, _, _ -> new", pure = true)
    BlockBox offset(int x, int y, int z);
}
