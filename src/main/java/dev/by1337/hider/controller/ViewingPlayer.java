package dev.by1337.hider.controller;

import com.mojang.datafixers.util.Pair;
import dev.by1337.hider.PlayerController;
import dev.by1337.hider.config.Config;
import dev.by1337.hider.engine.RayTraceToPlayerEngine;
import dev.by1337.hider.mutator.SetEquipmentPacketMutator;
import dev.by1337.hider.network.PacketIds;
import dev.by1337.hider.network.packet.*;
import dev.by1337.hider.util.BoolWatcher;
import io.netty.channel.Channel;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ViewingPlayer implements ViewingEntity {
    private final PlayerController clientController;
    public final Logger logger;

    public final int entityId;
    public final UUID uuid;
    private final Map<EquipmentSlot, ItemStack> equipment = new HashMap<>();
    private final BoolWatcher hideArmor = new BoolWatcher(false);
    private final BoolWatcher fullHide = new BoolWatcher(false);
    public final ServerPlayer player;
    public final ServerPlayer client;
    private boolean suppressArmorUpdate;
    private boolean suppressUpdate;
    private final RayTraceToPlayerEngine rayTraceEngine;
    private final Config config;
    public final Channel channel;
    private final double fieldOfView;

    public ViewingPlayer(PlayerController clientController, AddPlayerPacket packet) {
        this.clientController = clientController;
        logger = clientController.logger;
        entityId = packet.entityId();
        uuid = packet.playerId();
        channel = clientController.channel;
        client = clientController.client;
        player = ((CraftPlayer) Bukkit.getPlayer(uuid)).getHandle();
        config = clientController.config;
        fieldOfView = config.armorHide.fieldOfView;
        rayTraceEngine = new RayTraceToPlayerEngine(clientController, this);
    }

    @Override
    public void onPacket(Packet packet) {
        if (packet instanceof MoveEntityPacket) {
            onMove((MoveEntityPacket) packet);
            if (suppressUpdate) packet.setCanceled(true);
        } else if (packet instanceof SetEquipmentPacket) {
            setEquipmentPacket((SetEquipmentPacket) packet);
            if (suppressUpdate) packet.setCanceled(true);
        } else if (packet instanceof SetEntityDataPacket) {
            setEntityDataPacket((SetEntityDataPacket) packet);
            if (suppressUpdate) packet.setCanceled(true);
        }
    }

    @Override
    public void tick(long tick) {
        //hideArmorTick();
        visibleTick();
    }

    @Override
    public Entity getBukkitEntity() {
        return player.getBukkitEntity();
    }

    @Override
    public AABB getAABB() {
        return player.getBoundingBox();
    }

    private void visibleTick() {

        fullHide.set(player.isSneaking() && !isVisible());

        if (fullHide.isDirty()) {
            if (fullHide.get()) {
                suppressUpdate = true;
                channel.writeAndFlush(new RemoveEntitiesPacket(entityId));
            } else {
                suppressUpdate = false;
                channel.writeAndFlush(player.getAddEntityPacket());
                channel.writeAndFlush(new ClientboundSetEntityDataPacket(entityId, player.getDataWatcher(), true));
                sendActualEquip(); // todo hide armor check
            }
            fullHide.setDirty(false);
        }
    }

    private void hideArmorTick() {
        if (equipment.isEmpty()) return;
        if (config.armorHide.disableWorlds.contains(((ServerLevel) client.world).worldDataServer.getName())) return;
        long l = System.nanoTime();
        hideArmor.set(!isVisible());
        long l1 = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - l);
        if (l1 > 10) {
            logger.error("isVisible Time {}", l1);
        } else if (l1 > 0) {
            logger.warn("isVisible Time {}", l1);
        }

        if (hideArmor.isDirty()) {
            if (hideArmor.get()) {
                hideArmor();
            } else {
                unhideArmor();
            }
            hideArmor.setDirty(false);
        }
    }


    public boolean isVisible() {
        var clientEye = client.getBukkitEntity().getEyeLocation();
        Vector directionToTarget = player.getBukkitEntity().getLocation().add(0, -0.5, 0)
                .toVector().subtract(clientEye.toVector()).normalize();
        if (directionToTarget.angle(clientEye.getDirection()) >= fieldOfView) return false;

        return rayTraceEngine.noneMatch();
    }

    private void onMove(MoveEntityPacket packet) {
    }


    private void setEntityDataPacket(SetEntityDataPacket packet) {
        //  isShift = me.isSneaking();
        //  isGlowing = me.isGlowing();
    }

    private void setEquipmentPacket(SetEquipmentPacket packet) {
        var list = packet.slots();
        for (Pair<EquipmentSlot, ItemStack> pair : list) {
            equipment.put(pair.getFirst(), pair.getSecond());
        }
        equipment.entrySet().removeIf(e -> e.getValue().isEmpty());
        if (suppressArmorUpdate || suppressUpdate) {
            packet.setCanceled(true);
            return;
        }
        if (config.armorHide.hideMeta)
            SetEquipmentPacketMutator.obfuscate(packet);
    }

    private void hideArmor() {
        suppressArmorUpdate = true;
        SetEquipmentPacket equipmentPacket = new SetEquipmentPacket(
                PacketIds.SET_EQUIPMENT_PACKET,
                entityId,
                SetEquipmentPacketMutator.EMPTY_EQUIPMENTS
        );
        channel.writeAndFlush(equipmentPacket);
    }

    private void unhideArmor() {
        suppressArmorUpdate = false;
        sendActualEquip();
    }

    private void sendActualEquip() {
        if (equipment.isEmpty()) return;
        SetEquipmentPacket equipmentPacket = new SetEquipmentPacket(
                PacketIds.SET_EQUIPMENT_PACKET,
                entityId,
                equipment.entrySet().stream().map(e -> Pair.of(e.getKey(), e.getValue())).toList()
        );
        channel.writeAndFlush(equipmentPacket);
    }

    @Override
    public String toString() {
        return "PlayerData{" +
                "entityId=" + entityId +
                ", playerId=" + uuid +
                '}';
    }
}
