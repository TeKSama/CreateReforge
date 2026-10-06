package com.tek_sama.createreforge.registry;

import com.simibubi.create.AllCreativeModeTabs;
import com.tek_sama.createreforge.Createreforge;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/** Injects the War Forge into Create's own "Base" creative tab instead of adding a new tab. */
@EventBusSubscriber(modid = Createreforge.MODID)
public class ModCreativeTab {

    @SubscribeEvent
    public static void addToCreateTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == AllCreativeModeTabs.BASE_CREATIVE_TAB.getKey())
            event.accept(ModItems.WAR_FORGE);
    }
}
