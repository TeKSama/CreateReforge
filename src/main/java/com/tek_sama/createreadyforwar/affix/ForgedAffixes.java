package com.tek_sama.createreadyforwar.affix;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** The data component holding the affixes forged onto an item (at most one per slot). */
public record ForgedAffixes(List<AppliedAffix> affixes) {

    public static final int SLOTS = 3;
    public static final ForgedAffixes EMPTY = new ForgedAffixes(List.of());

    public static final Codec<ForgedAffixes> CODEC =
        AppliedAffix.CODEC.listOf().xmap(ForgedAffixes::new, ForgedAffixes::affixes);
    public static final StreamCodec<ByteBuf, ForgedAffixes> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    public ForgedAffixes {
        affixes = List.copyOf(affixes);
    }

    /** The affix in the given slot, or null when the slot is still empty. */
    @Nullable
    public AppliedAffix get(int slot) {
        for (AppliedAffix applied : affixes)
            if (applied.slot() == slot)
                return applied;
        return null;
    }

    /** A copy with the given affix in its slot, replacing whatever was there. */
    public ForgedAffixes with(AppliedAffix applied) {
        List<AppliedAffix> updated = new ArrayList<>();
        for (AppliedAffix existing : affixes)
            if (existing.slot() != applied.slot())
                updated.add(existing);
        updated.add(applied);
        updated.sort(Comparator.comparingInt(AppliedAffix::slot));
        return new ForgedAffixes(updated);
    }

    public boolean isEmpty() {
        return affixes.isEmpty();
    }
}
