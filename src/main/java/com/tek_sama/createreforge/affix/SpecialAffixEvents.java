package com.tek_sama.createreforge.affix;

import javax.annotation.Nullable;

import com.simibubi.create.content.equipment.potatoCannon.PotatoCannonItem;
import com.simibubi.create.content.equipment.potatoCannon.PotatoProjectileEntity;
import com.tek_sama.createreforge.Createreforge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Affixes that belong to one kind of item: the mace's smash, the bow and crossbow's projectiles and
 * draw speed, and the Create potato cannon's speed, piercing and shrapnel.
 *
 * Arrows remember the weapon they were fired from ({@link AbstractArrow#getWeaponItem()}). Potatoes
 * have no such memory, so they take a snapshot of their cannon's affixes on their first tick and
 * keep it in their persistent data: swapping items mid-flight changes nothing.
 */
@EventBusSubscriber(modid = Createreforge.MODID)
public class SpecialAffixEvents {

    private static final String KEY_INIT = "ReforgeInit";
    private static final String KEY_DAMAGE = "ReforgeDamage";
    private static final String KEY_PIERCE = "ReforgePierce";
    private static final String KEY_SHRAPNEL = "ReforgeShrapnel";
    private static final String KEY_IGNORE = "ReforgeIgnore";

    private static final double SHRAPNEL_RADIUS = 2.5;

    private static final ResourceLocation POTATO_PROJECTILE = ResourceLocation.fromNamespaceAndPath("create", "potato_projectile");

    // ---- damage bonuses ----------------------------------------------------------------------

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        Entity direct = source.getDirectEntity();
        double bonus = 0;

        if (direct instanceof AbstractArrow arrow) {
            ItemStack weapon = arrow.getWeaponItem();
            if (weapon != null)
                bonus = AffixStats.sum(weapon, AffixEffect.Type.PROJECTILE_DAMAGE);
        } else if (direct instanceof PotatoProjectileEntity potato) {
            bonus = potato.getPersistentData().getFloat(KEY_DAMAGE);
        } else if (direct instanceof Player player && source.is(DamageTypes.PLAYER_ATTACK)
            && MaceItem.canSmashAttack(player)) {
            bonus = AffixStats.sum(player.getMainHandItem(), AffixEffect.Type.SMASH_DAMAGE);
        }

        if (bonus > 0)
            event.setAmount((float) (event.getAmount() * (1 + bonus)));
    }

    // ---- arrows and bolts --------------------------------------------------------------------

    @SubscribeEvent
    public static void onArrowFired(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || event.loadedFromDisk())
            return;
        if (!(event.getEntity() instanceof AbstractArrow arrow))
            return;
        ItemStack weapon = arrow.getWeaponItem();
        if (weapon == null)
            return;

        double speed = AffixStats.sum(weapon, AffixEffect.Type.PROJECTILE_SPEED);
        if (speed > 0) {
            arrow.setDeltaMovement(arrow.getDeltaMovement().scale(1 + speed));
            arrow.hurtMarked = true;
        }
        int pierce = (int) Math.round(AffixStats.sum(weapon, AffixEffect.Type.PIERCING));
        if (pierce > 0)
            arrow.setPierceLevel((byte) Math.min(Byte.MAX_VALUE, arrow.getPierceLevel() + pierce));
    }

    /**
     * Draw speed: every so often a tick of use counts double. The skipped ticks depend only on how
     * long the item has been in use, so the client and the server agree without any packet.
     */
    @SubscribeEvent
    public static void onUseTick(LivingEntityUseItemEvent.Tick event) {
        double speed = AffixStats.sum(event.getItem(), AffixEffect.Type.DRAW_SPEED);
        if (speed <= 0)
            return;
        int used = event.getEntity().getTicksUsingItem();
        if ((int) (used * speed) > (int) ((used - 1) * speed))
            event.setDuration(event.getDuration() - 1);
    }

    // ---- potato cannon -----------------------------------------------------------------------

    private static ItemStack heldCannon(Entity owner) {
        if (!(owner instanceof LivingEntity living))
            return ItemStack.EMPTY;
        if (living.getMainHandItem().getItem() instanceof PotatoCannonItem)
            return living.getMainHandItem();
        if (living.getOffhandItem().getItem() instanceof PotatoCannonItem)
            return living.getOffhandItem();
        return ItemStack.EMPTY;
    }

    /** On its first tick, a potato copies the affixes of the cannon that fired it and gets its speed bonus. */
    @SubscribeEvent
    public static void onPotatoTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof PotatoProjectileEntity potato) || potato.level().isClientSide)
            return;
        CompoundTag data = potato.getPersistentData();
        if (data.getBoolean(KEY_INIT))
            return;
        data.putBoolean(KEY_INIT, true);

        ItemStack cannon = heldCannon(potato.getOwner());
        if (cannon.isEmpty())
            return;

        data.putFloat(KEY_DAMAGE, (float) AffixStats.sum(cannon, AffixEffect.Type.PROJECTILE_DAMAGE));
        data.putInt(KEY_PIERCE, (int) Math.round(AffixStats.sum(cannon, AffixEffect.Type.PIERCING)));
        data.putFloat(KEY_SHRAPNEL, (float) AffixStats.sum(cannon, AffixEffect.Type.SHRAPNEL));

        double speed = AffixStats.sum(cannon, AffixEffect.Type.PROJECTILE_SPEED);
        if (speed > 0) {
            potato.setDeltaMovement(potato.getDeltaMovement().scale(1 + speed));
            potato.hurtMarked = true;
        }
    }

    /**
     * Shrapnel bursts on every impact. Piercing lets the potato carry on: the potato that hit does
     * its normal (Create) hit and ends, and a copy continues from just behind the target, ignoring
     * everything the volley has already hit.
     */
    @SubscribeEvent
    public static void onPotatoImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof PotatoProjectileEntity potato)
            || !(potato.level() instanceof ServerLevel level))
            return;
        CompoundTag data = potato.getPersistentData();
        HitResult hit = event.getRayTraceResult();
        Entity hitEntity = hit instanceof EntityHitResult entityHit ? entityHit.getEntity() : null;

        if (hitEntity != null && isIgnored(data, hitEntity)) {
            event.setCanceled(true);
            return;
        }

        float shrapnel = data.getFloat(KEY_SHRAPNEL);
        if (shrapnel > 0)
            burstShrapnel(level, potato, hit.getLocation(), hitEntity, shrapnel);

        int pierce = data.getInt(KEY_PIERCE);
        if (hitEntity != null && pierce > 0)
            continueThrough(level, potato, hitEntity, hit.getLocation(), pierce - 1);
    }

    private static boolean isIgnored(CompoundTag data, Entity entity) {
        for (int id : data.getIntArray(KEY_IGNORE))
            if (id == entity.getId())
                return true;
        return false;
    }

    private static void burstShrapnel(ServerLevel level, PotatoProjectileEntity potato, Vec3 at,
        @Nullable Entity hitEntity, float fraction) {
        Entity owner = potato.getOwner();
        float damage = potato.getProjectileType().damage() * fraction;
        if (damage > 0) {
            DamageSource source = level.damageSources().thrown(potato, owner);
            AABB area = new AABB(at, at).inflate(SHRAPNEL_RADIUS);
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e.isAlive() && e != owner && e != hitEntity)) {
                if (entity instanceof Player target && owner instanceof Player attacker && !attacker.canHarmPlayer(target))
                    continue;
                entity.hurt(source, damage);
            }
        }

        ItemStack ammo = potato.getItem();
        if (!ammo.isEmpty())
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, ammo), at.x, at.y, at.z, 14, 0.3, 0.3, 0.3, 0.15);
        level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 8, 0.3, 0.3, 0.3, 0.2);
        level.playSound(null, BlockPos.containing(at), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 0.4f, 1.4f);
    }

    private static void continueThrough(ServerLevel level, PotatoProjectileEntity potato, Entity hitEntity, Vec3 at,
        int remainingPierce) {
        // Looked up in the vanilla registry: Create's own entry type lives in Registrate, which is not a compile dependency.
        if (!(BuiltInRegistries.ENTITY_TYPE.get(POTATO_PROJECTILE).create(level) instanceof PotatoProjectileEntity next))
            return;
        next.setItem(potato.getItem().copy());
        Entity owner = potato.getOwner();
        if (owner != null) {
            next.setOwner(owner);
            ItemStack cannon = heldCannon(owner);
            if (!cannon.isEmpty())
                next.setEnchantmentEffectsFromCannon(cannon);
        }

        Vec3 motion = potato.getDeltaMovement();
        next.setPos(at.add(motion.normalize().scale(0.4)));
        next.setDeltaMovement(motion);
        next.setYRot(potato.getYRot());
        next.setXRot(potato.getXRot());

        CompoundTag data = potato.getPersistentData();
        CompoundTag nextData = next.getPersistentData();
        nextData.putBoolean(KEY_INIT, true);
        nextData.putFloat(KEY_DAMAGE, data.getFloat(KEY_DAMAGE));
        nextData.putFloat(KEY_SHRAPNEL, data.getFloat(KEY_SHRAPNEL));
        nextData.putInt(KEY_PIERCE, remainingPierce);

        int[] ignored = data.getIntArray(KEY_IGNORE);
        int[] extended = java.util.Arrays.copyOf(ignored, ignored.length + 1);
        extended[ignored.length] = hitEntity.getId();
        nextData.putIntArray(KEY_IGNORE, extended);

        level.addFreshEntity(next);
    }
}
