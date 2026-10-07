package com.tek_sama.createreadyforwar.client;

import java.util.List;

import com.mojang.datafixers.util.Either;
import com.tek_sama.createreadyforwar.Createreadyforwar;
import com.tek_sama.createreadyforwar.affix.AffixStats;
import com.tek_sama.createreadyforwar.affix.ForgedAffixes;

import net.minecraft.network.chat.FormattedText;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;

/** Client-side wiring: the affix panel in item tooltips. */
@EventBusSubscriber(modid = Createreadyforwar.MODID, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void registerTooltipFactories(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(AffixTooltipData.class, ClientAffixTooltip::new);
    }

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> ClientAffixTooltip.clearIconCache());
    }

    /**
     * Adds the slot panel just under the item's name, but only once at least one slot has been
     * forged: an item that never went through the War Forge keeps its plain tooltip.
     */
    @SubscribeEvent
    public static void addAffixPanel(RenderTooltipEvent.GatherComponents event) {
        ForgedAffixes forged = AffixStats.get(event.getItemStack());
        if (forged.isEmpty())
            return;
        List<Either<FormattedText, TooltipComponent>> elements = event.getTooltipElements();
        elements.add(Math.min(1, elements.size()), Either.right(new AffixTooltipData(forged)));
    }
}
