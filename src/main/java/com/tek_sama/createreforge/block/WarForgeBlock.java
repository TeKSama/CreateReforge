package com.tek_sama.createreforge.block;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import com.tek_sama.createreforge.registry.ModBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * The War Forge: a two-block-wide machine, placed like a bed. The block that is clicked becomes the
 * LEFT half and its partner appears on its right (as seen from the front of the machine).
 *
 * <pre>
 *   LEFT  half: front = weapon/tool/armor input, back = output, left end = material (tier block) input
 *   RIGHT half: front = lava input, right end = rotation input, back = unused
 *   Top and bottom of both halves are closed.
 * </pre>
 *
 * Breaking either half removes the other one. Only the LEFT half carries the loot-table drop.
 */
public class WarForgeBlock extends HorizontalKineticBlock implements IBE<WarForgeBlockEntity> {

    public static final EnumProperty<ForgePart> PART = EnumProperty.create("part", ForgePart.class);
    /** How full the lava buffer is, 0 (empty) to 4 (full); only the RIGHT half ever leaves 0. Drives the model and the glow. */
    public static final IntegerProperty LAVA = IntegerProperty.create("lava", 0, 4);

    public WarForgeBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(PART, ForgePart.LEFT).setValue(LAVA, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PART, LAVA);
    }

    // ---- geometry helpers -------------------------------------------------------------------

    /** Direction of the RIGHT half relative to the LEFT one, seen from the front of the machine. */
    public static Direction rightOf(Direction facing) {
        return facing.getCounterClockWise();
    }

    /** Direction of the LEFT half relative to the RIGHT one, seen from the front of the machine. */
    public static Direction leftOf(Direction facing) {
        return facing.getClockWise();
    }

    /** Direction pointing from this half to the other half. */
    public static Direction partnerDirection(BlockState state) {
        Direction facing = state.getValue(HORIZONTAL_FACING);
        return state.getValue(PART) == ForgePart.LEFT ? rightOf(facing) : leftOf(facing);
    }

    public static BlockPos partnerPos(BlockPos pos, BlockState state) {
        return pos.relative(partnerDirection(state));
    }

    private boolean isPartnerOf(BlockState state, BlockState other) {
        return other.is(this)
            && other.getValue(PART) != state.getValue(PART)
            && other.getValue(HORIZONTAL_FACING) == state.getValue(HORIZONTAL_FACING);
    }

    // ---- placement --------------------------------------------------------------------------

    /** The front of the machine faces the player. Placement fails if the right half would not fit. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos rightPos = context.getClickedPos().relative(rightOf(facing));
        Level level = context.getLevel();
        if (level.isOutsideBuildHeight(rightPos)
            || !level.getWorldBorder().isWithinBounds(rightPos)
            || !level.getBlockState(rightPos).canBeReplaced(context))
            return null;
        return defaultBlockState().setValue(HORIZONTAL_FACING, facing).setValue(PART, ForgePart.LEFT);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide)
            return;
        BlockPos rightPos = partnerPos(pos, state);
        level.setBlock(rightPos, state.setValue(PART, ForgePart.RIGHT), Block.UPDATE_ALL);
        level.blockUpdated(pos, Blocks.AIR);
        state.updateNeighbourShapes(level, pos, Block.UPDATE_ALL);
    }

    // ---- breaking ---------------------------------------------------------------------------

    /** If one half disappears for any reason (explosion, command...), the other one goes too. */
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
        LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == partnerDirection(state) && !isPartnerOf(state, neighborState))
            return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            BlockPos otherPos = partnerPos(pos, state);
            BlockState otherState = level.getBlockState(otherPos);
            if (isPartnerOf(state, otherState)) {
                // Only the LEFT half has a loot table, so when the player mines the RIGHT half
                // the drop is produced on behalf of the LEFT one (respecting the pickaxe rule).
                if (state.getValue(PART) == ForgePart.RIGHT && !player.isCreative()
                    && player.hasCorrectToolForDrops(otherState))
                    Block.dropResources(otherState, level, otherPos, level.getBlockEntity(otherPos), player,
                        player.getMainHandItem());
                level.setBlock(otherPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                level.levelEvent(player, 2001, otherPos, Block.getId(otherState));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    // ---- Create wrench ----------------------------------------------------------------------

    /** The double block cannot be rotated with a wrench. */
    @Override
    public BlockState getRotatedBlockState(BlockState originalState, Direction targetedFace) {
        return originalState;
    }

    /** Dismantling the RIGHT half with a wrench must give the machine back, like the LEFT half. */
    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        if (state.getValue(PART) == ForgePart.LEFT)
            return super.onSneakWrenched(state, context);

        Level level = context.getLevel();
        BlockPos otherPos = partnerPos(context.getClickedPos(), state);
        BlockState otherState = level.getBlockState(otherPos);
        if (!isPartnerOf(state, otherState))
            return super.onSneakWrenched(state, context);

        Player player = context.getPlayer();
        if (level instanceof ServerLevel serverLevel) {
            if (player != null && !player.isCreative())
                Block.getDrops(otherState, serverLevel, otherPos, level.getBlockEntity(otherPos), player,
                    context.getItemInHand()).forEach(stack -> player.getInventory().placeItemBackInInventory(stack));
            otherState.spawnAfterBreak(serverLevel, otherPos, ItemStack.EMPTY, true);
            level.destroyBlock(otherPos, false);
            IWrenchable.playRemoveSound(level, otherPos);
        }
        return InteractionResult.SUCCESS;
    }

    // ---- kinetics: only the RIGHT half takes rotation, through its right end -----------------

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return rightOf(state.getValue(HORIZONTAL_FACING)).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return state.getValue(PART) == ForgePart.RIGHT && face == rightOf(state.getValue(HORIZONTAL_FACING));
    }

    // ---- block entity -----------------------------------------------------------------------

    @Override
    public Class<WarForgeBlockEntity> getBlockEntityClass() {
        return WarForgeBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends WarForgeBlockEntity> getBlockEntityType() {
        return ModBlockEntityTypes.WAR_FORGE.get();
    }
}
