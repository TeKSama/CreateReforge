package com.tek_sama.createreadyforwar;

import com.mojang.logging.LogUtils;
import com.tek_sama.createreadyforwar.compat.bigcannons.BigCannonsCompat;
import com.tek_sama.createreadyforwar.registry.ModBlockEntityTypes;
import com.tek_sama.createreadyforwar.registry.ModBlocks;
import com.tek_sama.createreadyforwar.registry.ModDataComponents;
import com.tek_sama.createreadyforwar.registry.ModItems;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
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

        // Optional: our shells exist only when Create Big Cannons is installed. The check keeps
        // every class of the compat package from loading otherwise.
        if (ModList.get().isLoaded(BigCannonsCompat.MOD_ID))
            BigCannonsCompat.init(modEventBus);
    }
}
