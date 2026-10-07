package com.tek_sama.createreadyforwar.compat.bigcannons;

import net.minecraft.core.Position;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Breaks nothing: a blinding flash and a deafening bang. Everything that can see the burst is
 * blinded and dizzy for a few seconds, more if it was looking straight at it.
 */
public class FlashbangShellProjectile extends ShellProjectile {

    public static final double RADIUS = 12;
    /** Ticks of Blindness / Nausea when looking right at the burst; half when it is only in view. */
    private static final int BLIND_TICKS = 120;
    private static final int NAUSEA_TICKS = 200;

    public FlashbangShellProjectile(EntityType<? extends FlashbangShellProjectile> type, Level level) {
        super(type, level);
    }

    @Override
    protected ShellKind kind() {
        return ShellKind.FLASHBANG;
    }

    @Override
    protected void detonate(Position position) {
        if (!(level() instanceof ServerLevel server))
            return;
        Vec3 burst = new Vec3(position.x(), position.y(), position.z());
        // Line-of-sight checks start a little back along the trajectory, out of the block that was hit.
        Vec3 motion = getDeltaMovement();
        Vec3 sightOrigin = motion.lengthSqr() < 1e-4 ? burst : burst.subtract(motion.normalize().scale(0.5));

        server.sendParticles(ParticleTypes.FLASH, burst.x, burst.y, burst.z, 3, 0.2, 0.2, 0.2, 0);
        server.sendParticles(ParticleTypes.FIREWORK, burst.x, burst.y, burst.z, 60, 0.3, 0.3, 0.3, 0.4);
        server.playSound(null, burst.x, burst.y, burst.z, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.BLOCKS,
            6.0f, 1.4f);
        server.playSound(null, burst.x, burst.y, burst.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS,
            4.0f, 1.8f);

        for (LivingEntity target : server.getEntitiesOfClass(LivingEntity.class, new AABB(burst, burst).inflate(RADIUS),
            e -> e.isAlive() && !e.isSpectator() && e.getEyePosition().distanceToSqr(burst) <= RADIUS * RADIUS)) {
            Vec3 eyes = target.getEyePosition();
            if (server.clip(new ClipContext(sightOrigin, eyes, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, this))
                .getType() != HitResult.Type.MISS)
                continue; // behind a wall

            // Full strength when looking towards the burst, half when it is only around.
            double facing = target.getViewVector(1f).dot(burst.subtract(eyes).normalize());
            float strength = facing > 0.5 ? 1f : 0.5f;
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, Math.round(BLIND_TICKS * strength), 0));
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, Math.round(NAUSEA_TICKS * strength), 0));
        }
    }
}
