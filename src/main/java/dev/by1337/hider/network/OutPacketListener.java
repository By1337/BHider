package dev.by1337.hider.network;

import dev.by1337.hider.PlayerController;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.Closeable;
import java.util.UUID;

public class OutPacketListener extends MessageToByteEncoder<ByteBuf> implements Closeable {
    public static final String NAME = "bhider_listener";
    private final PlayerController playerController;

    public OutPacketListener(Player player, Plugin plugin) {
        final UUID uuid = player.getUniqueId();
        this.playerController = new PlayerController(
                () -> Bukkit.getPlayer(uuid),
                plugin,
                uuid
        );
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) throws Exception {
        playerController.onPacket(ctx, in, out);
    }

    @Override
    public void close() {
        playerController.close();
    }
}
