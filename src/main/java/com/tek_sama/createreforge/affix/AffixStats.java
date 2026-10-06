package com.tek_sama.createreforge.affix;

import com.tek_sama.createreforge.Createreforge;
import com.tek_sama.createreforge.registry.ModDataComponents;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;

/** Reading the affixes of an item. */
public final class AffixStats {

    private AffixStats() {}

    public static ForgedAffixes get(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.FORGED_AFFIXES.get(), ForgedAffixes.EMPTY);
    }

    /** The sum of the values of every affix of the given effect type on the item. */
    public static double sum(ItemStack stack, AffixEffect.Type type) {
        double total = 0;
        for (AppliedAffix applied : get(stack).affixes())
            if (applied.effect().type() == type)
                total += applied.value();
        return total;
    }

    /** Armor and elytra work from their own slot; weapons and tools only count while held in the main hand. */
    public static EquipmentSlotGroup slotGroup(ItemStack stack) {
        if (stack.getItem() instanceof Equipable equipable)
            return EquipmentSlotGroup.bySlot(equipable.getEquipmentSlot());
        return EquipmentSlotGroup.MAINHAND;
    }

    /**
     * Attribute modifier ids must be unique among everything a player wears, so they carry the
     * equipment slot group as well as the affix slot (a helmet and a chestplate may both carry a
     * "fortified" affix in their first slot).
     */
    public static ResourceLocation modifierId(EquipmentSlotGroup group, int slot) {
        return ResourceLocation.fromNamespaceAndPath(Createreforge.MODID, "affix_" + group.getSerializedName() + "_" + slot);
    }
}
