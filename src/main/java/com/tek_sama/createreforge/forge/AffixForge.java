package com.tek_sama.createreforge.forge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tek_sama.createreforge.affix.Affix;
import com.tek_sama.createreforge.affix.AffixStats;
import com.tek_sama.createreforge.affix.AppliedAffix;
import com.tek_sama.createreforge.affix.ForgedAffixes;
import com.tek_sama.createreforge.registry.ModDataComponents;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/** Rolls and applies affixes: at the end of a successful forge, or on demand through {@code /reforge}. */
public final class AffixForge {

    private AffixForge() {}

    /**
     * Rolls a random affix valid for the item and writes it into the given slot, replacing whatever
     * was there. Slots are independent: nothing stops three affixes of the same kind or tier.
     * The item comes back unchanged when no affix fits it.
     */
    public static ItemStack forge(ItemStack stack, ForgeTier tier, int slot, RegistryAccess registries,
        RandomSource random) {
        Registry<Affix> registry = registries.registryOrThrow(Affix.REGISTRY_KEY);

        List<Map.Entry<ResourceKey<Affix>, Affix>> candidates = new ArrayList<>();
        for (Map.Entry<ResourceKey<Affix>, Affix> entry : registry.entrySet())
            if (fits(stack, entry.getValue()))
                candidates.add(entry);
        if (candidates.isEmpty())
            return stack;

        Map.Entry<ResourceKey<Affix>, Affix> picked = candidates.get(random.nextInt(candidates.size()));
        apply(stack, picked.getKey().location(), picked.getValue(), tier, slot, random);
        return stack;
    }

    /** Whether the affix can roll on this item (they share at least one category). */
    public static boolean fits(ItemStack stack, Affix affix) {
        Set<ReforgeCategory> categories = ReforgeCategory.of(stack);
        return affix.categories().stream().anyMatch(categories::contains);
    }

    /** Writes a freshly rolled value of the given affix and tier into the slot, replacing its content. */
    public static void apply(ItemStack stack, ResourceLocation id, Affix affix, ForgeTier tier, int slot,
        RandomSource random) {
        AppliedAffix applied = new AppliedAffix(slot, id, tier.level(), affix.range(tier).roll(random),
            affix.effect(), affix.percent());
        stack.set(ModDataComponents.FORGED_AFFIXES.get(), AffixStats.get(stack).with(applied));
    }

    /** The first slot without an affix, or -1 when all of them are taken. */
    public static int firstEmptySlot(ItemStack stack) {
        ForgedAffixes forged = AffixStats.get(stack);
        for (int slot = 0; slot < ForgedAffixes.SLOTS; slot++)
            if (forged.get(slot) == null)
                return slot;
        return -1;
    }
}
