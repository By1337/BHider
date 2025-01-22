package dev.by1337.hider;

import com.mojang.datafixers.util.Pair;
import dev.by1337.hider.mutator.SetEquipmentPacketMutator;
import dev.by1337.hider.network.PacketIds;
import dev.by1337.hider.network.packet.*;
import dev.by1337.hider.world.VirtualWorld;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.by1337.blib.geom.Vec2i;
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
    private final VirtualWorld level = new VirtualWorld();

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
        if (player.isSneaking()) {
            Chunk chunk = player.getBukkitEntity().getLocation().getChunk();
            logger.info("Сверяю блоки в чанке {} {}...", chunk.getX(), chunk.getZ());
            for (int x = 0; x < 15; x++) {
                for (int y = 0; y < 255; y++) {
                    for (int z = 0; z < 15; z++) {
                        var b = chunk.getBlock(x, y, z);
                        var playerBlock = level.getBlock(chunk.getX() << 4 | x, y, chunk.getZ() << 4 | z);
                        if (b.getType().isAir() && (playerBlock == null || playerBlock.getBukkitMaterial().isAir()))
                            continue;
                        if (playerBlock != null && b.getType() == playerBlock.getBukkitMaterial()) continue;
                        logger.warn("Неправильный блок на координатах {} {} {} ожидался {} а получен {}", b.getX(), b.getY(), b.getZ(), b.getType().name(), playerBlock.getBukkitMaterial().name());
                    }
                }
            }
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
        } else if (packetId == PacketIds.LOAD_LEVEL_CHUNK) {
            onPacket(new LevelChunkPacket(in, new FriendlyByteBuf(out)));
        } else if (packetId == PacketIds.FORGET_LEVEL_CHUNK) {
            onPacket(new ForgetLevelChunkPacket(in, new FriendlyByteBuf(out)));
        } else if (packetId == PacketIds.SECTION_BLOCKS_UPDATE) {
            onPacket(new SectionBlocksUpdatePacket(in, new FriendlyByteBuf(out)));
        } else if (packetId == PacketIds.BLOCK_UPDATE_PACKET) {
            onPacket(new BlockUpdatePacket(in, new FriendlyByteBuf(out)));
        } else if (packetId == PacketIds.EXPLODE_PACKET) {
            onPacket(new ExplodePacket(in, new FriendlyByteBuf(out)));
        } else {
//            Packet<?> packet = ConnectionProtocol.PLAY.createPacket(PacketFlow.CLIENTBOUND, packetId);
//            if (
//                    !(packet instanceof ClientboundLightUpdatePacket) &&
//                            !(packet instanceof ClientboundSetTimePacket)
//                            && packet != null)
//               // logger.info(packet.getClass().getName());


            in0.resetReaderIndex();
            out.writeBytes(in0);
        }

    }

    private void onPacket(dev.by1337.hider.network.packet.Packet packet) {
        long l = System.nanoTime();
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
        } else if (packet instanceof LevelChunkPacket packet1) {
            packet1.writeOut(); // todo хз пакет ломается если его сначала прочитать
            level.readChunk(packet1);
        } else if (packet instanceof ForgetLevelChunkPacket packet1) {
            level.unloadChunk(new Vec2i(packet1.x(), packet1.y()));
            packet1.writeOut();
        } else if (packet instanceof SectionBlocksUpdatePacket packet1) {
            packet1.runUpdates((pos, block) -> level.setBlock(pos.getX(), pos.getY(), pos.getZ(), block));
            packet1.writeOut();
        } else if (packet instanceof BlockUpdatePacket packet1) {
            var pos = packet1.getPos();
            level.setBlock(pos.getX(), pos.getY(), pos.getZ(), packet1.getBlock());
            packet1.writeOut();
        } else if (packet instanceof ExplodePacket packet1) {
            packet1.toBlow().forEach(pos -> level.setBlock(pos.getX(), pos.getY(), pos.getZ(), VirtualWorld.AIR));
            packet1.writeOut();
        } else {
            packet.writeOut();
        }
        long time = (System.nanoTime() - l) / 1_000_000;
        if (time < 1) {
         //   logger.info("Packet {} {} ms.", packet.getClass().getSimpleName(), time);
        } else if (time < 10) {
            logger.warn("Packet {} {} ms.", packet.getClass().getSimpleName(), time);
        } else {
            logger.error("Packet {} {} ms.", packet.getClass().getSimpleName(), time);
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
