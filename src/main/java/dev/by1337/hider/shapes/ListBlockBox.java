package dev.by1337.hider.shapes;

import org.by1337.blib.geom.Vec3d;

import java.util.List;

public class ListBlockBox implements BlockBox {
    private final List<DefaultBlockBox> list;

    public ListBlockBox(List<DefaultBlockBox> list) {
        this.list = list;
    }

    @Override
    public boolean intersect(BlockBox aabb) {
        return list.stream().anyMatch(aabb::intersect);
    }

    @Override
    public boolean rayIntersects(Vec3d rayOrigin, Vec3d rayDirection) {
        return list.stream().anyMatch(box -> box.rayIntersects(rayOrigin, rayDirection));
    }

    @Override
    public BlockBox offset(Vec3d offset) {
        return new ListBlockBox(list.stream().map(box -> (DefaultBlockBox) box.offset(offset)).toList());
    }
}
