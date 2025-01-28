package dev.by1337.hider.network.packet;

import com.mojang.datafixers.util.Pair;
import dev.by1337.hider.network.PacketIds;
import dev.by1337.hider.util.LazyLoad;
import dev.by1337.hider.util.ValueHolder;
import dev.by1337.hider.util.WrappedValueHolder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SetEquipmentPacket extends Packet {
    private static final ValueHolder<Integer> PACKET_ID_HOLDER = WrappedValueHolder.of(PacketIds.SET_EQUIPMENT_PACKET);
    private final @Nullable FriendlyByteBuf in;

    private final ValueHolder<Integer> packetId;
    private final ValueHolder<Integer> entityId;
    private final ValueHolder<List<Pair<EquipmentSlot, ItemStack>>> slots;
    private boolean modified;


    public SetEquipmentPacket(final FriendlyByteBuf in) {
        this.in = in;
        packetId = new LazyLoad<>(in::readVarInt_, null);
        entityId = new LazyLoad<>(in::readVarInt_, packetId);
        slots = new LazyLoad<>(this::readSlots, entityId);
    }

    public SetEquipmentPacket(int entityId, List<Pair<EquipmentSlot, ItemStack>> slots) {
        in = null;
        this.packetId = PACKET_ID_HOLDER;
        this.entityId = WrappedValueHolder.of(entityId);
        this.slots = WrappedValueHolder.of(slots);
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

    private void writeSlots(FriendlyByteBuf out) {
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
    protected void write0(FriendlyByteBuf out) {
        if (in != null && !modified) {
            in.resetReaderIndex();
            out.writeBytes(in);
            return;
        }
        out.writeVarInt(packetId.get());
        out.writeVarInt(entityId.get());
        writeSlots(out);
    }

    @Override
    public int getEntity() {
        return entityId.get();
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

    public void setEntityId(int entityId) {
        this.entityId.set(entityId);
        modified = true;
    }

    public void setSlots(List<Pair<EquipmentSlot, ItemStack>> slots) {
        this.slots.set(slots);
        modified = true;
    }
}
