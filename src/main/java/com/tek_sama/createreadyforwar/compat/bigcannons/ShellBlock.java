package com.tek_sama.createreadyforwar.compat.bigcannons;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.DirectionalBlock;
import rbasamoyai.createbigcannons.index.CBCMunitionPropertiesHandlers;
import rbasamoyai.createbigcannons.munitions.big_cannon.SimpleShellBlock;

/**
 * The block form of one of our shells, the way Create Big Cannons' own HE shell is built: it sits in
 * the cannon, takes a fuze and a tracer, and turns into its projectile when fired. It uses Create Big
 * Cannons' own "fuzed_block" block entity (our blocks are added to it, see {@link BigCannonsCompat}).
 */
public class ShellBlock extends SimpleShellBlock<ShellProjectile> {

    public static final MapCodec<ShellBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ShellKind.CODEC.fieldOf("kind").forGetter(ShellBlock::kind),
        propertiesCodec()
    ).apply(instance, ShellBlock::new));

    private final ShellKind kind;

    public ShellBlock(ShellKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public ShellKind kind() {
        return kind;
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public boolean isBaseFuze() {
        return CBCMunitionPropertiesHandlers.COMMON_SHELL_BIG_CANNON_PROJECTILE
            .getPropertiesOf(getAssociatedEntityType()).fuze().baseFuze();
    }

    @Override
    public EntityType<? extends ShellProjectile> getAssociatedEntityType() {
        return ModShells.projectile(kind);
    }
}
