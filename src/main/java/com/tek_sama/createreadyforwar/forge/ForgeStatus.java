package com.tek_sama.createreadyforwar.forge;

import net.minecraft.ChatFormatting;

/** What the War Forge is doing right now. Synced to clients so the engineer's goggles can show it. */
public enum ForgeStatus {
    WAITING_TOOL(ChatFormatting.GRAY),
    WAITING_MATERIAL(ChatFormatting.GRAY),
    OUTPUT_BLOCKED(ChatFormatting.GOLD),
    NO_ROTATION(ChatFormatting.GOLD),
    NO_LAVA(ChatFormatting.RED),
    TOO_SLOW(ChatFormatting.GOLD),
    TOO_FAST(ChatFormatting.RED),
    FORGING(ChatFormatting.GREEN);

    private final ChatFormatting color;

    ForgeStatus(ChatFormatting color) {
        this.color = color;
    }

    public ChatFormatting color() {
        return color;
    }

    public String translationKey() {
        return "createreadyforwar.status." + name().toLowerCase();
    }

    /** True while a forge is making progress (possibly slowly or riskily). */
    public boolean isProgressing() {
        return this == FORGING || this == TOO_SLOW || this == TOO_FAST;
    }

    public static ForgeStatus byName(String name) {
        for (ForgeStatus status : values())
            if (status.name().equals(name))
                return status;
        return WAITING_TOOL;
    }
}
