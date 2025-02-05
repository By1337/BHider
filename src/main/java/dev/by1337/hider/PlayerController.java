package dev.by1337.hider;

import dev.by1337.hider.config.Config;
import dev.by1337.hider.controller.ViewingEntity;
import dev.by1337.hider.controller.ViewingPlayer;
import dev.by1337.hider.network.PacketIds;
import dev.by1337.hider.network.packet.*;
import dev.by1337.hider.shapes.BlockShapes;
import dev.by1337.hider.world.VirtualWorld;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerController implements Closeable, Runnable {
    public final Logger logger;
    private final Plugin plugin;
    private final UUID uuid;
    private final Map<Integer, ViewingEntity> viewingEntities = new ConcurrentHashMap<>();
    public final Channel channel;
    public final ServerPlayer client;
    public final VirtualWorld level;
    public final Config config;
    public final boolean bypassHide;
    public final boolean bypassArmor;
    public long ticksLived;

    public PlayerController(Player player, Plugin plugin, UUID uuid, Channel channel, Config config, BlockShapes blockShapes) {
        this.plugin = plugin;
        this.uuid = uuid;
        level = new VirtualWorld(blockShapes);
        logger = LoggerFactory.getLogger(player.getName());
        this.channel = channel;
        this.client = ((CraftPlayer) player).getHandle();
        this.config = config;
        bypassHide = player.hasPermission(config.hideSettings.bypassPermission);
        bypassArmor = player.hasPermission(config.armorHide.bypassPermission);
    }

    @Override
    public void run() {
        tick();
    }

    private void tick() {
        ticksLived++;
        for (ViewingEntity value : viewingEntities.values()) {
            value.tick();
        }
    }

    public void onPacket(ChannelHandlerContext ctx, ByteBuf in0, ByteBuf out0) {
        FriendlyByteBuf in = new FriendlyByteBuf(in0);
        FriendlyByteBuf out = new FriendlyByteBuf(out0);
        int packetId = in.readVarInt_();
        in.resetReaderIndex();
        PacketIds.PacketCreator creator = PacketIds.getCreator(packetId);
        if (creator != null) {
            onPacket(creator.create(in), out);
        } else {
           /* var packet = ConnectionProtocol.PLAY.createPacket(PacketFlow.CLIENTBOUND, packetId);
            if (
                    !(packet instanceof ClientboundLightUpdatePacket) &&
                            !(packet instanceof ClientboundSetTimePacket)
                            && packet != null)
                //   logger.info(packet.getClass().getName());*/

            in0.resetReaderIndex();
            out0.writeBytes(in0);
        }
    }

    private void onPacket(dev.by1337.hider.network.packet.Packet packet, FriendlyByteBuf out) {
        if (packet instanceof AddPlayerPacket addPlayerPacket) {
            ViewingPlayer playerData = new ViewingPlayer(this, addPlayerPacket);
            viewingEntities.put(playerData.entityId, playerData);
        } else if (packet instanceof LevelChunkPacket packet1) {
            packet1.write(out); // todo хз пакет ломается если его сначала прочитать
            packet1.setCanceled(true); // отменяем чтобы повторно не записать пакет в байт буфер
            level.readChunk(packet1);
        } else if (packet instanceof ForgetLevelChunkPacket packet1) {
            level.unloadChunk(packet1.x(), packet1.y());
        } else if (packet instanceof SectionBlocksUpdatePacket packet1) {
            packet1.runUpdates((pos, block) -> level.setBlockState(pos.getX(), pos.getY(), pos.getZ(), block));
        } else if (packet instanceof BlockUpdatePacket packet1) {
            var pos = packet1.getPos();
            level.setBlockState(pos.getX(), pos.getY(), pos.getZ(), packet1.getBlock());
        } else if (packet instanceof ExplodePacket packet1) {
            packet1.toBlow().forEach(pos -> level.setBlockState(pos.getX(), pos.getY(), pos.getZ(), 0));
        } else if (packet instanceof RemoveEntitiesPacket packet1) {
            for (int entityId : packet1.getEntityIds()) {
                viewingEntities.remove(entityId);
            }
        } else {
            int entity = packet.getEntity();
            ViewingEntity viewingEntity = viewingEntities.get(entity);
            if (viewingEntity != null) {
                viewingEntity.onPacket(packet);
            }
        }
        packet.write(out);
    }

    @Override
    public void close() {
    }
}
