package com.tek_sama.createreadyforwar.compat.bigcannons;

import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import rbasamoyai.createbigcannons.ModGroup;
import rbasamoyai.createbigcannons.index.CBCBlockEntities;
import rbasamoyai.createbigcannons.index.CBCMunitionPropertiesHandlers;
import rbasamoyai.createbigcannons.munitions.config.MunitionPropertiesHandler;

/**
 * Entry point of the optional Create Big Cannons integration. Must only be called once Create Big
 * Cannons is known to be loaded.
 */
public final class BigCannonsCompat {

    public static final String MOD_ID = "createbigcannons";

    private BigCannonsCompat() {}

    public static void init(IEventBus modEventBus) {
        ModShells.register(modEventBus);
        modEventBus.addListener(BigCannonsCompat::useCannonsBlockEntity);
        modEventBus.addListener(BigCannonsCompat::commonSetup);
        modEventBus.addListener(BigCannonsCompat::addToCreativeTab);
        if (FMLEnvironment.dist == Dist.CLIENT)
            BigCannonsClient.init(modEventBus);
    }

    /**
     * Our shells use Create Big Cannons' own fuzed-shell block entity, so its fuze handling,
     * ticking and rendering all work unchanged.
     */
    private static void useCannonsBlockEntity(BlockEntityTypeAddBlocksEvent event) {
        Block[] blocks = new Block[ShellKind.values().length];
        for (ShellKind kind : ShellKind.values())
            blocks[kind.ordinal()] = ModShells.block(kind);
        event.modify(CBCBlockEntities.FUZED_BLOCK.get(), blocks);
    }

    /** Lets Create Big Cannons read our shells' JSON properties, like it does for its HE shell. */
    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> ModShells.projectiles().forEach(type -> MunitionPropertiesHandler
            .registerProjectileHandler(type.get(), CBCMunitionPropertiesHandlers.COMMON_SHELL_BIG_CANNON_PROJECTILE)));
    }

    /** Next to Create Big Cannons' own munitions. */
    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == ModGroup.MAIN_TAB_KEY)
            for (ShellKind kind : ShellKind.values())
                event.accept(ModShells.item(kind));
    }
}
