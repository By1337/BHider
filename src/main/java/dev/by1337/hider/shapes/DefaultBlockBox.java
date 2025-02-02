package dev.by1337.hider.shapes;

import org.by1337.blib.geom.Vec3d;
import org.jetbrains.annotations.Contract;

import java.util.Objects;

public class DefaultBlockBox implements BlockBox {
    public final Vec3d min;
    public final Vec3d max;

    public DefaultBlockBox(Vec3d min, Vec3d max) {
        this.min = min;
        this.max = max;
    }

    public boolean intersect(BlockBox aabb) {
        if (aabb instanceof DefaultBlockBox def) {
            return min.x <= def.max.x &&
                    max.x >= def.min.x &&
                    min.y <= def.max.y &&
                    max.y >= def.min.y &&
                    min.z <= def.max.z &&
                    max.z >= def.min.z;
        } else if (aabb instanceof ListBlockBox list) {
            return list.intersect(this);
        }
        return false;
    }

    public boolean rayIntersects(Vec3d rayOrigin, Vec3d rayDirection, int x, int y, int z) {
        Vec3d invDir = new Vec3d(
                rayDirection.x == 0 ? Double.POSITIVE_INFINITY : 1.0 / rayDirection.x,
                rayDirection.y == 0 ? Double.POSITIVE_INFINITY : 1.0 / rayDirection.y,
                rayDirection.z == 0 ? Double.POSITIVE_INFINITY : 1.0 / rayDirection.z
        );

        double t1 = (min.x + x - rayOrigin.x) * invDir.x;
        double t2 = (max.x + x - rayOrigin.x) * invDir.x;

        double t3 = (min.y + y - rayOrigin.y) * invDir.y;
        double t4 = (max.y + y - rayOrigin.y) * invDir.y;

        double t5 = (min.z + z - rayOrigin.z) * invDir.z;
        double t6 = (max.z + z - rayOrigin.z) * invDir.z;

        double tMin = Math.max(Math.max(Math.min(t1, t2), Math.min(t3, t4)), Math.min(t5, t6));
        double tMax = Math.min(Math.min(Math.max(t1, t2), Math.max(t3, t4)), Math.max(t5, t6));

        return !(tMax < 0) && !(tMin > tMax);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefaultBlockBox that = (DefaultBlockBox) o;
        return Objects.equals(min, that.min) && Objects.equals(max, that.max);
    }

    @Override
    public int hashCode() {
        return Objects.hash(min, max);
    }
}
