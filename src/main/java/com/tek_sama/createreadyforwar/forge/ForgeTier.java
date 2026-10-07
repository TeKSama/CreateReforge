package com.tek_sama.createreadyforwar.forge;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/**
 * The three forging tiers, picked by the block of material fed to the machine. Every cost grows
 * exponentially from one tier to the next (duration x3, lava x4, rotation speed x2).
 */
public enum ForgeTier {
    IRON(1, Items.IRON_BLOCK, 32, 64, 8, 1_000, 10 * 60 * 20),
    DIAMOND(2, Items.DIAMOND_BLOCK, 64, 128, 16, 4_000, 30 * 60 * 20),
    NETHERITE(3, Items.NETHERITE_BLOCK, 128, 256, 32, 16_000, 90 * 60 * 20);

    private final int level;
    private final ItemLike material;
    private final int minRpm;
    private final int maxRpm;
    private final int stressPerRpm;
    private final int lavaMillibuckets;
    private final int durationTicks;

    ForgeTier(int level, ItemLike material, int minRpm, int maxRpm, int stressPerRpm, int lavaMillibuckets,
        int durationTicks) {
        this.level = level;
        this.material = material;
        this.minRpm = minRpm;
        this.maxRpm = maxRpm;
        this.stressPerRpm = stressPerRpm;
        this.lavaMillibuckets = lavaMillibuckets;
        this.durationTicks = durationTicks;
    }

    /** 1 for iron, 2 for diamond, 3 for netherite. */
    public int level() {
        return level;
    }

    /** Lower bound of the ideal rotation speed zone. */
    public int minRpm() {
        return minRpm;
    }

    /** Upper bound of the ideal rotation speed zone. */
    public int maxRpm() {
        return maxRpm;
    }

    /** Stress units consumed per RPM while forging. */
    public int stressPerRpm() {
        return stressPerRpm;
    }

    /** Total lava consumed by one forge, drained progressively. */
    public int lavaMillibuckets() {
        return lavaMillibuckets;
    }

    /** Duration of one forge when running inside the ideal rotation zone. */
    public int durationTicks() {
        return durationTicks;
    }

    /** Colour used for this tier in tooltips. */
    public ChatFormatting color() {
        return switch (this) {
            case IRON -> ChatFormatting.GREEN;
            case DIAMOND -> ChatFormatting.AQUA;
            case NETHERITE -> ChatFormatting.LIGHT_PURPLE;
        };
    }

    public String translationKey() {
        return "createreadyforwar.tier." + name().toLowerCase();
    }

    @Nullable
    public static ForgeTier byName(String name) {
        for (ForgeTier tier : values())
            if (tier.name().equals(name))
                return tier;
        return null;
    }

    public boolean isMaterial(ItemStack stack) {
        return stack.is(material.asItem());
    }

    /** The tier a stack of material belongs to, or null if the item is not a tier block. */
    @Nullable
    public static ForgeTier fromMaterial(ItemStack stack) {
        for (ForgeTier tier : values())
            if (tier.isMaterial(stack))
                return tier;
        return null;
    }
}
