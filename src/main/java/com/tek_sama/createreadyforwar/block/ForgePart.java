package com.tek_sama.createreadyforwar.block;

import net.minecraft.util.StringRepresentable;

/**
 * The two halves of the War Forge. Seen from the front of the machine, LEFT is the half that was
 * clicked at placement time (item inputs and output) and RIGHT is the one to its right (lava and
 * rotation).
 */
public enum ForgePart implements StringRepresentable {
    LEFT("left"),
    RIGHT("right");

    private final String name;

    ForgePart(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
