package com.tek_sama.createreadyforwar;

import com.mojang.logging.LogUtils;
import com.tek_sama.createreadyforwar.registry.ModBlockEntityTypes;
import com.tek_sama.createreadyforwar.registry.ModBlocks;
import com.tek_sama.createreadyforwar.registry.ModDataComponents;
import com.tek_sama.createreadyforwar.registry.ModItems;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(Createreadyforwar.MODID)
public class Createreadyforwar {
    public static final String MODID = "createreadyforwar";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Createreadyforwar(IEventBus modEventBus) {
        ModDataComponents.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntityTypes.register(modEventBus);
    }
}
