package dev.by1337.hider;

import com.mojang.datafixers.util.Pair;
import dev.by1337.hider.mutator.SetEquipmentPacketMutator;
import dev.by1337.hider.network.PacketIds;
import dev.by1337.hider.network.packet.AddPlayerPacket;
import dev.by1337.hider.network.packet.MoveEntityPacket;
import dev.by1337.hider.network.packet.SetEntityDataPacket;
import dev.by1337.hider.network.packet.SetEquipmentPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class PlayerController implements Closeable {
    private final Logger logger;
    private final Supplier<Player> playerSupplier;
    private final Plugin plugin;
    private final UUID uuid;
    private final Map<Integer, PlayerData> viewingPlayers = new ConcurrentHashMap<>();
    private final BukkitTask task;
    private final Channel channel;
    private final ServerPlayer player;

    public PlayerController(Supplier<Player> playerSupplier, Plugin plugin, UUID uuid, Channel channel) {
        this.playerSupplier = playerSupplier;
        this.plugin = plugin;
        this.uuid = uuid;
        logger = LoggerFactory.getLogger(playerSupplier.get().getName());
        task = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::tick, 1, 1);
        this.channel = channel;
        player = ((CraftPlayer) playerSupplier.get()).getHandle();
    }

    private void tick() {
        for (PlayerData value : viewingPlayers.values()) {
            value.tick();
        }
    }

    public void onPacket(ChannelHandlerContext ctx, ByteBuf in0, ByteBuf out) {
        FriendlyByteBuf in = new FriendlyByteBuf(in0);
        int packetId = in.readVarInt_();
        in.resetReaderIndex();
        if (packetId == PacketIds.ADD_PLAYER) {
            onPacket(new AddPlayerPacket(in, new FriendlyByteBuf(out)));
        } else if (packetId == PacketIds.SET_EQUIPMENT_PACKET) {
            onPacket(new SetEquipmentPacket(in, new FriendlyByteBuf(out)));
        } else if (packetId == PacketIds.SET_ENTITY_DATA_PACKET) {
            onPacket(new SetEntityDataPacket(in, new FriendlyByteBuf(out)));
        } else if (packetId == PacketIds.MOVE_ENTITY_PACKET_POS) {
            onPacket(new MoveEntityPacket.Pos(in, new FriendlyByteBuf(out)));
        } else if (packetId == PacketIds.MOVE_ENTITY_PACKET_ROT) {
            onPacket(new MoveEntityPacket.Rot(in, new FriendlyByteBuf(out)));
        } else if (packetId == PacketIds.MOVE_ENTITY_PACKET_POS_ROT) {
            onPacket(new MoveEntityPacket.PosRot(in, new FriendlyByteBuf(out)));
        } else {
            Packet<?> packet = ConnectionProtocol.PLAY.createPacket(PacketFlow.CLIENTBOUND, packetId);
            if (
                    !(packet instanceof ClientboundLightUpdatePacket) &&
                            !(packet instanceof ClientboundSetTimePacket)
                            && packet != null)
                logger.info(packet.getClass().getName());


            in0.resetReaderIndex();
            out.writeBytes(in0);
        }

    }

    private void onPacket(dev.by1337.hider.network.packet.Packet packet) {
        logger.info(packet.toString());
        if (packet instanceof AddPlayerPacket addPlayerPacket) {
            PlayerData playerData = new PlayerData(addPlayerPacket);
            System.out.println(playerData);
            viewingPlayers.put(playerData.entityId, playerData);
        } else if (packet instanceof SetEquipmentPacket setEquipmentPacket) {
            PlayerData playerData = viewingPlayers.get(setEquipmentPacket.entityId());
            if (playerData != null) {
                playerData.setEquipmentPacket(setEquipmentPacket);
            }
        } else if (packet instanceof SetEntityDataPacket packet1) {
            PlayerData playerData = viewingPlayers.get(packet1.entityId());
            if (playerData != null) {
                playerData.setEntityDataPacket(packet1);
            }
        } else if (packet instanceof MoveEntityPacket packet1) {
            PlayerData playerData = viewingPlayers.get(packet1.getEntityId());
            if (playerData != null) {
                playerData.onMove(packet1);
            }
        } else {
            packet.writeOut();
        }
    }

    @Override
    public void close() {
        task.cancel();
    }

    public class PlayerData {
        private int entityId;
        private UUID playerId;
        private double x;
        private double y;
        private double z;
        private final Map<EquipmentSlot, ItemStack> equipment = new HashMap<>();
        private boolean hideArmor;
        private boolean isShift;
        private boolean isGlowing;
        private final Supplier<Player> playerSupplier;
        private final ServerPlayer serverPlayer;
        private boolean suppressArmorUpdate;

        public PlayerData(AddPlayerPacket packet) {
            entityId = packet.entityId();
            playerId = packet.playerId();
            x = packet.x();
            y = packet.y();
            z = packet.z();
            packet.writeOut();
            playerSupplier = () -> Bukkit.getPlayer(playerId);
            serverPlayer = ((CraftPlayer) playerSupplier.get()).getHandle();
        }

        public void tick() {
            //  hideArmor.setVote(0, !isGlowing && isShift);
            //  hideArmor.setVote(1, new Vec3d(PlayerController.this.playerSupplier.get().getLocation()).distance(x, y, z) > 5);


//            hideArmor.result();
//            if (hideArmor.changed()) {
//                if (hideArmor.is()) {
//                    hideArmor();
//                } else {
//                    unhideArmor();
//                }
//            }
            if (false) {
                if (!hideArmor) {
                    hideArmor();
                    hideArmor = true;
                }
            } else if (hideArmor) {
                unhideArmor();
                hideArmor = false;
            }
        }

        public void onMove(MoveEntityPacket packet) {
            x = serverPlayer.lastX;
            y = serverPlayer.lastY;
            z = serverPlayer.lastZ;

            packet.writeOut();
        }


        public void setEntityDataPacket(SetEntityDataPacket packet) {
            isShift = serverPlayer.isSneaking();
            isGlowing = serverPlayer.isGlowing();
            packet.writeOut();
        }

        public void setEquipmentPacket(SetEquipmentPacket packet) {
            var list = packet.slots();
            for (Pair<EquipmentSlot, ItemStack> pair : list) {
                equipment.put(pair.getFirst(), pair.getSecond());
            }
            if (suppressArmorUpdate) return;
            SetEquipmentPacketMutator.obfuscate(packet);
            packet.writeOut();
        }

        public void hideArmor() {
            suppressArmorUpdate = true;
            SetEquipmentPacket equipmentPacket = new SetEquipmentPacket(
                    PacketIds.SET_EQUIPMENT_PACKET,
                    entityId,
                    SetEquipmentPacketMutator.EMPTY_EQUIPMENTS
            );
            channel.write(equipmentPacket);
        }

        public void unhideArmor() {
            suppressArmorUpdate = false;
            SetEquipmentPacket equipmentPacket = new SetEquipmentPacket(
                    PacketIds.SET_EQUIPMENT_PACKET,
                    entityId,
                    equipment.entrySet().stream().map(e -> Pair.of(e.getKey(), e.getValue())).toList()
            );
            channel.write(equipmentPacket);
        }

        public boolean isArmorHide() {
            return hideArmor;
        }

        @Override
        public String toString() {
            return "PlayerData{" +
                    "entityId=" + entityId +
                    ", playerId=" + playerId +
                    ", x=" + x +
                    ", y=" + y +
                    ", z=" + z +
                    '}';
        }
    }
}
