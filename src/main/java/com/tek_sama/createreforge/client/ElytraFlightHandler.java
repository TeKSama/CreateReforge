package com.tek_sama.createreforge.client;

import com.tek_sama.createreforge.Createreforge;
import com.tek_sama.createreforge.affix.AffixEffect;
import com.tek_sama.createreforge.affix.AffixStats;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * The elytra "flight speed" affix. A player's movement is decided on their own client (the server
 * just validates it), so the boost is applied there: each tick of gliding, the speed is multiplied
 * a little, which amounts to cutting the elytra's air drag. A hard cap keeps stacked affixes from
 * pushing the player to absurd speeds.
 */
@EventBusSubscriber(modid = Createreforge.MODID, value = Dist.CLIENT)
public class ElytraFlightHandler {

    /** Vanilla drag is about 1% of the speed per tick; a flight speed of 100% would remove all of it. */
    private static final double DRAG_RELIEF_PER_POINT = 0.01;
    /** Blocks per tick (60 blocks per second). */
    private static final double MAX_SPEED = 3.0;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide || player != Minecraft.getInstance().player || !player.isFallFlying())
            return;

        double boost = AffixStats.sum(player.getItemBySlot(EquipmentSlot.CHEST), AffixEffect.Type.FLIGHT_SPEED);
        if (boost <= 0)
            return;

        Vec3 motion = player.getDeltaMovement();
        if (motion.length() >= MAX_SPEED)
            return;
        player.setDeltaMovement(motion.scale(1 + boost * DRAG_RELIEF_PER_POINT));
    }
}
