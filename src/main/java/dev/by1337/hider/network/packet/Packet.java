package dev.by1337.hider.network.packet;

import net.minecraft.network.FriendlyByteBuf;

public abstract class Packet {
    private boolean canceled;

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    public void write() {
        if (canceled) return;
        writeOut();
    }

    protected abstract FriendlyByteBuf writeOut();

    protected abstract FriendlyByteBuf getOut();

    public abstract void setOut(FriendlyByteBuf out);
}
