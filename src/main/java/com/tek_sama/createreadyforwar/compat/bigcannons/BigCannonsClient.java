package com.tek_sama.createreadyforwar.compat.bigcannons;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import rbasamoyai.createbigcannons.munitions.big_cannon.BigCannonProjectileRenderer;

/** Client side of the shells: flying shells are drawn like Create Big Cannons' own, bomblets as mini shells. */
final class BigCannonsClient {

    private BigCannonsClient() {}

    static void init(IEventBus modEventBus) {
        modEventBus.addListener(BigCannonsClient::registerRenderers);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        ModShells.projectiles().forEach(type -> event.registerEntityRenderer(type.get(), BigCannonProjectileRenderer::new));
        event.registerEntityRenderer(ModShells.BOMBLET.get(), ClusterBombletRenderer::new);
    }
}
