package dev.by1337.hider.network.packet;

import dev.by1337.hider.util.LazyLoad;
import net.minecraft.network.FriendlyByteBuf;

public abstract class MoveEntityPacket extends Packet {
    protected final FriendlyByteBuf in;
    protected FriendlyByteBuf out;

    protected final LazyLoad<Integer> packetId;
    protected final LazyLoad<Integer> entityId;
    protected LazyLoad<Short> xa;
    protected LazyLoad<Short> ya;
    protected LazyLoad<Short> za;
    protected LazyLoad<Byte> yRot;
    protected LazyLoad<Byte> xRot;
    protected LazyLoad<Boolean> onGround;
    protected boolean modified;

    public MoveEntityPacket(final FriendlyByteBuf in, FriendlyByteBuf out) {
        this.in = in;
        this.out = out;
        packetId = new LazyLoad<>(in::readVarInt_, null);
        entityId = new LazyLoad<>(in::readVarInt_, packetId);
    }


    @Override
    protected FriendlyByteBuf getOut() {
        return out;
    }

    @Override
    public void setOut(FriendlyByteBuf out) {
        this.out = out;
    }

    public void setPacketId(final int id) {
        modified = true;
        packetId.set(id);
    }

    public void setEntityId(final int id) {
        modified = true;
        entityId.set(id);
    }

    public void setXa(final short xa) {
        modified = true;
        this.xa.set(xa);
    }

    public void setYa(final short ya) {
        modified = true;
        this.ya.set(ya);
    }

    public void setZa(final short za) {
        modified = true;
        this.za.set(za);
    }

    public void setYRot(final byte yRot) {
        modified = true;
        this.yRot.set(yRot);
    }

    public void setXRot(final byte xRot) {
        modified = true;
        this.xRot.set(xRot);
    }

    public void setOnGround(final boolean onGround) {
        modified = true;
        this.onGround.set(onGround);
    }

    public int getPacketId() {
        return packetId.get();
    }

    public int getEntityId() {
        return entityId.get();
    }

    public short getXa() {
        return xa.get();
    }

    public short getYa() {
        return ya.get();
    }

    public short getZa() {
        return za.get();
    }

    public short getYRot() {
        return yRot.get();
    }

    public short getXRot() {
        return xRot.get();
    }

    public boolean isOnGround() {
        return onGround.get();
    }

    public static class Pos extends MoveEntityPacket {

        public Pos(FriendlyByteBuf in, FriendlyByteBuf out) {
            super(in, out);
            xa = new LazyLoad<>(in::readShort, entityId);
            ya = new LazyLoad<>(in::readShort, xa);
            za = new LazyLoad<>(in::readShort, ya);
            onGround = new LazyLoad<>(in::readBoolean, za);
        }

        @Override
        protected FriendlyByteBuf writeOut() {
            if (!modified) {
                in.resetReaderIndex();
                out.writeBytes(in);
                return out;
            }
            out.writeVarInt(this.packetId.get());
            out.writeVarInt(this.entityId.get());
            out.writeShort(this.xa.get());
            out.writeShort(this.ya.get());
            out.writeShort(this.za.get());
            out.writeBoolean(this.onGround.get());
            return out;
        }
    }

    public static class Rot extends MoveEntityPacket {

        public Rot(FriendlyByteBuf in, FriendlyByteBuf out) {
            super(in, out);
            yRot = new LazyLoad<>(in::readByte, entityId);
            xRot = new LazyLoad<>(in::readByte, yRot);
            onGround = new LazyLoad<>(in::readBoolean, xRot);
        }

        @Override
        protected FriendlyByteBuf writeOut() {
            if (!modified) {
                in.resetReaderIndex();
                out.writeBytes(in);
                return out;
            }
            out.writeVarInt(this.packetId.get());
            out.writeVarInt(this.entityId.get());
            out.writeByte(this.yRot.get());
            out.writeByte(this.xRot.get());
            out.writeBoolean(this.onGround.get());
            return out;
        }
    }

    public static class PosRot extends MoveEntityPacket {

        public PosRot(FriendlyByteBuf in, FriendlyByteBuf out) {
            super(in, out);
            xa = new LazyLoad<>(in::readShort, xa);
            ya = new LazyLoad<>(in::readShort, xa);
            za = new LazyLoad<>(in::readShort, ya);
            yRot = new LazyLoad<>(in::readByte, za);
            xRot = new LazyLoad<>(in::readByte, yRot);
            onGround = new LazyLoad<>(in::readBoolean, xRot);

        }

        @Override
        protected FriendlyByteBuf writeOut() {
            if (!modified) {
                in.resetReaderIndex();
                out.writeBytes(in);
                return out;
            }
            out.writeVarInt(this.packetId.get());
            out.writeVarInt(this.entityId.get());
            out.writeShort(this.xa.get());
            out.writeShort(this.ya.get());
            out.writeShort(this.za.get());
            out.writeByte(this.yRot.get());
            out.writeByte(this.xRot.get());
            out.writeBoolean(this.onGround.get());
            return out;
        }
    }
}
