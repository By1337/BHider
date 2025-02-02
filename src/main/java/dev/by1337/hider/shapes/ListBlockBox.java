package dev.by1337.hider.shapes;

import org.by1337.blib.geom.Vec3d;
import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ListBlockBox implements BlockBox {
    private final List<DefaultBlockBox> list;

    public ListBlockBox(List<DefaultBlockBox> list) {
        this.list = list;
    }

    @Override
    public boolean intersect(BlockBox aabb) {
        for (DefaultBlockBox box : list) {
            if (box.intersect(aabb)) return true;
        }
        return false;
    }

    @Override
    public boolean rayIntersects(Vec3d rayOrigin, Vec3d rayDirection, int x, int y, int z) {
        for (DefaultBlockBox box : list) {
            if (box.rayIntersects(rayOrigin, rayDirection, x, y, z)) return true;
        }
        return false;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ListBlockBox that = (ListBlockBox) o;
        return Objects.equals(list, that.list);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(list);
    }
}
