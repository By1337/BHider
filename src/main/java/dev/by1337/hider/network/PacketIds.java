package dev.by1337.hider.network;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;

import java.util.Objects;

public class PacketIds {
    public static int ADD_PLAYER = getId(new ClientboundAddPlayerPacket());
    public static int SET_EQUIPMENT_PACKET = getId(new ClientboundSetEquipmentPacket());

    private static int getId(Packet<?> packet){
        return Objects.requireNonNull(ConnectionProtocol.PLAY.getPacketId(PacketFlow.CLIENTBOUND, packet));
    }
}
