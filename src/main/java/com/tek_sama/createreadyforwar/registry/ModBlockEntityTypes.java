package com.tek_sama.createreadyforwar.registry;

import com.tek_sama.createreadyforwar.Createreadyforwar;
import com.tek_sama.createreadyforwar.block.WarDrumBlockEntity;
import com.tek_sama.createreadyforwar.block.WarForgeBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntityTypes {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Createreadyforwar.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WarForgeBlockEntity>> WAR_FORGE =
        BLOCK_ENTITY_TYPES.register("war_forge", () -> BlockEntityType.Builder
            .of(WarForgeBlockEntity::new, ModBlocks.WAR_FORGE.get())
            .build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WarDrumBlockEntity>> WAR_DRUM =
        BLOCK_ENTITY_TYPES.register("war_drum", () -> BlockEntityType.Builder
            .of(WarDrumBlockEntity::new, ModBlocks.WAR_DRUM.get())
            .build(null));

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}
