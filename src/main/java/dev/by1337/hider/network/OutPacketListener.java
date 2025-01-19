package dev.by1337.hider.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class OutPacketListener extends MessageToByteEncoder<ByteBuf> {
    public static final String NAME = "bhider_listener";

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) throws Exception {
        System.out.println("out " + in.readableBytes() + " bytes");
        out.writeBytes(in);
    }
}
