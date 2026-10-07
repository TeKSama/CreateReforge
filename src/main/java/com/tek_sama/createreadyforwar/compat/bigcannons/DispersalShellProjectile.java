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
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The "breaking the encirclement" shell. Breaks nothing: a shockwave throws everything around the
 * burst outwards and leaves it slowed and weakened, to open a way out when surrounded.
 * It hits everyone in the zone, friend or foe.
 */
public class DispersalShellProjectile extends ShellProjectile {

    public static final double RADIUS = 8;
    private static final int EFFECT_TICKS = 160;
    /** Horizontal push at the centre of the burst, fading to nothing at the edge. */
    private static final double MAX_PUSH = 2.2;

    public DispersalShellProjectile(EntityType<? extends DispersalShellProjectile> type, Level level) {
        super(type, level);
    }

    @Override
    protected ShellKind kind() {
        return ShellKind.DISPERSAL;
    }

    @Override
    protected void detonate(Position position) {
        if (!(level() instanceof ServerLevel server))
            return;
        Vec3 burst = new Vec3(position.x(), position.y(), position.z());

        server.sendParticles(ParticleTypes.EXPLOSION, burst.x, burst.y, burst.z, 1, 0, 0, 0, 0);
        for (int i = 0; i < 48; i++) {
            double angle = i * Math.PI * 2 / 48;
            server.sendParticles(ParticleTypes.CLOUD, burst.x, burst.y + 0.2, burst.z, 0, Math.cos(angle), 0.02,
                Math.sin(angle), 0.9);
        }
        server.playSound(null, burst.x, burst.y, burst.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS,
            4.0f, 0.6f);

        for (LivingEntity target : server.getEntitiesOfClass(LivingEntity.class, new AABB(burst, burst).inflate(RADIUS),
            e -> e.isAlive() && !e.isSpectator() && e.position().distanceToSqr(burst) <= RADIUS * RADIUS)) {
            Vec3 away = target.position().subtract(burst);
            double distance = away.length();
            Vec3 direction = distance < 0.1 ? new Vec3(0, 1, 0) : new Vec3(away.x, 0, away.z).normalize();
            double push = MAX_PUSH * (1 - distance / RADIUS)
                * (1 - target.getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE));
            target.push(direction.x * push, 0.4 + push * 0.25, direction.z * push);
            target.hurtMarked = true; // players only move if the server tells their client

            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, EFFECT_TICKS, 1));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, EFFECT_TICKS, 1));
        }
    }
}
