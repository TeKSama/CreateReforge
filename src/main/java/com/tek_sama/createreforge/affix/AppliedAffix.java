package com.tek_sama.createreforge.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.ResourceLocation;

/**
 * An affix as stored on an item. It keeps a snapshot of the effect, so the item keeps working (and
 * its tooltip stays readable, even client-side) whatever happens to the datapack afterwards.
 *
 * @param slot    the affix slot it occupies, 0 to 2
 * @param affix   the id of the affix definition (also the base of its lang keys and icon)
 * @param tier    1 (iron), 2 (diamond) or 3 (netherite)
 * @param value   the rolled value
 * @param effect  what it does
 * @param percent whether the value is displayed as a percentage
 */
public record AppliedAffix(int slot, ResourceLocation affix, int tier, double value, AffixEffect effect,
    boolean percent) {

    public static final Codec<AppliedAffix> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.intRange(0, ForgedAffixes.SLOTS - 1).fieldOf("slot").forGetter(AppliedAffix::slot),
        ResourceLocation.CODEC.fieldOf("affix").forGetter(AppliedAffix::affix),
        Codec.intRange(1, 3).fieldOf("tier").forGetter(AppliedAffix::tier),
        Codec.DOUBLE.fieldOf("value").forGetter(AppliedAffix::value),
        AffixEffect.CODEC.fieldOf("effect").forGetter(AppliedAffix::effect),
        Codec.BOOL.optionalFieldOf("percent", false).forGetter(AppliedAffix::percent)
    ).apply(instance, AppliedAffix::new));
}
