package com.tek_sama.createreadyforwar.registry;

import com.tek_sama.createreadyforwar.Createreadyforwar;
import com.tek_sama.createreadyforwar.affix.ForgedAffixes;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {

    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Createreadyforwar.MODID);

    /** The affixes forged onto a weapon, tool or armor piece. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ForgedAffixes>> FORGED_AFFIXES =
        DATA_COMPONENTS.registerComponentType("forged_affixes",
            builder -> builder.persistent(ForgedAffixes.CODEC).networkSynchronized(ForgedAffixes.STREAM_CODEC));

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
    }
}
