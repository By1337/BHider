package dev.by1337.hider;

import com.mojang.datafixers.util.Pair;
import dev.by1337.hider.mutator.SetEquipmentPacketMutator;
import dev.by1337.hider.network.PacketIds;
import dev.by1337.hider.network.packet.AddPlayerPacket;
import dev.by1337.hider.network.packet.SetEquipmentPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
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

    public PlayerController(Supplier<Player> playerSupplier, Plugin plugin, UUID uuid) {
        this.playerSupplier = playerSupplier;
        this.plugin = plugin;
        this.uuid = uuid;
        logger = LoggerFactory.getLogger(playerSupplier.get().getName());
    }

    public void onPacket(ChannelHandlerContext ctx, ByteBuf in0, ByteBuf out) {
        FriendlyByteBuf in = new FriendlyByteBuf(in0);
        int packetId = in.readVarInt_();
        in.resetReaderIndex();
        if (packetId == PacketIds.ADD_PLAYER) {
            onPacket(new AddPlayerPacket(in, new FriendlyByteBuf(out)));
        } else if (packetId == PacketIds.SET_EQUIPMENT_PACKET) {
            onPacket(new SetEquipmentPacket(in, new FriendlyByteBuf(out)));
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
        }

        packet.writeOut();
    }

    @Override
    public void close() {

    }

    public static class PlayerData {
        private int entityId;
        private UUID playerId;
        private double x;
        private double y;
        private double z;
        private final Map<EquipmentSlot, ItemStack> equipment = new HashMap<>();

        public PlayerData(AddPlayerPacket addPlayerPacket) {
            entityId = addPlayerPacket.entityId();
            playerId = addPlayerPacket.playerId();
            x = addPlayerPacket.x();
            y = addPlayerPacket.y();
            z = addPlayerPacket.z();
        }

        public void setEquipmentPacket(SetEquipmentPacket packet) {

            var list = packet.slots();
            for (Pair<EquipmentSlot, ItemStack> pair : list) {
                equipment.put(pair.getFirst(), pair.getSecond());
            }
            SetEquipmentPacketMutator.obfuscate(packet);
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
