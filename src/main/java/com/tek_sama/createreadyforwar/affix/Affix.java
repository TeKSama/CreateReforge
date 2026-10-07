package com.tek_sama.createreadyforwar.affix;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tek_sama.createreadyforwar.Createreadyforwar;
import com.tek_sama.createreadyforwar.forge.ForgeTier;
import com.tek_sama.createreadyforwar.forge.ReforgeCategory;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

/**
 * An affix definition, loaded from {@code data/<namespace>/createreadyforwar/affix/<id>.json}.
 *
 * <pre>
 * {
 *   "categories": ["weapon"],
 *   "effect": { "type": "attribute", "attribute": "minecraft:generic.attack_damage", "operation": "add_value" },
 *   "tiers": [ {"min": 1, "max": 2}, {"min": 3, "max": 5}, {"min": 8, "max": 12} ],
 *   "percent": false
 * }
 * </pre>
 *
 * Its name and description come from the lang keys {@code affix.<namespace>.<path>} and
 * {@code affix.<namespace>.<path>.desc}; its icon is {@code textures/affix/<path>.png} in the same
 * namespace (a placeholder is shown when the texture does not exist).
 */
public record Affix(List<ReforgeCategory> categories, AffixEffect effect, List<ValueRange> tiers, boolean percent) {

    public static final ResourceKey<Registry<Affix>> REGISTRY_KEY =
        ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(Createreadyforwar.MODID, "affix"));

    public static final Codec<Affix> CODEC = RecordCodecBuilder.<Affix>create(instance -> instance.group(
        ReforgeCategory.CODEC.listOf().fieldOf("categories").forGetter(Affix::categories),
        AffixEffect.CODEC.fieldOf("effect").forGetter(Affix::effect),
        ValueRange.CODEC.listOf().fieldOf("tiers").forGetter(Affix::tiers),
        Codec.BOOL.optionalFieldOf("percent", false).forGetter(Affix::percent)
    ).apply(instance, Affix::new)).validate(affix -> {
        if (affix.categories.isEmpty())
            return DataResult.error(() -> "An affix needs at least one category");
        if (affix.tiers.size() != ForgeTier.values().length)
            return DataResult.error(() -> "An affix needs exactly " + ForgeTier.values().length + " tiers");
        return DataResult.success(affix);
    });

    public ValueRange range(ForgeTier tier) {
        return tiers.get(tier.level() - 1);
    }

    public static String nameKey(ResourceLocation id) {
        return "affix." + id.getNamespace() + "." + id.getPath();
    }

    public static String descriptionKey(ResourceLocation id) {
        return nameKey(id) + ".desc";
    }
}
