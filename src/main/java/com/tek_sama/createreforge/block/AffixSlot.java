package com.tek_sama.createreforge.block;

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions;
import com.simibubi.create.foundation.gui.AllIcons;

/**
 * Which of the item's three affix slots the next forge will fill (or re-roll, if it already holds
 * an affix). Picked with the hold-right-click-and-scroll dial on top of the machine, the same widget
 * Create uses for a Brass Tunnel's distribution mode.
 */
public enum AffixSlot implements INamedIconOptions {

    SLOT_1(AllIcons.I_PATTERN_CHANCE_25),
    SLOT_2(AllIcons.I_PATTERN_CHANCE_50),
    SLOT_3(AllIcons.I_PATTERN_CHANCE_75);

    private final AllIcons icon;

    AffixSlot(AllIcons icon) {
        this.icon = icon;
    }

    /** 0 to 2, matching {@link com.tek_sama.createreforge.affix.AppliedAffix#slot()}. */
    public int index() {
        return ordinal();
    }

    /** 1 to 3, as shown to the player. */
    public int number() {
        return ordinal() + 1;
    }

    @Override
    public String getTranslationKey() {
        return "createreforge.slot." + number();
    }

    @Override
    public AllIcons getIcon() {
        return icon;
    }
}
