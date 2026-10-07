package com.tek_sama.createreadyforwar.registry;

import com.tek_sama.createreadyforwar.Createreadyforwar;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Createreadyforwar.MODID);

    public static final DeferredItem<BlockItem> WAR_FORGE = ITEMS.register("war_forge",
        () -> new BlockItem(ModBlocks.WAR_FORGE.get(), new Item.Properties()));

    public static final DeferredItem<BlockItem> WAR_DRUM = ITEMS.register("war_drum",
        () -> new BlockItem(ModBlocks.WAR_DRUM.get(), new Item.Properties()));

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
