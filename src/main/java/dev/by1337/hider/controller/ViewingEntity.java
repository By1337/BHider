package dev.by1337.hider.controller;

import dev.by1337.hider.network.packet.Packet;
import net.minecraft.world.phys.AABB;
import org.bukkit.entity.Entity;
import org.by1337.blib.geom.Vec3d;

public interface ViewingEntity {
    void onPacket(Packet packet);
    void tick();
    Entity getBukkitEntity();
    AABB getAABB();
}
