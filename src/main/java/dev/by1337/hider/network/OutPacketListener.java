package dev.by1337.hider.network;

import dev.by1337.hider.PlayerController;
import dev.by1337.hider.config.Config;
import dev.by1337.hider.shapes.BlockShapes;
import dev.by1337.hider.ticker.Ticker;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import net.minecraft.network.CompressionEncoder;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.Closeable;

public class OutPacketListener extends MessageToByteEncoder<ByteBuf> implements Closeable {
    public static final String NAME = "bhider_listener";
    private final PlayerController playerController;
    private final Ticker ticker;

    public OutPacketListener(Player player, Plugin plugin, Channel channel, Config config, BlockShapes blockShapes, Ticker ticker) {
        this.ticker = ticker;
        this.playerController = new PlayerController(
                player,
                plugin,
                player.getUniqueId(),
                channel,
                config,
                blockShapes
        );
        ticker.addTask(playerController);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) throws Exception {
        playerController.onPacket(ctx, in, out);
    }

    @Override
    public void close() {
        ticker.removeTask(playerController);
        playerController.close();
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        super.handlerRemoved(ctx);
        close();
    }
}
