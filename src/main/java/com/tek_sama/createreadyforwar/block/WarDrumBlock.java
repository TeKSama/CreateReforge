package com.tek_sama.createreadyforwar.block;

import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.tek_sama.createreadyforwar.registry.ModBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The War Drum: an andesite casing with a hide stretched over its top. It takes rotation from a
 * shaft below and beats as long as it turns, buffing every player around it (see
 * {@link WarDrumBlockEntity}).
 */
public class WarDrumBlock extends KineticBlock implements IBE<WarDrumBlockEntity> {

    public WarDrumBlock(Properties properties) {
        super(properties);
    }

    // ---- kinetics: rotation comes in from below only ------------------------------------------

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return Direction.Axis.Y;
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == Direction.DOWN;
    }

    // ---- beats ------------------------------------------------------------------------------

    /**
     * Each beat is sent as a block event so that every nearby client plays the air wave at the same
     * moment. Plain blocks ignore block events, so they are forwarded to the block entity here.
     */
    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        return level.getBlockEntity(pos) instanceof WarDrumBlockEntity drum && drum.onBeatEvent(id, param);
    }

    // ---- block entity -----------------------------------------------------------------------

    @Override
    public Class<WarDrumBlockEntity> getBlockEntityClass() {
        return WarDrumBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends WarDrumBlockEntity> getBlockEntityType() {
        return ModBlockEntityTypes.WAR_DRUM.get();
    }
}
