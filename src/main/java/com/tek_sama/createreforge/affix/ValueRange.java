package com.tek_sama.createreforge.affix;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.RandomSource;

/** The inclusive range a tier can roll an affix value in. */
public record ValueRange(double min, double max) {

    public static final Codec<ValueRange> CODEC = RecordCodecBuilder.<ValueRange>create(instance -> instance.group(
        Codec.DOUBLE.fieldOf("min").forGetter(ValueRange::min),
        Codec.DOUBLE.fieldOf("max").forGetter(ValueRange::max)
    ).apply(instance, ValueRange::new)).validate(range -> range.min <= range.max
        ? DataResult.success(range)
        : DataResult.error(() -> "min must not be greater than max"));

    /** A random value in the range, rounded to 3 decimals so stored values stay readable. */
    public double roll(RandomSource random) {
        double value = min + random.nextDouble() * (max - min);
        return Math.round(value * 1000.0) / 1000.0;
    }
}
