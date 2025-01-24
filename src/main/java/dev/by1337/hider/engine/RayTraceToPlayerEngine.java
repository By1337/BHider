package dev.by1337.hider.engine;

import dev.by1337.hider.PlayerController;
import dev.by1337.hider.shapes.BlockBox;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import org.by1337.blib.geom.Vec3d;
import org.by1337.blib.geom.Vec3i;
import org.by1337.blib.util.lock.AutoReadWriteLock;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class RayTraceToPlayerEngine {
    private static final RayDirectionCreator[] RAY_DIRECTIONS;
    private final PlayerController controller;
    private final PlayerController.PlayerData player;

    private @Nullable Vec3d lastClientPos;
    private @Nullable Vec3d lastPlayerPos;
    private boolean lastState;
    private final AutoReadWriteLock lock = new AutoReadWriteLock();
    private final ArrayList<BlockBox> blockBoxes = new ArrayList<>();

    private @Nullable RayDirectionCreator lastDirection;

    public RayTraceToPlayerEngine(PlayerController controller, PlayerController.PlayerData player) {
        this.controller = controller;
        this.player = player;

    }

    public boolean noneMatch() {
        try (var lock1 = lock.autoWriteLock()) {
            lock1.lock();
            return lastState = noneMatch0();
        }
    }

    private boolean noneMatch0() {
        ServerPlayer client = controller.client;
        Vec3d clientPos = new Vec3d(client.lastX, client.getHeadY(), client.lastZ);
        Vec3d playerPos = new Vec3d(player.x, player.y, player.z);

        if (clientPos.equals(lastClientPos) && playerPos.equals(lastPlayerPos)) {
            return lastState;
        }
        lastClientPos = clientPos;
        lastPlayerPos = playerPos;

        Vec3d clientEye = new Vec3d(client.lastX, client.getHeadY(), client.lastZ);

        var extraSize = controller.config.armorHide.expandAabb;
        var aabb = player.player.getBoundingBox().expand(extraSize.x, extraSize.y, extraSize.z);
        Vec3d playerCenter = new Vec3d(aabb.maxX + aabb.minX, aabb.maxY + aabb.minY, aabb.maxZ + aabb.minZ).divide(2);

        blockBoxes.clear();

        loadBoxes(clientEye.toBlockPos(), playerCenter.toBlockPos());

        if (lastDirection != null) {
            if (!rayIntersects(clientPos, lastDirection.create(this, aabb, clientEye, playerCenter))) {
                return true;
            }
        }
        for (RayDirectionCreator rayDirection : RAY_DIRECTIONS) {
            if (rayDirection == lastDirection) continue;
            if (!rayIntersects(clientPos, rayDirection.create(this, aabb, clientEye, playerCenter))) {
                lastDirection = rayDirection;
                return true;
            }
        }
        return false;
    }

    private boolean rayIntersects(Vec3d clientPos, Vec3d rayDirection) {
        for (BlockBox box : blockBoxes) {
            if (box.rayIntersects(clientPos, rayDirection)) {
                return true;
            }
        }
        return false;
    }

    public void loadBoxes(Vec3i start, Vec3i end) {

        var level = controller.level;
        int x0 = start.x, y0 = start.y, z0 = start.z;
        int x1 = end.x, y1 = end.y, z1 = end.z;

        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0), dz = Math.abs(z1 - z0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int sz = z0 < z1 ? 1 : -1;

        double tMaxX = dx == 0 ? Double.MAX_VALUE : (0.5 / dx);
        double tMaxY = dy == 0 ? Double.MAX_VALUE : (0.5 / dy);
        double tMaxZ = dz == 0 ? Double.MAX_VALUE : (0.5 / dz);

        double tDeltaX = dx == 0 ? Double.MAX_VALUE : (1.0 / dx);
        double tDeltaY = dy == 0 ? Double.MAX_VALUE : (1.0 / dy);
        double tDeltaZ = dz == 0 ? Double.MAX_VALUE : (1.0 / dz);

        int x = x0, y = y0, z = z0;

        GotoType lastStep = null;
        while (true) {
            blockBoxes.add(level.getBlockBox(x, y, z));

            if (x == x1 && y == y1 && z == z1) break;

            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    add(GotoType.X, lastStep, x, y, z);
                    lastStep = GotoType.X;

                    tMaxX += tDeltaX;
                    x += sx;
                } else {
                    add(GotoType.Z, lastStep, x, y, z);
                    lastStep = GotoType.Z;

                    tMaxZ += tDeltaZ;
                    z += sz;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    add(GotoType.Y, lastStep, x, y, z);
                    lastStep = GotoType.Y;

                    tMaxY += tDeltaY;
                    y += sy;
                } else {
                    add(GotoType.Z, lastStep, x, y, z);
                    lastStep = GotoType.Z;

                    tMaxZ += tDeltaZ;
                    z += sz;
                }
            }
        }
    }

    public enum GotoType {
        X, Y, Z
    }

    public void add(GotoType nextStep, GotoType currentStep, int x, int y, int z) {
        var level = controller.level;
        blockBoxes.add(level.getBlockBox(x, y, z));

        if (nextStep == GotoType.Y) {
            if (currentStep != GotoType.X) {
                blockBoxes.add(level.getBlockBox(x + 1, y, z));
                blockBoxes.add(level.getBlockBox(x - 1, y, z));
            }
            if (currentStep != GotoType.Z) {
                blockBoxes.add(level.getBlockBox(x, y, z + 1));
                blockBoxes.add(level.getBlockBox(x, y, z - 1));
            }
        }

        if (nextStep == GotoType.Z) {
            if (currentStep != GotoType.X) {
                blockBoxes.add(level.getBlockBox(x + 1, y, z));
                blockBoxes.add(level.getBlockBox(x - 1, y, z));

                if (currentStep != GotoType.Y) {
                    blockBoxes.add(level.getBlockBox(x + 1, y + 1, z));
                    blockBoxes.add(level.getBlockBox(x + 1, y - 1, z));

                    blockBoxes.add(level.getBlockBox(x - 1, y + 1, z));
                    blockBoxes.add(level.getBlockBox(x - 1, y - 1, z));
                }
            }
            if (currentStep != GotoType.Y) {
                blockBoxes.add(level.getBlockBox(x, y + 1, z));
                blockBoxes.add(level.getBlockBox(x, y - 1, z));
            }
        }
        if (nextStep == GotoType.X) {
            if (currentStep != GotoType.Z) {
                blockBoxes.add(level.getBlockBox(x, y, z + 1));
                blockBoxes.add(level.getBlockBox(x, y, z - 1));
            }
            if (currentStep != GotoType.Y) {
                blockBoxes.add(level.getBlockBox(x, y + 1, z));
                blockBoxes.add(level.getBlockBox(x, y - 1, z));

                blockBoxes.add(level.getBlockBox(x, y + 1, z + 1));
                blockBoxes.add(level.getBlockBox(x, y - 1, z + 1));

                blockBoxes.add(level.getBlockBox(x, y + 1, z - 1));
                blockBoxes.add(level.getBlockBox(x, y - 1, z - 1));
            }
        }
    }

    static {
        RAY_DIRECTIONS = new RayDirectionCreator[]{
                (engine, aabb, clientEye, ignored) -> new Vec3d(aabb.minX, aabb.minY, aabb.minZ).sub(clientEye).normalize(),
                (engine, aabb, clientEye, ignored) -> new Vec3d(aabb.minX, aabb.maxY, aabb.minZ).sub(clientEye).normalize(),
                (engine, aabb, clientEye, ignored) -> new Vec3d(aabb.maxX, aabb.maxY, aabb.minZ).sub(clientEye).normalize(),
                (engine, aabb, clientEye, ignored) -> new Vec3d(aabb.maxX, aabb.minY, aabb.minZ).sub(clientEye).normalize(),
                (engine, aabb, clientEye, ignored) -> new Vec3d(aabb.minX, aabb.minY, aabb.maxZ).sub(clientEye).normalize(),
                (engine, aabb, clientEye, ignored) -> new Vec3d(aabb.minX, aabb.maxY, aabb.maxZ).sub(clientEye).normalize(),
                (engine, aabb, clientEye, ignored) -> new Vec3d(aabb.maxX, aabb.maxY, aabb.maxZ).sub(clientEye).normalize(),
                (engine, aabb, clientEye, ignored) -> new Vec3d(aabb.maxX, aabb.minY, aabb.maxZ).sub(clientEye).normalize(),
                (engine, aabb, clientEye, playerCenter) -> playerCenter.add(0, 0, 0).sub(clientEye).normalize(),
                (engine, aabb, clientEye, playerCenter) -> playerCenter.add(0.6, 0, 0).sub(clientEye).normalize(),
                (engine, aabb, clientEye, playerCenter) -> playerCenter.add(-0.6, 0, 0).sub(clientEye).normalize(),
                (engine, aabb, clientEye, playerCenter) -> playerCenter.add(0, 0, 0.6).sub(clientEye).normalize(),
                (engine, aabb, clientEye, playerCenter) -> playerCenter.add(0, 0, -0.6).sub(clientEye).normalize()
        };
    }

    @FunctionalInterface
    private interface RayDirectionCreator {
        Vec3d create(RayTraceToPlayerEngine engine, AABB aabb, Vec3d clientEye, Vec3d playerCenter);
    }
}
