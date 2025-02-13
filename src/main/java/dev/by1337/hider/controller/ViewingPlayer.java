package dev.by1337.hider.controller;

import com.mojang.datafixers.util.Pair;
import dev.by1337.hider.PlayerController;
import dev.by1337.hider.config.Config;
import dev.by1337.hider.engine.RayTraceToPlayerEngine;
import dev.by1337.hider.mutator.SetEquipmentPacketMutator;
import dev.by1337.hider.network.packet.*;
import dev.by1337.hider.util.BoolWatcher;
import dev.by1337.hider.util.CashedSupplier;
import io.netty.channel.Channel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
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
    private final CashedSupplier<Boolean> isVisible;

    public ViewingPlayer(PlayerController clientController, AddPlayerPacket packet) {
        this.clientController = clientController;
        logger = clientController.logger;
        entityId = packet.entityId();
        uuid = packet.playerId();
        channel = clientController.channel;
        client = clientController.client;
        player = ((CraftPlayer) Bukkit.getPlayer(uuid)).getHandle();
        config = clientController.config;
        fieldOfView = config.fieldOfView;
        rayTraceEngine = new RayTraceToPlayerEngine(clientController, this);
        isVisible = new CashedSupplier<>(() -> {
            var clientEye = client.getBukkitEntity().getEyeLocation();
            Vector directionToTarget = player.getBukkitEntity().getLocation().add(0, -0.5, 0)
                    .toVector().subtract(clientEye.toVector()).normalize();
            if (directionToTarget.angle(clientEye.getDirection()) >= fieldOfView) return false;

            return rayTraceEngine.noneMatch();
        });

        if (clientController.bypassHide) return;
        fullHide.set(!isGlowing() && (isHideNickName() || isInvisible()) && !isVisible.get());
        fullHide.setDirty(false);
        packet.setCanceled(fullHide.get()); // я попытался, пакеты чанков могут прийти позже из-за чего при заходе на сервер некоторые игроки могут некоторое время быть видимыми
    }

    @Override
    public void onPacket(Packet packet) {
        if (packet instanceof MoveEntityPacket) {
            if (suppressUpdate) packet.setCanceled(true);
        } else if (packet instanceof SetEquipmentPacket) {
            setEquipmentPacket((SetEquipmentPacket) packet);
            if (suppressUpdate) packet.setCanceled(true);
        } else if (packet instanceof SetEntityDataPacket) {
            if (suppressUpdate) packet.setCanceled(true);
        } else if (packet instanceof RotateHeadPacket) {
            if (suppressUpdate) packet.setCanceled(true);
        } else if (packet instanceof TeleportEntityPacket) {
            if (suppressUpdate) packet.setCanceled(true);
        } else if (packet instanceof AnimatePacket) {
            if (suppressUpdate) packet.setCanceled(true);
        } else if (packet instanceof AddPlayerPacket) {
            if (suppressUpdate) packet.setCanceled(true);
        }
    }

    @Override
    public void tick() {
        visibleTick();
        hideArmorTick();
        isVisible.invalidate();
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
        if (clientController.bypassHide) return;
        if (config.hideSettings.disableWorlds.contains(((ServerLevel) client.world).worldDataServer.getName())) {
            fullHide.set(false);
        } else {
            fullHide.set(!isGlowing() && (isHideNickName() || isInvisible()) && !isVisible.get());
        }


        if (fullHide.isDirty()) {
            if (fullHide.get()) {
                suppressUpdate = true;
                channel.writeAndFlush(new RemoveEntitiesPacket(entityId));
            } else {
                suppressUpdate = false;
                channel.write(new AddPlayerPacket(
                        entityId,
                        uuid,
                        player.locX(),
                        player.locY(),
                        player.locZ(),
                        (byte) ((int) (player.yaw * 256.0F / 360.0F)),
                        (byte) ((int) (player.pitch * 256.0F / 360.0F))
                ));
                channel.write(new SetEntityDataPacket(
                        entityId,
                        player.getDataWatcher().getAll()
                ));
                channel.write(new RotateHeadPacket(
                        entityId,
                        (byte) ((int) (player.yaw * 256.0F / 360.0F))
                ));
                if (!equipment.isEmpty() && isVisible.get()) {
                    sendActualEquip();
                } else {
                    channel.flush();
                }

            }
            fullHide.setDirty(false);
        }
    }

    private boolean isInvisible() {
        return (player.isInvisible() || player.hasEffect(MobEffects.INVISIBILITY));
    }

    private boolean isHideNickName() {
        return player.isSneaking() || player.isSpectator();
    }

    private boolean isGlowing() {
        return player.isGlowing() || player.hasEffect(MobEffects.GLOWING);
    }

    private void hideArmorTick() {
        if (clientController.bypassArmor) return;
        if (equipment.isEmpty() || suppressUpdate) return;
        if (config.armorHide.disableWorlds.contains(((ServerLevel) client.world).worldDataServer.getName())) {
            if (hideArmor.get()) {
                hideArmor.set(false);
                hideArmor.setDirty(false);
                unhideArmor();
            }
            return;
        }

        hideArmor.set(!isVisible.get());

        if (hideArmor.isDirty()) {
            if (hideArmor.get()) {
                hideArmor();
            } else {
                unhideArmor();
            }
            hideArmor.setDirty(false);
        }
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
                entityId,
                equipment.entrySet().stream().map(e -> Pair.of(e.getKey(), e.getValue())).toList()
        );
        if (config.armorHide.hideMeta) SetEquipmentPacketMutator.obfuscate(equipmentPacket);
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
