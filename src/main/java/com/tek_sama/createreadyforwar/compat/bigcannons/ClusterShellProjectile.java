package com.tek_sama.createreadyforwar.compat.bigcannons;

import net.minecraft.core.Position;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import rbasamoyai.createbigcannons.CreateBigCannons;
import rbasamoyai.createbigcannons.config.CBCConfigs;
import rbasamoyai.createbigcannons.munitions.ShellExplosion;
import rbasamoyai.createbigcannons.munitions.big_cannon.config.BigCannonCommonShellProperties;

/**
 * Opens with a small burst and scatters {@value #BOMBLETS} bomblets that keep the shell's momentum,
 * spread out and each explode where they land. Best with a timed fuze, so it opens above the target.
 */
public class ClusterShellProjectile extends ShellProjectile {

    public static final int BOMBLETS = 8;
    /** Bomblets keep part of the shell's speed, capped so they rain down instead of flying off. */
    private static final double MAX_CARRIED_SPEED = 1.2;
    private static final double SPREAD = 0.35;

    public ClusterShellProjectile(EntityType<? extends ClusterShellProjectile> type, Level level) {
        super(type, level);
    }

    @Override
    protected ShellKind kind() {
        return ShellKind.CLUSTER;
    }

    @Override
    protected void detonate(Position position) {
        BigCannonCommonShellProperties properties = getAllProperties();
        Level level = level();
        // The casing bursting open: the small powers set in cluster_shell.json.
        ShellExplosion explosion = new ShellExplosion(level, this, indirectArtilleryFire(false), position.x(),
            position.y(), position.z(), properties.explosion().blockDamagePower(),
            properties.explosion().entityDamagePower(), false,
            CBCConfigs.server().munitions.damageRestriction.get().explosiveInteraction());
        CreateBigCannons.handleCustomExplosion(level, explosion);
        level.playSound(null, position.x(), position.y(), position.z(), SoundEvents.FIREWORK_ROCKET_BLAST,
            SoundSource.BLOCKS, 3.0f, 0.7f);

        Vec3 motion = getDeltaMovement();
        double speed = Math.min(motion.length() * 0.3, MAX_CARRIED_SPEED);
        Vec3 carried = motion.lengthSqr() < 1e-4 ? Vec3.ZERO : motion.normalize().scale(speed);
        RandomSource random = level.random;
        for (int i = 0; i < BOMBLETS; i++) {
            ClusterBomblet bomblet = new ClusterBomblet(ModShells.BOMBLET.get(), level);
            bomblet.setPos(position.x(), position.y(), position.z());
            double angle = (i + random.nextDouble() * 0.5) * Math.PI * 2 / BOMBLETS;
            bomblet.setDeltaMovement(carried.add(Math.cos(angle) * SPREAD * (0.6 + random.nextDouble() * 0.4),
                0.15 + random.nextDouble() * 0.1, Math.sin(angle) * SPREAD * (0.6 + random.nextDouble() * 0.4)));
            level.addFreshEntity(bomblet);
        }
    }
}
