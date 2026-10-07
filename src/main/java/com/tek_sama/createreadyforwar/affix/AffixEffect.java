package com.tek_sama.createreadyforwar.affix;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * What an affix does. Most affixes simply modify a vanilla attribute; a few (critical hits,
 * life steal) have no vanilla attribute and are applied by event handlers instead.
 */
public record AffixEffect(Type type, Optional<ResourceLocation> attribute,
    Optional<AttributeModifier.Operation> operation) {

    public enum Type implements StringRepresentable {
        ATTRIBUTE("attribute"),
        CRITICAL_CHANCE("critical_chance"),
        CRITICAL_DAMAGE("critical_damage"),
        LIFESTEAL("lifesteal"),
        /** Extra damage on a mace smash attack (a hit after falling more than 1.5 blocks). */
        SMASH_DAMAGE("smash_damage"),
        /** Extra damage on arrows, bolts and potatoes shot from the item. */
        PROJECTILE_DAMAGE("projectile_damage"),
        PROJECTILE_SPEED("projectile_speed"),
        /** Bows and crossbows draw or charge faster. */
        DRAW_SPEED("draw_speed"),
        /** Arrows and potatoes pass through that many extra enemies. */
        PIERCING("piercing"),
        /** Potatoes burst into fragments on impact, hurting everything around the target. */
        SHRAPNEL("shrapnel"),
        /** Elytra: less air drag while gliding. */
        FLIGHT_SPEED("flight_speed");

        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

        private final String name;

        Type(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public static final Codec<AffixEffect> CODEC = RecordCodecBuilder.<AffixEffect>create(instance -> instance.group(
        Type.CODEC.fieldOf("type").forGetter(AffixEffect::type),
        ResourceLocation.CODEC.optionalFieldOf("attribute").forGetter(AffixEffect::attribute),
        AttributeModifier.Operation.CODEC.optionalFieldOf("operation").forGetter(AffixEffect::operation)
    ).apply(instance, AffixEffect::new)).validate(AffixEffect::validate);

    private static DataResult<AffixEffect> validate(AffixEffect effect) {
        if (effect.type != Type.ATTRIBUTE)
            return DataResult.success(effect);
        if (effect.attribute.isEmpty() || effect.operation.isEmpty())
            return DataResult.error(() -> "An attribute effect needs both an attribute and an operation");
        if (!BuiltInRegistries.ATTRIBUTE.containsKey(effect.attribute.get()))
            return DataResult.error(() -> "Unknown attribute: " + effect.attribute.get());
        return DataResult.success(effect);
    }
}
