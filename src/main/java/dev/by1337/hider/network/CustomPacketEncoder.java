package dev.by1337.hider.network;

import dev.by1337.hider.network.packet.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import net.minecraft.network.FriendlyByteBuf;

public class CustomPacketEncoder extends MessageToByteEncoder<Packet> {
    public static final String NAME = "bhider_encoder";

    @Override
    protected void encode(ChannelHandlerContext ctx, Packet packet, ByteBuf buf) throws Exception {
        packet.write(new FriendlyByteBuf(buf));
    }
}
