package dev.by1337.hider;

import com.mojang.datafixers.util.Pair;
import dev.by1337.hider.config.Config;
import dev.by1337.hider.engine.RayTraceToPlayerEngine;
import dev.by1337.hider.mutator.SetEquipmentPacketMutator;
import dev.by1337.hider.network.PacketIds;
import dev.by1337.hider.network.packet.*;
import dev.by1337.hider.shapes.BlockBox;
import dev.by1337.hider.util.BoolWatcher;
import dev.by1337.hider.world.VirtualWorld;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.by1337.blib.geom.Vec3d;
import org.by1337.blib.geom.Vec3i;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class PlayerController implements Closeable {
    private final Logger logger;
    private final Plugin plugin;
    private final UUID uuid;
    private final Map<Integer, PlayerData> viewingPlayers = new ConcurrentHashMap<>();
    private final BukkitTask task;
    public final Channel channel;
    public final ServerPlayer client;
    public final VirtualWorld level = new VirtualWorld();
    public final Config config;

    public PlayerController(Player player, Plugin plugin, UUID uuid, Channel channel, Config config) {
        this.plugin = plugin;
        this.uuid = uuid;
        logger = LoggerFactory.getLogger(player.getName());
        //  task = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::tick, 1, 1);
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1, 1); // todo make async
        this.channel = channel;
        this.client = ((CraftPlayer) player).getHandle();
        this.config = config;
    }

    private void tick() {
        for (PlayerData value : viewingPlayers.values()) {
            value.tick();
        }
        if (client.isSneaking() && false) {
            Chunk chunk = client.getBukkitEntity().getLocation().getChunk();
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
        } else if (packetId == PacketIds.REMOVE_ENTITIES_PACKET) {
            onPacket(new RemoveEntitiesPacket(in, new FriendlyByteBuf(out)));
        } else {
            var packet = ConnectionProtocol.PLAY.createPacket(PacketFlow.CLIENTBOUND, packetId);
            if (
                    !(packet instanceof ClientboundLightUpdatePacket) &&
                            !(packet instanceof ClientboundSetTimePacket)
                            && packet != null)
                //logger.info(packet.getClass().getName());


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
            packet1.write(); // todo хз пакет ломается если его сначала прочитать
            packet1.setCanceled(true); // отменяем чтобы повторно не записать пакет в байт буфер
            level.readChunk(packet1);
        } else if (packet instanceof ForgetLevelChunkPacket packet1) {
            level.unloadChunk(packet1.x(), packet1.y());
        } else if (packet instanceof SectionBlocksUpdatePacket packet1) {
            packet1.runUpdates((pos, block) -> level.setBlock(pos.getX(), pos.getY(), pos.getZ(), block));
        } else if (packet instanceof BlockUpdatePacket packet1) {
            var pos = packet1.getPos();
            level.setBlock(pos.getX(), pos.getY(), pos.getZ(), packet1.getBlock());
        } else if (packet instanceof ExplodePacket packet1) {
            packet1.toBlow().forEach(pos -> level.setBlock(pos.getX(), pos.getY(), pos.getZ(), 0));
        } else if (packet instanceof RemoveEntitiesPacket packet1) {
            for (int entityId : packet1.getEntityIds()) {
                viewingPlayers.remove(entityId);
            }
        }
        packet.write();
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
        public final int entityId;
        public final UUID uuid;
        public double x;
        public double y;
        public double z;
        private final Map<EquipmentSlot, ItemStack> equipment = new HashMap<>();
        private final BoolWatcher hideArmor = new BoolWatcher(false);
        public final ServerPlayer player;
        private boolean suppressArmorUpdate;
        private final RayTraceToPlayerEngine rayTraceEngine;

        public PlayerData(AddPlayerPacket packet) {
            entityId = packet.entityId();
            uuid = packet.playerId();
            x = packet.x();
            y = packet.y();
            z = packet.z();
            player = ((CraftPlayer) Bukkit.getPlayer(uuid)).getHandle();
            rayTraceEngine = new RayTraceToPlayerEngine(PlayerController.this, this);
        }

        public void tick() {
            hideArmorTick();
        }

        private void hideArmorTick() {
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
            if (directionToTarget.angle(clientEye.getDirection()) >= config.armorHide.fieldOfView) return false;

            return rayTraceEngine.noneMatch();
        }

        public void onMove(MoveEntityPacket packet) {
            x = player.lastX;
            y = player.lastY;
            z = player.lastZ;
        }


        public void setEntityDataPacket(SetEntityDataPacket packet) {
            //  isShift = me.isSneaking();
            //  isGlowing = me.isGlowing();
        }

        public void setEquipmentPacket(SetEquipmentPacket packet) {
            var list = packet.slots();
            for (Pair<EquipmentSlot, ItemStack> pair : list) {
                equipment.put(pair.getFirst(), pair.getSecond());
            }
            equipment.entrySet().removeIf(e -> e.getValue().isEmpty());
            if (suppressArmorUpdate) {
                packet.setCanceled(true);
                return;
            }
            if (config.armorHide.hideMeta)
                SetEquipmentPacketMutator.obfuscate(packet);
        }

        public void hideArmor() {
            System.out.println("PlayerData.hideArmor");
            suppressArmorUpdate = true;
            SetEquipmentPacket equipmentPacket = new SetEquipmentPacket(
                    PacketIds.SET_EQUIPMENT_PACKET,
                    entityId,
                    SetEquipmentPacketMutator.EMPTY_EQUIPMENTS
            );
            channel.writeAndFlush(equipmentPacket);
        }

        public void unhideArmor() {
            System.out.println("PlayerData.unhideArmor");
            suppressArmorUpdate = false;
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
                    ", x=" + x +
                    ", y=" + y +
                    ", z=" + z +
                    '}';
        }
    }
}
