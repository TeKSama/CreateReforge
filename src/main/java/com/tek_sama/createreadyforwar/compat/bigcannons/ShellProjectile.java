package com.tek_sama.createreadyforwar.compat.bigcannons;

import javax.annotation.Nonnull;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import rbasamoyai.createbigcannons.index.CBCMunitionPropertiesHandlers;
import rbasamoyai.createbigcannons.munitions.big_cannon.FuzedBigCannonProjectile;
import rbasamoyai.createbigcannons.munitions.big_cannon.config.BigCannonCommonShellProperties;
import rbasamoyai.createbigcannons.munitions.big_cannon.config.BigCannonFuzePropertiesComponent;
import rbasamoyai.createbigcannons.munitions.big_cannon.config.BigCannonProjectilePropertiesComponent;
import rbasamoyai.createbigcannons.munitions.config.components.BallisticPropertiesComponent;
import rbasamoyai.createbigcannons.munitions.config.components.EntityDamagePropertiesComponent;

/**
 * A fired shell in flight. Ballistics, damage, fuze behaviour and explosion power all come from the
 * data-driven properties Create Big Cannons uses for its HE shell, read from
 * {@code data/createreadyforwar/munition_properties/projectiles/<shell>.json}. Each subclass only
 * decides what happens on detonation.
 */
public abstract class ShellProjectile extends FuzedBigCannonProjectile {

    protected ShellProjectile(EntityType<? extends ShellProjectile> type, Level level) {
        super(type, level);
    }

    protected abstract ShellKind kind();

    @Override
    public BlockState getRenderedBlockState() {
        return ModShells.block(kind()).defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH);
    }

    protected BigCannonCommonShellProperties getAllProperties() {
        return CBCMunitionPropertiesHandlers.COMMON_SHELL_BIG_CANNON_PROJECTILE.getPropertiesOf(this);
    }

    @Nonnull
    @Override
    protected BigCannonFuzePropertiesComponent getFuzeProperties() {
        return getAllProperties().fuze();
    }

    @Nonnull
    @Override
    protected BigCannonProjectilePropertiesComponent getBigCannonProjectileProperties() {
        return getAllProperties().bigCannonProperties();
    }

    @Nonnull
    @Override
    public EntityDamagePropertiesComponent getDamageProperties() {
        return getAllProperties().damage();
    }

    @Nonnull
    @Override
    protected BallisticPropertiesComponent getBallisticProperties() {
        return getAllProperties().ballistics();
    }
}
