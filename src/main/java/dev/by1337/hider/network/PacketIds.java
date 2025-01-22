package dev.by1337.hider.network;

import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.*;

import java.util.Objects;

public class PacketIds {
    public static int ADD_PLAYER = getId(new ClientboundAddPlayerPacket());
    public static int SET_EQUIPMENT_PACKET = getId(new ClientboundSetEquipmentPacket());
    public static int SET_ENTITY_DATA_PACKET = getId(new ClientboundSetEntityDataPacket());
    public static int MOVE_ENTITY_PACKET_POS = getId(new ClientboundMoveEntityPacket.Pos());
    public static int MOVE_ENTITY_PACKET_ROT = getId(new ClientboundMoveEntityPacket.Rot());
    public static int MOVE_ENTITY_PACKET_POS_ROT = getId(new ClientboundMoveEntityPacket.PosRot());
    public static int LOAD_LEVEL_CHUNK = getId(new ClientboundLevelChunkPacket());
    public static int FORGET_LEVEL_CHUNK = getId(new ClientboundForgetLevelChunkPacket());
    public static int SECTION_BLOCKS_UPDATE = getId(new ClientboundSectionBlocksUpdatePacket());
    public static int BLOCK_UPDATE_PACKET = getId(new ClientboundBlockUpdatePacket());
    public static int EXPLODE_PACKET = getId(new ClientboundExplodePacket());

    private static int getId(Packet<?> packet) {
        return Objects.requireNonNull(ConnectionProtocol.PLAY.getPacketId(PacketFlow.CLIENTBOUND, packet));
    }
}
