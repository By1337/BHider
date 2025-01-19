package dev.by1337.hider.network.packet;

import com.mojang.datafixers.util.Pair;
import dev.by1337.hider.util.LazyLoad;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SetEquipmentPacket implements Packet {
    private final FriendlyByteBuf in;
    private FriendlyByteBuf out;

    private final LazyLoad<Integer> packetId;
    private final LazyLoad<Integer> entityId;
    private final LazyLoad<List<Pair<EquipmentSlot, ItemStack>>> slots;


    public SetEquipmentPacket(final FriendlyByteBuf in, FriendlyByteBuf out) {
        this.in = in;
        this.out = out;
        packetId = new LazyLoad<>(in::readVarInt_, null);
        entityId = new LazyLoad<>(in::readVarInt_, packetId);
        slots = new LazyLoad<>(this::readSlots, entityId);
    }

    public SetEquipmentPacket(int packetId, int entityId, List<Pair<EquipmentSlot, ItemStack>> slots) {
        in = null;
        this.packetId = new LazyLoad<>(() -> packetId, null, true);
        this.entityId = new LazyLoad<>(() -> entityId, null, true);
        this.slots = new LazyLoad<>(() -> slots, null, true);
    }

    private List<Pair<EquipmentSlot, ItemStack>> readSlots() {
        List<Pair<EquipmentSlot, ItemStack>> result = new ArrayList<>();
        EquipmentSlot[] var1 = EquipmentSlot.values();
        byte var2;
        do {
            var2 = in.readByte();
            EquipmentSlot var3 = var1[var2 & 127];
            ItemStack var4 = in.readItem();
            result.add(Pair.of(var3, var4));
        } while ((var2 & -128) != 0);
        return Collections.unmodifiableList(result);
    }

    private void writeSlots() {
        var slots = this.slots.get();
        int var1 = slots.size();

        for (int var2 = 0; var2 < var1; ++var2) {
            Pair<EquipmentSlot, ItemStack> var3 = slots.get(var2);
            EquipmentSlot var4 = var3.getFirst();
            boolean var5 = var2 != var1 - 1;
            int var6 = var4.ordinal();
            out.writeByte(var5 ? var6 | -128 : var6);
            out.writeItem(var3.getSecond());
        }
    }

    @Override
    public FriendlyByteBuf writeOut() {
        if (!slots.isModified() && !entityId.isModified()) {
            in.resetReaderIndex();
            out.writeBytes(in);
            return out;
        }
        out.writeVarInt(packetId.get());
        out.writeVarInt(entityId.get());
        if (!slots.isModified()) {
            out.writeBytes(in);
        } else {
            writeSlots();
        }
        return out;
    }

    @Override
    public void setOut(FriendlyByteBuf out) {
        this.out = out;
    }

    @Override
    public FriendlyByteBuf getOut() {
        return out;
    }

    public int packetId() {
        return packetId.get();
    }

    public int entityId() {
        return entityId.get();
    }

    public List<Pair<EquipmentSlot, ItemStack>> slots() {
        return slots.get();
    }

    public void setPacketId(int packetId) {
        this.packetId.set(packetId);
    }

    public void setEntityId(int entityId) {
        this.entityId.set(entityId);
    }

    public void setSlots(List<Pair<EquipmentSlot, ItemStack>> slots) {
        this.slots.set(slots);
    }
}
