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

    public void write(FriendlyByteBuf out) {
        if (canceled) return;
        write0(out);
    }

    protected abstract void write0(FriendlyByteBuf out);


    public int getEntity(){
        return -1;
    }
}
