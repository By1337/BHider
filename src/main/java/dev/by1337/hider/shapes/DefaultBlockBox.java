package dev.by1337.hider.shapes;

import org.by1337.blib.geom.Vec3d;

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

    public boolean rayIntersects(Vec3d rayOrigin, Vec3d rayDirection) {
        Vec3d invDir = new Vec3d(
                rayDirection.x == 0 ? Double.POSITIVE_INFINITY : 1.0 / rayDirection.x,
                rayDirection.y == 0 ? Double.POSITIVE_INFINITY : 1.0 / rayDirection.y,
                rayDirection.z == 0 ? Double.POSITIVE_INFINITY : 1.0 / rayDirection.z
        );

        double t1 = (min.x - rayOrigin.x) * invDir.x;
        double t2 = (max.x - rayOrigin.x) * invDir.x;

        double t3 = (min.y - rayOrigin.y) * invDir.y;
        double t4 = (max.y - rayOrigin.y) * invDir.y;

        double t5 = (min.z - rayOrigin.z) * invDir.z;
        double t6 = (max.z - rayOrigin.z) * invDir.z;

        double tMin = Math.max(Math.max(Math.min(t1, t2), Math.min(t3, t4)), Math.min(t5, t6));
        double tMax = Math.min(Math.min(Math.max(t1, t2), Math.max(t3, t4)), Math.max(t5, t6));

        if (tMax < 0 || tMin > tMax) {
            return false; // Нет пересечения
        }
        return true; // Есть пересечение
    }



    public BlockBox offset(Vec3d offset) {
        return new DefaultBlockBox(min.add(offset), max.add(offset));
    }
}
