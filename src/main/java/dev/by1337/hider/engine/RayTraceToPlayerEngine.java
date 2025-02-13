package dev.by1337.hider.engine;

import dev.by1337.hider.PlayerController;
import dev.by1337.hider.controller.ViewingEntity;
import dev.by1337.hider.shapes.BlockBox;
import dev.by1337.hider.util.MutableVec3d;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.by1337.blib.geom.NumberUtil;
import org.by1337.blib.util.lock.AutoReadWriteLock;
import org.jetbrains.annotations.Nullable;

public class RayTraceToPlayerEngine {
    private static final int[] BLOCK_BOX = new int[3072 * 4]; // рассчитано что будет всего один поток поэтому это static
    private static int BLOCK_BOX_INDEX;
    private static final Vec3dCreator[] RAY_DIRECTIONS;
    private final PlayerController controller;
    private final ViewingEntity viewingEntity;

    private final MutableVec3d lastClientPos = new MutableVec3d();
    private final MutableVec3d lastPlayerPos = new MutableVec3d();
    private boolean lastState;
    private final AutoReadWriteLock lock = new AutoReadWriteLock();


    private @Nullable Vec3dCreator lastDirection;

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

    private final MutableVec3d clientEye = new MutableVec3d();
    private final MutableVec3d playerCenter = new MutableVec3d();

    private boolean noneMatch0() {
        ServerPlayer client = controller.client;
        double clientPosX = client.lastX;
        double clientPosY = client.getHeadY();
        double clientPosZ = client.lastZ;

        Entity entity = ((CraftEntity) viewingEntity.getBukkitEntity()).getHandle();
        double playerPosX = entity.lastX;
        double playerPosY = entity.lastY;
        double playerPosZ = entity.lastZ;

        // исключаем ситуации когда рейтрейс был проверен с неполной информацией о чанках
        if (controller.ticksLived > 100 && lastClientPos.equals(clientPosX, clientPosY, clientPosZ) && lastPlayerPos.equals(playerPosX, playerPosY, playerPosZ)) {
            return lastState;
        }

        lastClientPos.x = clientPosX;
        lastClientPos.y = clientPosY;
        lastClientPos.z = clientPosZ;

        lastPlayerPos.x = playerPosX;
        lastPlayerPos.y = playerPosY;
        lastPlayerPos.z = playerPosZ;

        clientEye.set(client.lastX, client.getHeadY(), client.lastZ);

        var extraSize = controller.config.expandAabb;
        var aabb = viewingEntity.getAABB().expand(extraSize.x, extraSize.y, extraSize.z);
        playerCenter.set(aabb.maxX + aabb.minX, aabb.maxY + aabb.minY, aabb.maxZ + aabb.minZ).div(2);

        BLOCK_BOX_INDEX = 0;


        loadBoxes(
                NumberUtil.floor(clientEye.x),
                NumberUtil.floor(clientEye.y),
                NumberUtil.floor(clientEye.z),

                NumberUtil.floor(playerCenter.x),
                NumberUtil.floor(playerCenter.y),
                NumberUtil.floor(playerCenter.z)
        );

        if (lastDirection != null) {
            if (!rayIntersects(lastClientPos, lastDirection.create(this, aabb, clientEye, playerCenter))) {
                return true;
            }
        }
        for (Vec3dCreator rayDirection : RAY_DIRECTIONS) {
            if (rayDirection == lastDirection) continue;
            if (!rayIntersects(lastClientPos, rayDirection.create(this, aabb, clientEye, playerCenter))) {
                lastDirection = rayDirection;
                return true;
            }
        }
        return false;
    }

