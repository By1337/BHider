package dev.by1337.hider.network.packet;

import net.minecraft.network.FriendlyByteBuf;

public interface Packet {
    FriendlyByteBuf writeOut();
}
