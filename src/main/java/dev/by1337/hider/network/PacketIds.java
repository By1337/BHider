package dev.by1337.hider.network;

import dev.by1337.hider.network.packet.*;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.*;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
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
    public static int REMOVE_ENTITIES_PACKET = getId(new ClientboundRemoveEntitiesPacket());
    public static int ROTATE_HEAD_PACKET = getId(new ClientboundRotateHeadPacket());
    public static int TELEPORT_ENTITY_PACKET = getId(new ClientboundTeleportEntityPacket());
    public static int ANIMATE_PACKET = getId(new ClientboundAnimatePacket());

    private static final Map<Integer, PacketCreator> packetCreators = new HashMap<>();

    private static int getId(Packet<?> packet) {
        return Objects.requireNonNull(ConnectionProtocol.PLAY.getPacketId(PacketFlow.CLIENTBOUND, packet));
    }

    @Nullable
    public static PacketCreator getCreator(int id) {
        return packetCreators.get(id);
    }

    static {
        packetCreators.put(ADD_PLAYER, AddPlayerPacket::new);
        packetCreators.put(SET_EQUIPMENT_PACKET, SetEquipmentPacket::new);
        packetCreators.put(SET_ENTITY_DATA_PACKET, SetEntityDataPacket::new);
        packetCreators.put(MOVE_ENTITY_PACKET_POS, MoveEntityPacket.Pos::new);
        packetCreators.put(MOVE_ENTITY_PACKET_ROT, MoveEntityPacket.Rot::new);
        packetCreators.put(MOVE_ENTITY_PACKET_POS_ROT, MoveEntityPacket.PosRot::new);
        packetCreators.put(LOAD_LEVEL_CHUNK, LevelChunkPacket::new);
        packetCreators.put(FORGET_LEVEL_CHUNK, ForgetLevelChunkPacket::new);
        packetCreators.put(SECTION_BLOCKS_UPDATE, SectionBlocksUpdatePacket::new);
        packetCreators.put(BLOCK_UPDATE_PACKET, BlockUpdatePacket::new);
        packetCreators.put(EXPLODE_PACKET, ExplodePacket::new);
        packetCreators.put(REMOVE_ENTITIES_PACKET, RemoveEntitiesPacket::new);
        packetCreators.put(ROTATE_HEAD_PACKET, RotateHeadPacket::new);
        packetCreators.put(TELEPORT_ENTITY_PACKET, TeleportEntityPacket::new);
        packetCreators.put(ANIMATE_PACKET, AnimatePacket::new);
    }

    public interface PacketCreator {
        dev.by1337.hider.network.packet.Packet create(FriendlyByteBuf in);
    }
}