    private boolean rayIntersects(MutableVec3d clientPos, MutableVec3d rayDirection) {
        for (int i = 0; i < BLOCK_BOX_INDEX; ) {
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

    private void loadBoxes(int startX, int startY, int startZ, int endX, int endY, int endZ) {

        int dx = Math.abs(endX - startX), dy = Math.abs(endY - startY), dz = Math.abs(endZ - startZ);
        int sx = startX < endX ? 1 : -1;
        int sy = startY < endY ? 1 : -1;
        int sz = startZ < endZ ? 1 : -1;

        double tMaxX = dx == 0 ? Double.MAX_VALUE : (0.5 / dx);
        double tMaxY = dy == 0 ? Double.MAX_VALUE : (0.5 / dy);
        double tMaxZ = dz == 0 ? Double.MAX_VALUE : (0.5 / dz);

        double tDeltaX = dx == 0 ? Double.MAX_VALUE : (1.0 / dx);
        double tDeltaY = dy == 0 ? Double.MAX_VALUE : (1.0 / dy);
        double tDeltaZ = dz == 0 ? Double.MAX_VALUE : (1.0 / dz);

        int x = startX, y = startY, z = startZ;

        GotoType lastStep = null;
        while (true) {
            add(x, y, z);
            if (x == endX && y == endY && z == endZ) break;

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

    private void add(int x, int y, int z) {
        BLOCK_BOX[BLOCK_BOX_INDEX++] = x;
        BLOCK_BOX[BLOCK_BOX_INDEX++] = y;
        BLOCK_BOX[BLOCK_BOX_INDEX++] = z;
        BLOCK_BOX[BLOCK_BOX_INDEX++] = controller.level.getBlockBoxId(x, y, z) & 0xFF;
    }

    public enum GotoType {
        X, Y, Z
    }

    static {
        RAY_DIRECTIONS = new Vec3dCreator[]{
                new Vec3dCreator() {
                    @Override
                    public MutableVec3d create(MutableVec3d instance, RayTraceToPlayerEngine engine, AABB aabb, MutableVec3d clientEye, MutableVec3d playerCenter) {
                        return instance.set(aabb.minX, aabb.minY, aabb.minZ).sub(clientEye).normalize();
                    }
                },

                new Vec3dCreator() {
                    @Override
                    public MutableVec3d create(MutableVec3d instance, RayTraceToPlayerEngine engine, AABB aabb, MutableVec3d clientEye, MutableVec3d playerCenter) {
                        return instance.set(aabb.minX, aabb.minY, aabb.maxZ).sub(clientEye).normalize();
                    }
                },
                new Vec3dCreator() {
                    @Override
                    public MutableVec3d create(MutableVec3d instance, RayTraceToPlayerEngine engine, AABB aabb, MutableVec3d clientEye, MutableVec3d playerCenter) {
                        return instance.set(aabb.minX, aabb.maxY, aabb.maxZ).sub(clientEye).normalize();
                    }
                },

                new Vec3dCreator() {
                    @Override
                    public MutableVec3d create(MutableVec3d instance, RayTraceToPlayerEngine engine, AABB aabb, MutableVec3d clientEye, MutableVec3d playerCenter) {
                        return instance.set(aabb.minX, aabb.maxY, aabb.minZ).sub(clientEye).normalize();
                    }
                },


                new Vec3dCreator() {
                    @Override
                    public MutableVec3d create(MutableVec3d instance, RayTraceToPlayerEngine engine, AABB aabb, MutableVec3d clientEye, MutableVec3d playerCenter) {
                        return instance.set(aabb.maxX, aabb.minY, aabb.minZ).sub(clientEye).normalize();
                    }
                },

                new Vec3dCreator() {
                    @Override
                    public MutableVec3d create(MutableVec3d instance, RayTraceToPlayerEngine engine, AABB aabb, MutableVec3d clientEye, MutableVec3d playerCenter) {
                        return instance.set(aabb.maxX, aabb.minY, aabb.maxZ).sub(clientEye).normalize();
                    }
                },
                new Vec3dCreator() {
                    @Override
                    public MutableVec3d create(MutableVec3d instance, RayTraceToPlayerEngine engine, AABB aabb, MutableVec3d clientEye, MutableVec3d playerCenter) {
                        return instance.set(aabb.maxX, aabb.maxY, aabb.maxZ).sub(clientEye).normalize();
                    }
                },

                new Vec3dCreator() {
                    @Override
                    public MutableVec3d create(MutableVec3d instance, RayTraceToPlayerEngine engine, AABB aabb, MutableVec3d clientEye, MutableVec3d playerCenter) {
                        return instance.set(aabb.maxX, aabb.maxY, aabb.minZ).sub(clientEye).normalize();

                    }
                },

                new Vec3dCreator() {
                    @Override
                    public MutableVec3d create(MutableVec3d instance, RayTraceToPlayerEngine engine, AABB aabb, MutableVec3d clientEye, MutableVec3d playerCenter) {
                        var center = aabb.getCenter();
                        return instance.set(center.x, center.y, center.z).sub(clientEye).normalize();
                    }
                }
        };
    }
    @FunctionalInterface
    private interface RayDirectionCreator {
        MutableVec3d create(MutableVec3d instance, RayTraceToPlayerEngine engine, AABB aabb, MutableVec3d clientEye, MutableVec3d playerCenter);
    }

    public static abstract class Vec3dCreator implements RayDirectionCreator { // рассчитано на работу в одном потоке
        private final MutableVec3d instance = new MutableVec3d();

        public MutableVec3d create(RayTraceToPlayerEngine engine, AABB aabb, MutableVec3d clientEye, MutableVec3d playerCenter) {
            return create(instance, engine, aabb, clientEye, playerCenter);
        }
    }
}
