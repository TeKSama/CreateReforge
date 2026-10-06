package com.tek_sama.createreforge.registry;

import com.tek_sama.createreforge.Createreforge;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** Exposes the War Forge to Create's funnels and belts (items) and pipes (lava). */
@EventBusSubscriber(modid = Createreforge.MODID)
public class ModCapabilities {

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModBlockEntityTypes.WAR_FORGE.get(),
            (be, side) -> be.getItemHandler(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntityTypes.WAR_FORGE.get(),
            (be, side) -> be.getFluidHandler(side));
    }
}
