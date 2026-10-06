package com.tek_sama.createreforge;

import com.mojang.logging.LogUtils;
import com.tek_sama.createreforge.registry.ModBlockEntityTypes;
import com.tek_sama.createreforge.registry.ModBlocks;
import com.tek_sama.createreforge.registry.ModDataComponents;
import com.tek_sama.createreforge.registry.ModItems;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(Createreforge.MODID)
public class Createreforge {
    public static final String MODID = "createreforge";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Createreforge(IEventBus modEventBus) {
        ModDataComponents.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntityTypes.register(modEventBus);
    }
}
