package dev.by1337.hider.mutator;

import com.mojang.datafixers.util.Pair;
import dev.by1337.hider.network.packet.SetEquipmentPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SetEquipmentPacketMutator {
    private static final ListTag FAKE_ENCHANTMENTS;
    public static final List<Pair<EquipmentSlot, ItemStack>> EMPTY_EQUIPMENTS;

    public static void obfuscate(SetEquipmentPacket packet) {
        var equipment = packet.slots();

        List<Pair<EquipmentSlot, ItemStack>> slots = new ArrayList<>(equipment.size());

        for (Pair<EquipmentSlot, ItemStack> pair : equipment) {
            ItemStack item = pair.getSecond().cloneItemStack();
            if (item.getTag() != null) {
                CompoundTag tag = item.getTag();
                CompoundTag newTag = new CompoundTag();
                if (tag.hasKey("Enchantments")) {
                    newTag.set("Enchantments", FAKE_ENCHANTMENTS);
                }
                if (tag.hasKey("SkullOwner")) {
                    newTag.set("SkullOwner", tag.get("SkullOwner"));
                }
                if (tag.hasKey("SkullOwnerOrig")) {
                    newTag.set("SkullOwnerOrig", tag.get("SkullOwnerOrig"));
                }
                item.setTag(newTag);
            }
            slots.add(Pair.of(pair.getFirst(), item));
        }

        packet.setSlots(slots);
    }

    static {
        FAKE_ENCHANTMENTS = new ListTag();
        CompoundTag enchantments = new CompoundTag();
        enchantments.setShort("lvl", (short) 0);
        enchantments.setString("id", "by1337:bhider");
        FAKE_ENCHANTMENTS.add(enchantments);

        EMPTY_EQUIPMENTS = new ArrayList<>();
        for (EquipmentSlot value : EquipmentSlot.values()) {
            EMPTY_EQUIPMENTS.add(Pair.of(value, ItemStack.EMPTY));
        }
    }
}
