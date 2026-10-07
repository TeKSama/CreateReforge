package com.tek_sama.createreadyforwar.affix;

import com.tek_sama.createreadyforwar.Createreadyforwar;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/**
 * Declares affixes as a datapack registry: every JSON file in
 * {@code data/<namespace>/createreadyforwar/affix/} becomes an affix, and {@code /reload} picks up
 * changes. Only the server needs them (items carry a snapshot of their own affixes), so the
 * registry is not synced to clients.
 */
@EventBusSubscriber(modid = Createreadyforwar.MODID)
public class AffixRegistry {

    @SubscribeEvent
    public static void registerDatapackRegistry(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(Affix.REGISTRY_KEY, Affix.CODEC);
    }
}
