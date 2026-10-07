package com.tek_sama.createreadyforwar.client;

import com.tek_sama.createreadyforwar.affix.ForgedAffixes;

import net.minecraft.world.inventory.tooltip.TooltipComponent;

/** Tooltip payload for the affix slot panel; rendered by {@link ClientAffixTooltip}. */
public record AffixTooltipData(ForgedAffixes affixes) implements TooltipComponent {}
