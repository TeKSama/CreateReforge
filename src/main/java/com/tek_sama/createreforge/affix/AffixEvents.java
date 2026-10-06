package com.tek_sama.createreforge.affix;

import com.tek_sama.createreforge.Createreforge;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;

/** Puts the forged affixes to work. */
@EventBusSubscriber(modid = Createreforge.MODID)
public class AffixEvents {

    /** Attribute affixes become ordinary attribute modifiers on the item (and show up in vanilla's tooltip). */
    @SubscribeEvent
    public static void addAttributeModifiers(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        ForgedAffixes forged = AffixStats.get(stack);
        if (forged.isEmpty())
            return;

        EquipmentSlotGroup group = AffixStats.slotGroup(stack);
        for (AppliedAffix applied : forged.affixes()) {
            AffixEffect effect = applied.effect();
            if (effect.type() != AffixEffect.Type.ATTRIBUTE || effect.attribute().isEmpty() || effect.operation().isEmpty())
                continue;
            Holder<Attribute> attribute = BuiltInRegistries.ATTRIBUTE.getHolder(effect.attribute().get()).orElse(null);
            if (attribute == null)
                continue;
            event.addModifier(attribute,
                new AttributeModifier(AffixStats.modifierId(group, applied.slot()), applied.value(), effect.operation().get()),
                group);
        }
    }

    /**
     * Critical chance can turn a fully charged hit into a critical one; critical damage then adds to
     * the damage multiplier of any critical hit, vanilla or not.
     */
    @SubscribeEvent
    public static void onCriticalHit(CriticalHitEvent event) {
        Player player = event.getEntity();
        ItemStack weapon = player.getMainHandItem();
        double chance = AffixStats.sum(weapon, AffixEffect.Type.CRITICAL_CHANCE);
        double bonus = AffixStats.sum(weapon, AffixEffect.Type.CRITICAL_DAMAGE);
        if (chance <= 0 && bonus <= 0)
            return;

        // The event also fires for weak, uncharged swings: those never get a free critical hit.
        boolean fullyCharged = player.getAttackStrengthScale(0.5f) > 0.9f;
        if (!event.isCriticalHit() && fullyCharged && chance > 0 && player.getRandom().nextDouble() < chance) {
            event.setCriticalHit(true);
            event.setDamageMultiplier(1.5f);
        }
        if (event.isCriticalHit() && bonus > 0)
            event.setDamageMultiplier(event.getDamageMultiplier() + (float) bonus);
    }

    /** Life steal heals the attacker by a share of the damage a melee hit actually dealt. */
    @SubscribeEvent
    public static void onDamageDealt(LivingDamageEvent.Post event) {
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof Player player) || source.getDirectEntity() != player)
            return;
        double share = AffixStats.sum(player.getMainHandItem(), AffixEffect.Type.LIFESTEAL);
        if (share > 0 && event.getNewDamage() > 0)
            player.heal((float) (event.getNewDamage() * share));
    }
}
