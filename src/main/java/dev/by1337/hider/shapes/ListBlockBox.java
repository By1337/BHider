package dev.by1337.hider.shapes;

import org.by1337.blib.geom.Vec3d;
import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.List;
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
    public boolean rayIntersects(Vec3d rayOrigin, Vec3d rayDirection) {
        for (DefaultBlockBox box : list) {
            if (box.rayIntersects(rayOrigin, rayDirection)) return true;
        }
        return false;
    }

    @Override
    @Contract(value = "_, _, _ -> new", pure = true)
    public BlockBox offset(int x, int y, int z) {
        return new ListBlockBox(list.stream().map(box -> (DefaultBlockBox) box.offset(x, y, z)).collect(Collectors.toCollection(ArrayList::new)));
    }
}
