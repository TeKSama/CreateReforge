package com.tek_sama.createreadyforwar.compat.bigcannons;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import rbasamoyai.createbigcannons.CreateBigCannons;
import rbasamoyai.createbigcannons.config.CBCConfigs;
import rbasamoyai.createbigcannons.munitions.ShellExplosion;

/**
 * One of the small bombs scattered by a cluster shell: falls, and explodes on whatever it hits.
 * Drawn as a miniature cluster shell ({@link ClusterBombletRenderer}); the fire charge item below is
 * only what vanilla shows in the rare places it draws a thrown item's particles.
 * Uses Create Big Cannons' shell explosion so its block-damage config applies.
 */
public class ClusterBomblet extends ThrowableItemProjectile {

    private static final float BLOCK_POWER = 2.0f;
    private static final float ENTITY_POWER = 3.0f;
    /** Safety: a bomblet still flying after this many ticks explodes anyway. */
    private static final int MAX_LIFETIME = 200;

    public ClusterBomblet(EntityType<? extends ClusterBomblet> type, Level level) {
        super(type, level);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.FIRE_CHARGE;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount > MAX_LIFETIME)
            explode();
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide)
            explode();
    }

    private void explode() {
        if (isRemoved())
            return;
        ShellExplosion explosion = new ShellExplosion(level(), this, null, getX(), getY(), getZ(), BLOCK_POWER,
            ENTITY_POWER, false, CBCConfigs.server().munitions.damageRestriction.get().explosiveInteraction());
        CreateBigCannons.handleCustomExplosion(level(), explosion);
        discard();
    }
}
