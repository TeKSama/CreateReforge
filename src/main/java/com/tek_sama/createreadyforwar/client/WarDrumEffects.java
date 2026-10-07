package com.tek_sama.createreadyforwar.client;

import com.tek_sama.createreadyforwar.Createreadyforwar;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * What a War Drum beat looks like: a ring of air bursting out of the hide, dust jumping off it and,
 * for players within reach, a short shudder of the camera as if the air itself was vibrating.
 * The shudder follows the game's "Distortion Effects" accessibility slider.
 */
@EventBusSubscriber(modid = Createreadyforwar.MODID, value = Dist.CLIENT)
public class WarDrumEffects {

    private static final int RING_PARTICLES = 36;
    private static final int SHAKE_TICKS = 8;
    /** Strongest shudder, in degrees, at top speed. */
    private static final float MAX_SHAKE_DEGREES = 1.2f;

    private static int shakeTicks;
    private static float shakeStrength;

    private WarDrumEffects() {}

    public static void onBeat(Level level, BlockPos pos, int radius, boolean maxed) {
        RandomSource random = level.random;
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 1.05;
        double z = pos.getZ() + 0.5;

        // The ring flies further on a stronger beat (clouds slow down and fade after ~1 s).
        double ringSpeed = 0.25 + 0.35 * Math.min(1.0, radius / 40.0);
        double offset = random.nextDouble() * Math.PI * 2;
        for (int i = 0; i < RING_PARTICLES; i++) {
            double angle = offset + i * Math.PI * 2 / RING_PARTICLES;
            double dx = Math.cos(angle);
            double dz = Math.sin(angle);
            level.addParticle(ParticleTypes.CLOUD, x + dx * 0.5, y, z + dz * 0.5, dx * ringSpeed, 0.01,
                dz * ringSpeed);
        }

        // Dust jumping off the hide.
        for (int i = 0; i < 6; i++)
            level.addParticle(ParticleTypes.WHITE_ASH, x - 0.35 + random.nextDouble() * 0.7, y,
                z - 0.35 + random.nextDouble() * 0.7, 0, 0.15, 0);

        // At top speed a second, faster ring follows the first one, a little higher.
        if (maxed)
            for (int i = 0; i < RING_PARTICLES; i++) {
                double angle = offset + (i + 0.5) * Math.PI * 2 / RING_PARTICLES;
                level.addParticle(ParticleTypes.POOF, x, y + 0.3, z, Math.cos(angle) * ringSpeed * 1.6, 0.02,
                    Math.sin(angle) * ringSpeed * 1.6);
            }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.level() == level
            && player.distanceToSqr(Vec3.atCenterOf(pos)) <= (double) radius * radius) {
            shakeTicks = SHAKE_TICKS;
            shakeStrength = MAX_SHAKE_DEGREES * Math.min(1f, radius / 40f + 0.25f);
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (shakeTicks > 0)
            shakeTicks--;
    }

    @SubscribeEvent
    public static void shakeCamera(ViewportEvent.ComputeCameraAngles event) {
        if (shakeTicks <= 0)
            return;
        float distortion = Minecraft.getInstance().options.screenEffectScale().get().floatValue();
        if (distortion <= 0)
            return;
        float time = shakeTicks - (float) event.getPartialTick();
        float fade = Mth.clamp(time / SHAKE_TICKS, 0f, 1f);
        float wobble = Mth.sin(time * 2.6f) * shakeStrength * fade * distortion;
        event.setRoll(event.getRoll() + wobble);
        event.setPitch(event.getPitch() + wobble * 0.4f);
    }
}
