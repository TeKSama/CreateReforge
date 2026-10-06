package com.tek_sama.createreforge.forge;

import java.util.EnumSet;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.simibubi.create.content.equipment.potatoCannon.PotatoCannonItem;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ElytraItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;

/**
 * The kinds of items the War Forge accepts. An item can belong to several categories (an axe is
 * both a weapon and a tool, a mace is a weapon and a mace), and affixes declare which categories
 * they can roll on.
 *
 * <ul>
 * <li>{@link #WEAPON}: melee weapons (swords, axes, tridents, maces)</li>
 * <li>{@link #MACE}, {@link #BOW}, {@link #CROSSBOW}, {@link #POTATO_CANNON}, {@link #ELYTRA}:
 * one item each, for the affixes that only make sense on it</li>
 * </ul>
 */
public enum ReforgeCategory implements StringRepresentable {
    WEAPON("weapon"),
    TOOL("tool"),
    ARMOR("armor"),
    MACE("mace"),
    BOW("bow"),
    CROSSBOW("crossbow"),
    POTATO_CANNON("potato_cannon"),
    ELYTRA("elytra");

    public static final Codec<ReforgeCategory> CODEC = StringRepresentable.fromEnum(ReforgeCategory::values);

    private final String name;

    ReforgeCategory(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /** All the categories an item belongs to; empty when the item cannot be reforged. */
    public static Set<ReforgeCategory> of(ItemStack stack) {
        Set<ReforgeCategory> categories = EnumSet.noneOf(ReforgeCategory.class);
        Item item = stack.getItem();
        if (item instanceof SwordItem || item instanceof AxeItem || item instanceof TridentItem
            || item instanceof MaceItem)
            categories.add(WEAPON);
        if (item instanceof DiggerItem)
            categories.add(TOOL);
        if (item instanceof ArmorItem)
            categories.add(ARMOR);
        if (item instanceof MaceItem)
            categories.add(MACE);
        if (item instanceof BowItem)
            categories.add(BOW);
        if (item instanceof CrossbowItem)
            categories.add(CROSSBOW);
        if (item instanceof PotatoCannonItem)
            categories.add(POTATO_CANNON);
        if (item instanceof ElytraItem)
            categories.add(ELYTRA);
        return categories;
    }

    public static boolean isReforgeable(ItemStack stack) {
        return !stack.isEmpty() && !of(stack).isEmpty();
    }
}
