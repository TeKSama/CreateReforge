package com.tek_sama.createreforge.registry;

import com.tek_sama.createreforge.Createreforge;
import com.tek_sama.createreforge.block.WarForgeBlock;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Createreforge.MODID);

    public static final DeferredBlock<WarForgeBlock> WAR_FORGE = BLOCKS.register("war_forge",
        () -> new WarForgeBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .requiresCorrectToolForDrops()
            .strength(5.0f, 6.0f)
            .sound(SoundType.NETHERITE_BLOCK)
            .noOcclusion()
            // the lava inside makes the machine glow
            .lightLevel(state -> state.getValue(WarForgeBlock.LAVA) > 0 ? 9 : 0)));

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
