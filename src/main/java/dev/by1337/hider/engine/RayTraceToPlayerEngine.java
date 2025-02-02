package dev.by1337.hider.engine;

import dev.by1337.hider.PlayerController;
import dev.by1337.hider.controller.ViewingEntity;
import dev.by1337.hider.shapes.BlockBox;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import org.by1337.blib.geom.Vec3d;
import org.by1337.blib.geom.Vec3i;
import org.by1337.blib.util.lock.AutoReadWriteLock;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class RayTraceToPlayerEngine {
    private static final int[] BLOCK_BOX = new int[2048 * 4];
    private static int BLOCK_BOX_INDEX;
    private static final RayDirectionCreator[] RAY_DIRECTIONS;
    private final PlayerController controller;
    private final ViewingEntity viewingEntity;

    private @Nullable Vec3d lastClientPos;
    private @Nullable Vec3d lastPlayerPos;
    private boolean lastState;
    private final AutoReadWriteLock lock = new AutoReadWriteLock();


    private @Nullable RayDirectionCreator lastDirection;

    public RayTraceToPlayerEngine(PlayerController controller, ViewingEntity viewingEntity) {
        this.controller = controller;
        this.viewingEntity = viewingEntity;
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
        Vec3d playerPos = new Vec3d(viewingEntity.getBukkitEntity().getLocation());

     //  if (clientPos.equals(lastClientPos) && playerPos.equals(lastPlayerPos)) {
     //      return lastState;
     //  }
        lastClientPos = clientPos;
        lastPlayerPos = playerPos;

        Vec3d clientEye = new Vec3d(client.lastX, client.getHeadY(), client.lastZ);

        var extraSize = controller.config.armorHide.expandAabb;
        var aabb = viewingEntity.getAABB().expand(extraSize.x, extraSize.y, extraSize.z);
        Vec3d playerCenter = new Vec3d(aabb.maxX + aabb.minX, aabb.maxY + aabb.minY, aabb.maxZ + aabb.minZ).divide(2);

        BLOCK_BOX_INDEX = 0;

        loadBoxes(clientEye.toBlockPos(), playerCenter.toBlockPos());

       // if (lastDirection != null) {
       //     if (!rayIntersects(clientPos, lastDirection.create(this, aabb, clientEye, playerCenter))) {
       //         return true;
       //     }
       // }
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
        for (int i = 0; i < BLOCK_BOX_INDEX;) {
            int x = BLOCK_BOX[i++];
            int y = BLOCK_BOX[i++];
            int z = BLOCK_BOX[i++];
            byte blockBox = (byte) BLOCK_BOX[i++];
            BlockBox box = controller.level.blockShapes.getBlockBox(blockBox);

            if (box.rayIntersects(clientPos, rayDirection, x, y, z)) {
                return true;
            }
        }
        return false;
    }

    public void loadBoxes(Vec3i start, Vec3i end) {

       //var level = controller.level;
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
            add(x, y, z);
            if (x == x1 && y == y1 && z == z1) break;

            if (tMaxX < tMaxY) {
                if (tMaxX < tMaxZ) {
                    if (lastStep != GotoType.Z) {
                        add(x, y, z + 1);
                        add(x, y, z - 1);
                    }
                    if (lastStep != GotoType.Y) {
                        add(x, y + 1, z);
                        add(x, y - 1, z);

                        add(x, y + 1, z + 1);
                        add(x, y - 1, z + 1);

                        add(x, y + 1, z - 1);
                        add(x, y - 1, z - 1);
                    }
                    lastStep = GotoType.X;

                    tMaxX += tDeltaX;
                    x += sx;
                } else {
                    if (lastStep != GotoType.X) {
                        add(x + 1, y, z);
                        add(x - 1, y, z);

                        if (lastStep != GotoType.Y) {
                            add(x + 1, y + 1, z);
                            add(x + 1, y - 1, z);

                            add(x - 1, y + 1, z);
                            add(x - 1, y - 1, z);
                        }
                    }
                    if (lastStep != GotoType.Y) {
                        add(x, y + 1, z);
                        add(x, y - 1, z);
                    }
                    lastStep = GotoType.Z;

                    tMaxZ += tDeltaZ;
                    z += sz;
                }
            } else {
                if (tMaxY < tMaxZ) {
                    if (lastStep != GotoType.X) {
                        add(x + 1, y, z);
                        add(x - 1, y, z);
                    }
                    if (lastStep != GotoType.Z) {
                        add(x, y, z + 1);
                        add(x, y, z - 1);
                    }
                    lastStep = GotoType.Y;

                    tMaxY += tDeltaY;
                    y += sy;
                } else {
                    if (lastStep != GotoType.X) {
                        add(x + 1, y, z);
                        add(x - 1, y, z);

                        if (lastStep != GotoType.Y) {
                            add(x + 1, y + 1, z);
                            add(x + 1, y - 1, z);

                            add(x - 1, y + 1, z);
                            add(x - 1, y - 1, z);
                        }
                    }
                    if (lastStep != GotoType.Y) {
                        add(x, y + 1, z);
                        add(x, y - 1, z);
                    }
                    lastStep = GotoType.Z;

                    tMaxZ += tDeltaZ;
                    z += sz;
                }
            }
        }
    }
    private void add(int x, int y, int z){
        BLOCK_BOX[BLOCK_BOX_INDEX++] = x;
        BLOCK_BOX[BLOCK_BOX_INDEX++] = y;
        BLOCK_BOX[BLOCK_BOX_INDEX++] = z;
        BLOCK_BOX[BLOCK_BOX_INDEX++] = controller.level.getBlockBoxId(x, y, z) & 0xFF;
    }

    public enum GotoType {
        X, Y, Z
    }

    /*public void add(GotoType nextStep, GotoType currentStep, int x, int y, int z) {
        var level = controller.level;
        //BLOCK_BOX[BLOCK_BOX_INDEX++] = level.getBlockBox(x, y, z);

        if (nextStep == GotoType.Y) {
            if (currentStep != GotoType.X) {
                BLOCK_BOX[BLOCK_BOX_INDEX++] = level.getBlockBox(x + 1, y, z);
                BLOCK_BOX[BLOCK_BOX_INDEX++] = level.getBlockBox(x - 1, y, z);
            }
            if (currentStep != GotoType.Z) {
                BLOCK_BOX[BLOCK_BOX_INDEX++] = level.getBlockBox(x, y, z + 1);
                BLOCK_BOX[BLOCK_BOX_INDEX++] = level.getBlockBox(x, y, z - 1);
            }
        }

        if (nextStep == GotoType.Z) {
            if (currentStep != GotoType.X) {
                BLOCK_BOX[BLOCK_BOX_INDEX++] = level.getBlockBox(x + 1, y, z);
                BLOCK_BOX[BLOCK_BOX_INDEX++] = level.getBlockBox(x - 1, y, z);

                if (currentStep != GotoType.Y) {
                    BLOCK_BOX[BLOCK_BOX_INDEX++] = level.getBlockBox(x + 1, y + 1, z);
                    BLOCK_BOX[BLOCK_BOX_INDEX++] = level.getBlockBox(x + 1, y - 1, z);

                    BLOCK_BOX[BLOCK_BOX_INDEX++] = level.getBlockBox(x - 1, y + 1, z);
                    BLOCK_BOX[BLOCK_BOX_INDEX++] = level.getBlockBox(x - 1, y - 1, z);
                }
            }
            if (currentStep != GotoType.Y) {
                BLOCK_BOX[BLOCK_BOX_INDEX++] = level.getBlockBox(x, y + 1, z);
                BLOCK_BOX[BLOCK_BOX_INDEX++] = level.getBlockBox(x, y - 1, z);
            }
        }
        if (nextStep == GotoType.X) {
            if (currentStep != GotoType.Z) {
                BLOCK_BOX[BLOCK_BOX_INDEX++] = level.getBlockBox(x, y, z + 1);
                BLOCK_BOX[BLOCK_BOX_INDEX++] = level.getBlockBox(x, y, z - 1);
            }
            if (currentStep != GotoType.Y) {
                BLOCK_BOX[BLOCK_BOX_INDEX++] = level.getBlockBox(x, y + 1, z);
                BLOCK_BOX[BLOCK_BOX_INDEX++] = level.getBlockBox(x, y - 1, z);

                BLOCK_BOX[BLOCK_BOX_INDEX++] = level.getBlockBox(x, y + 1, z + 1);
                BLOCK_BOX[BLOCK_BOX_INDEX++] = level.getBlockBox(x, y - 1, z + 1);

                BLOCK_BOX[BLOCK_BOX_INDEX++] = level.getBlockBox(x, y + 1, z - 1);
                BLOCK_BOX[BLOCK_BOX_INDEX++] = level.getBlockBox(x, y - 1, z - 1);
            }
        }
    }*/

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
