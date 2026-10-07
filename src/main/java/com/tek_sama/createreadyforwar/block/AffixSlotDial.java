package com.tek_sama.createreadyforwar.block;

import com.simibubi.create.foundation.blockEntity.behaviour.CenteredSideValueBoxTransform;

import net.minecraft.core.Direction;

/** Positions the affix slot dial on the top face, like Create does for a Brass Tunnel's mode dial. */
public class AffixSlotDial extends CenteredSideValueBoxTransform {

    public AffixSlotDial() {
        super((state, direction) -> direction == Direction.UP);
    }

    @Override
    public int getOverrideColor() {
        return 0xB08D57;
    }
}
