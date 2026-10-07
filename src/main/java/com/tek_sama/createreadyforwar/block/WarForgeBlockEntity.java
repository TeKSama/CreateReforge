package com.tek_sama.createreadyforwar.block;

import java.util.List;

import javax.annotation.Nullable;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;
import com.tek_sama.createreadyforwar.affix.ForgedAffixes;
import com.tek_sama.createreadyforwar.forge.AffixForge;
import com.tek_sama.createreadyforwar.forge.ForgeStatus;
import com.tek_sama.createreadyforwar.forge.ForgeTier;
import com.tek_sama.createreadyforwar.forge.ReforgeCategory;
import com.tek_sama.createreadyforwar.registry.ModBlockEntityTypes;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Block entity shared by both halves of the War Forge.
 *
 * <ul>
 * <li>The LEFT half holds the three item slots, reachable only through funnels, belts and other
 * automation (there is no GUI and no hand interaction).</li>
 * <li>The RIGHT half is the kinetic "core": it takes rotation from its right end, lava from its
 * front, and runs the forging cycle using the items stored in the LEFT half.</li>
 * </ul>
 */
public class WarForgeBlockEntity extends KineticBlockEntity {

    public static final int MATERIAL_SLOT_LIMIT = 64;
    /** Deliberately tiny: the machine is meant to be fed by pipes from a bigger reserve next to it. */
    public static final int LAVA_CAPACITY = 1_000;
    /** While forging, clients are refreshed this often (ticks) so the goggles' progress stays current. */
    private static final int SYNC_INTERVAL = 20;
    private static final int BAR_SEGMENTS = 10;
    /** While forging, an anvil strike rings this often (ticks)... */
    private static final int FORGE_STRIKE_INTERVAL = 60;
    /** ...and this loud: quiet on purpose, a forge can run for over an hour. */
    private static final float FORGE_STRIKE_VOLUME = 0.25f;

    // ---- LEFT half: item slots --------------------------------------------------------------

    /** The weapon, tool or armor waiting to be reforged. Fed from the front of the LEFT half. */
    private final ItemStackHandler toolInput = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return ReforgeCategory.isReforgeable(stack);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            inventoryChanged();
        }
    };

    /** Blocks of iron, diamond or netherite. Fed from the left end of the LEFT half. */
    private final ItemStackHandler materialInput = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return ForgeTier.fromMaterial(stack) != null;
        }

        @Override
        public int getSlotLimit(int slot) {
            return MATERIAL_SLOT_LIMIT;
        }

        @Override
        protected void onContentsChanged(int slot) {
            inventoryChanged();
        }
    };

    /** The reforged item. Taken from the back of the LEFT half; nothing can be inserted. */
    private final ItemStackHandler output = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            inventoryChanged();
        }
    };

    private final IItemHandler toolInputFace = new InsertOnlyItemHandler(toolInput);
    private final IItemHandler materialInputFace = new InsertOnlyItemHandler(materialInput);

    // ---- RIGHT half: lava and forging state -------------------------------------------------

    // No initializer on purpose: this is assigned by addBehaviours(), which runs inside the
    // superclass constructor, before field initializers would run.
    private SmartFluidTankBehaviour lavaTank;
    private ScrollOptionBehaviour<AffixSlot> slotSelector;

    /** The tier of the forge currently in progress, or null when the machine is idle. */
    @Nullable
    private ForgeTier forgingTier;
    /** The affix slot being forged, locked in when the forge starts (the dial may move afterwards). */
    private int forgingSlot;
    /** Effective forging ticks done so far (slower rotation below the ideal zone earns less per tick). */
    private float progress;
    /** Accumulated over-speed; turns into a failure chance when the forge completes. */
    private float instability;
    /** Lava already drained from the tank but not yet "spent" (lava is pulled 1 mB at a time). */
    private float lavaCredit;
    private ForgeStatus status = ForgeStatus.WAITING_TOOL;
    private int syncTimer;

    public WarForgeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public WarForgeBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntityTypes.WAR_FORGE.get(), pos, state);
    }

    public boolean isCore() {
        return getBlockState().getValue(WarForgeBlock.PART) == ForgePart.RIGHT;
    }

    /** The other half of the machine, if it is loaded. */
    @Nullable
    private WarForgeBlockEntity findPartner() {
        if (level == null)
            return null;
        BlockPos partnerPos = WarForgeBlock.partnerPos(worldPosition, getBlockState());
        return level.getBlockEntity(partnerPos) instanceof WarForgeBlockEntity forge ? forge : null;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        if (!isCore())
            return;
        lavaTank = SmartFluidTankBehaviour.single(this, LAVA_CAPACITY).forbidExtraction();
        lavaTank.getPrimaryHandler().setValidator(stack -> stack.getFluid().isSame(Fluids.LAVA));
        behaviours.add(lavaTank);
        behaviours.add(slotSelector = new ScrollOptionBehaviour<>(AffixSlot.class,
            Component.translatable("createreadyforwar.slot_label"), this, new AffixSlotDial()));
    }

    // ---- automation access ------------------------------------------------------------------

    /**
     * The item handler seen by automation on the given face, or null when the face has none.
     * Only the LEFT half has item faces: front = tool input, back = output, left end = material.
     */
    @Nullable
    public IItemHandler getItemHandler(@Nullable Direction side) {
        if (side == null || isCore())
            return null;
        Direction facing = getBlockState().getValue(WarForgeBlock.HORIZONTAL_FACING);
        if (side == facing)
            return toolInputFace;
        if (side == facing.getOpposite())
            return output;
        if (side == WarForgeBlock.leftOf(facing))
            return materialInputFace;
        return null;
    }

    /** Lava can only be piped into the front face of the RIGHT half. */
    @Nullable
    public IFluidHandler getFluidHandler(@Nullable Direction side) {
        if (side == null || !isCore() || lavaTank == null)
            return null;
        if (side != getBlockState().getValue(WarForgeBlock.HORIZONTAL_FACING))
            return null;
        return lavaTank.getCapability();
    }

    private void inventoryChanged() {
        setChanged();
        if (level != null && !level.isClientSide)
            sendData();
    }

    // ---- kinetics ---------------------------------------------------------------------------

    /** Stress per RPM: nothing while idle, then the tier's cost for as long as a forge is in progress. */
    @Override
    public float calculateStressApplied() {
        float impact = isCore() && forgingTier != null ? forgingTier.stressPerRpm() : 0;
        lastStressApplied = impact;
        return impact;
    }

    /** Tells the kinetic network the stress changed (a forge started or finished). */
    private void refreshStress() {
        float impact = calculateStressApplied();
        if (hasNetwork())
            getOrCreateNetwork().updateStressFor(this, impact);
    }

    // ---- forging cycle (RIGHT half, server side) ---------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (level == null || !isCore())
            return;
        if (level.isClientSide) {
            spawnForgeParticles();
            return;
        }
        WarForgeBlockEntity inventory = findPartner();
        if (inventory == null || inventory.isCore())
            return;

        updateLavaLevel();
        ForgeStatus previous = status;
        status = runForge(inventory);
        // Soft anvil strikes all along the forge; the offset keeps several machines out of lockstep.
        if (status.isProgressing() && (level.getGameTime() + worldPosition.asLong()) % FORGE_STRIKE_INTERVAL == 0)
            level.playSound(null, worldPosition, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, FORGE_STRIKE_VOLUME,
                0.85f + level.random.nextFloat() * 0.3f);

        boolean refresh = status != previous;
        if (forgingTier != null && ++syncTimer >= SYNC_INTERVAL) {
            syncTimer = 0;
            refresh = true;
            setChanged();
        }
        if (refresh)
            sendData();
    }

    /** Mirrors the lava buffer into the block state (5 levels), which swaps the model and lights the machine. */
    private void updateLavaLevel() {
        int amount = lavaTank.getPrimaryHandler().getFluidAmount();
        int lavaLevel = amount <= 0 ? 0 : 1 + (amount - 1) * 4 / LAVA_CAPACITY;
        BlockState state = getBlockState();
        if (state.getValue(WarForgeBlock.LAVA) != lavaLevel)
            level.setBlock(worldPosition, state.setValue(WarForgeBlock.LAVA, lavaLevel), Block.UPDATE_CLIENTS);
    }

    /** Client side: smoke over the open top while forging, lava pops when there is lava, black smoke when over-speeding. */
    private void spawnForgeParticles() {
        if (!status.isProgressing())
            return;
        RandomSource random = level.random;
        double x = worldPosition.getX();
        double y = worldPosition.getY() + 1.0;
        double z = worldPosition.getZ();
        if (random.nextInt(3) == 0)
            level.addParticle(ParticleTypes.SMOKE, x + 0.3 + random.nextDouble() * 0.4, y,
                z + 0.3 + random.nextDouble() * 0.4, 0, 0.04, 0);
        if (getBlockState().getValue(WarForgeBlock.LAVA) > 0 && random.nextInt(12) == 0)
            level.addParticle(ParticleTypes.LAVA, x + 0.35 + random.nextDouble() * 0.3, y - 0.1,
                z + 0.35 + random.nextDouble() * 0.3, 0, 0, 0);
        if (status == ForgeStatus.TOO_FAST && random.nextInt(2) == 0)
            level.addParticle(ParticleTypes.LARGE_SMOKE, x + 0.3 + random.nextDouble() * 0.4, y,
                z + 0.3 + random.nextDouble() * 0.4, 0, 0.05, 0);
    }

    private ForgeStatus runForge(WarForgeBlockEntity inventory) {
        float speed = Math.abs(getSpeed());

        if (forgingTier == null) {
            ItemStack tool = inventory.toolInput.getStackInSlot(0);
            if (tool.isEmpty())
                return ForgeStatus.WAITING_TOOL;
            ForgeTier tier = ForgeTier.fromMaterial(inventory.materialInput.getStackInSlot(0));
            if (tier == null)
                return ForgeStatus.WAITING_MATERIAL;
            if (!inventory.output.getStackInSlot(0).isEmpty())
                return ForgeStatus.OUTPUT_BLOCKED;
            if (speed == 0)
                return ForgeStatus.NO_ROTATION;

            // Start: the block of material is consumed up front.
            inventory.materialInput.extractItem(0, 1, false);
            forgingTier = tier;
            forgingSlot = slotSelector.get().index();
            progress = 0;
            instability = 0;
            lavaCredit = 0;
            syncTimer = 0;
            setChanged();
            refreshStress();
            level.playSound(null, worldPosition, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.8f, 0.8f);
        }

        ForgeTier tier = forgingTier;
        if (speed == 0)
            return ForgeStatus.NO_ROTATION;

        // Below the ideal zone the forge still runs, but proportionally slower.
        float rate = speed < tier.minRpm() ? speed / tier.minRpm() : 1f;

        // Lava is drained progressively, 1 mB at a time, in proportion to the progress made.
        float lavaStep = (float) tier.lavaMillibuckets() / tier.durationTicks() * rate;
        // (the epsilon keeps float rounding from demanding one mB more than the forge really costs)
        if (lavaCredit + 0.0001f < lavaStep) {
            FluidStack drained = lavaTank.getPrimaryHandler().drain(1, IFluidHandler.FluidAction.EXECUTE);
            if (drained.getAmount() < 1)
                return ForgeStatus.NO_LAVA;
            lavaCredit += 1;
        }
        lavaCredit = Math.max(0f, lavaCredit - lavaStep);

        progress += rate;
        if (speed > tier.maxRpm())
            instability += Math.min(1f, (speed - tier.maxRpm()) / tier.maxRpm());

        if (progress >= tier.durationTicks()) {
            complete(inventory, tier);
            return ForgeStatus.WAITING_TOOL;
        }
        if (speed < tier.minRpm())
            return ForgeStatus.TOO_SLOW;
        if (speed > tier.maxRpm())
            return ForgeStatus.TOO_FAST;
        return ForgeStatus.FORGING;
    }

    /**
     * Finishes the forge. Spending the whole forge above the ideal speed makes failure certain; a
     * failed forge still burns the material and the lava but hands the item back unchanged.
     */
    private void complete(WarForgeBlockEntity inventory, ForgeTier tier) {
        ItemStack tool = inventory.toolInput.extractItem(0, 1, false);
        if (!tool.isEmpty()) {
            boolean failed = level.random.nextFloat() < instability / tier.durationTicks();
            ItemStack result = failed ? tool
                : AffixForge.forge(tool, tier, forgingSlot, level.registryAccess(), level.random);
            inventory.output.setStackInSlot(0, result);
            playCompletionEffects(failed);
        }
        forgingTier = null;
        progress = 0;
        instability = 0;
        lavaCredit = 0;
        setChanged();
        refreshStress();
    }

    /**
     * Success: the sound of a big experience orb (two layers, one deep and one bright) and a burst
     * of lava and flames. Failure: a dull anvil crack and black smoke.
     */
    private void playCompletionEffects(boolean failed) {
        if (!(level instanceof ServerLevel server))
            return;
        double x = worldPosition.getX() + 0.5;
        double y = worldPosition.getY() + 1.0;
        double z = worldPosition.getZ() + 0.5;
        if (failed) {
            server.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 14, 0.3, 0.1, 0.3, 0.02);
            server.playSound(null, worldPosition, SoundEvents.ANVIL_BREAK, SoundSource.BLOCKS, 0.5f, 0.8f);
        } else {
            server.sendParticles(ParticleTypes.LAVA, x, y, z, 12, 0.25, 0.1, 0.25, 0);
            server.sendParticles(ParticleTypes.FLAME, x, y, z, 10, 0.25, 0.1, 0.25, 0.03);
            server.playSound(null, worldPosition, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 1.0f, 0.65f);
            server.playSound(null, worldPosition, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.6f, 1.05f);
        }
    }

    // ---- persistence and sync ---------------------------------------------------------------

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("ToolInput", toolInput.serializeNBT(registries));
        tag.put("MaterialInput", materialInput.serializeNBT(registries));
        tag.put("Output", output.serializeNBT(registries));
        if (forgingTier != null)
            tag.putString("ForgingTier", forgingTier.name());
        tag.putInt("ForgingSlot", forgingSlot);
        tag.putFloat("Progress", progress);
        tag.putFloat("Instability", instability);
        tag.putFloat("LavaCredit", lavaCredit);
        tag.putString("Status", status.name());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        toolInput.deserializeNBT(registries, tag.getCompound("ToolInput"));
        materialInput.deserializeNBT(registries, tag.getCompound("MaterialInput"));
        output.deserializeNBT(registries, tag.getCompound("Output"));
        forgingTier = ForgeTier.byName(tag.getString("ForgingTier"));
        forgingSlot = Mth.clamp(tag.getInt("ForgingSlot"), 0, ForgedAffixes.SLOTS - 1);
        progress = tag.getFloat("Progress");
        instability = tag.getFloat("Instability");
        lavaCredit = tag.getFloat("LavaCredit");
        status = ForgeStatus.byName(tag.getString("Status"));
    }

    /** Called when the block is removed: whatever is inside falls out. */
    @Override
    public void destroy() {
        super.destroy();
        if (level == null || level.isClientSide || isCore())
            return;
        for (ItemStackHandler handler : new ItemStackHandler[] { toolInput, materialInput, output }) {
            ItemStack stack = handler.getStackInSlot(0);
            if (!stack.isEmpty())
                Block.popResource(level, worldPosition, stack);
        }
    }

    // ---- engineer's goggles -----------------------------------------------------------------

    /**
     * {@code CreateLang.translate(key)} silently prefixes the key with "create.", so our own keys
     * are resolved the normal way first and only then handed to CreateLang for its goggle
     * formatting and indentation helpers.
     */
    private static LangBuilder ownText(String key, Object... args) {
        return CreateLang.text(Component.translatable(key, args).getString());
    }

    private static LangBuilder label(String key) {
        return ownText(key).style(ChatFormatting.GRAY).space();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        // Aiming at either half shows the same information, built from the core.
        if (!isCore()) {
            WarForgeBlockEntity core = findPartner();
            return core != null && core.isCore() && core.addToGoggleTooltip(tooltip, isPlayerSneaking);
        }

        // Create's own speed / stress section first.
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        addForgeInfo(tooltip, isPlayerSneaking);
        return true;
    }

    /** The War Forge's own section of the goggles tooltip (status, items, tier, progress, lava). */
    public void addForgeInfo(List<Component> tooltip, boolean isPlayerSneaking) {
        WarForgeBlockEntity inventory = findPartner();
        if (!isCore() || inventory == null || inventory.isCore())
            return;

        ownText("createreadyforwar.goggles.header").style(ChatFormatting.GRAY).forGoggles(tooltip);

        label("createreadyforwar.goggles.status")
            .add(ownText(status.translationKey()).style(status.color()))
            .forGoggles(tooltip, 1);

        ItemStack tool = inventory.toolInput.getStackInSlot(0);
        if (!tool.isEmpty())
            label("createreadyforwar.goggles.item")
                .add(CreateLang.itemName(tool).style(ChatFormatting.WHITE))
                .forGoggles(tooltip, 1);

        ItemStack material = inventory.materialInput.getStackInSlot(0);
        if (!material.isEmpty())
            label("createreadyforwar.goggles.material")
                .add(CreateLang.itemName(material).style(ChatFormatting.WHITE))
                .add(CreateLang.text(" x" + material.getCount()).style(ChatFormatting.GOLD))
                .forGoggles(tooltip, 1);

        // The slot being forged, or the one the dial is set to for the next forge.
        int slotNumber = (forgingTier != null ? forgingSlot : slotSelector.get().index()) + 1;
        label("createreadyforwar.goggles.slot")
            .add(CreateLang.text(String.valueOf(slotNumber)).style(ChatFormatting.GOLD))
            .forGoggles(tooltip, 1);

        // The tier being forged, or the one the stored material would start.
        ForgeTier tier = forgingTier != null ? forgingTier : ForgeTier.fromMaterial(material);
        if (tier != null)
            addTierLines(tooltip, tier);

        if (forgingTier != null)
            addProgressLines(tooltip, forgingTier);

        ItemStack result = inventory.output.getStackInSlot(0);
        if (!result.isEmpty())
            label("createreadyforwar.goggles.output")
                .add(CreateLang.itemName(result).style(ChatFormatting.GREEN))
                .forGoggles(tooltip, 1);

        if (lavaTank != null)
            containedFluidTooltip(tooltip, isPlayerSneaking, lavaTank.getCapability());
    }

    private void addTierLines(List<Component> tooltip, ForgeTier tier) {
        label("createreadyforwar.goggles.tier")
            .add(ownText(tier.translationKey()).style(tier.color()))
            .forGoggles(tooltip, 1);

        float speed = Math.abs(getSpeed());
        boolean inZone = speed >= tier.minRpm() && speed <= tier.maxRpm();
        label("createreadyforwar.goggles.ideal_speed")
            .add(CreateLang.text(tier.minRpm() + "-" + tier.maxRpm() + " RPM").style(ChatFormatting.GOLD))
            .space()
            .add(ownText("createreadyforwar.goggles.now", Math.round(speed))
                .style(inZone ? ChatFormatting.GREEN : ChatFormatting.RED))
            .forGoggles(tooltip, 1);

        label("createreadyforwar.goggles.lava_cost")
            .add(CreateLang.text(tier.lavaMillibuckets() + " mB").style(ChatFormatting.GOLD))
            .forGoggles(tooltip, 1);

        label("createreadyforwar.goggles.duration")
            .add(CreateLang.text(formatDuration(tier.durationTicks())).style(ChatFormatting.AQUA))
            .forGoggles(tooltip, 1);
    }

    private void addProgressLines(List<Component> tooltip, ForgeTier tier) {
        float fraction = Math.min(1f, progress / tier.durationTicks());
        int percent = Math.round(fraction * 100f);
        int filled = Math.round(fraction * BAR_SEGMENTS);

        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < BAR_SEGMENTS; i++)
            bar.append(i < filled ? '|' : ' ');
        bar.append(']');

        label("createreadyforwar.goggles.progress")
            .add(CreateLang.text(bar.toString()).style(status.isProgressing() ? ChatFormatting.GREEN : ChatFormatting.GRAY))
            .space()
            .add(CreateLang.text(percent + "%").style(ChatFormatting.WHITE))
            .forGoggles(tooltip, 1);

        float speed = Math.abs(getSpeed());
        float rate = speed <= 0 ? 0 : speed < tier.minRpm() ? speed / tier.minRpm() : 1f;
        String remaining = rate > 0 && status != ForgeStatus.NO_LAVA
            ? formatDuration((tier.durationTicks() - progress) / rate)
            : "--";
        label("createreadyforwar.goggles.time_left")
            .add(CreateLang.text(remaining).style(ChatFormatting.AQUA))
            .forGoggles(tooltip, 1);
    }

    /** "1 h 05 min", "12 min 30 s" or "45 s". */
    private static String formatDuration(float ticks) {
        int seconds = Math.max(0, Math.round(ticks / 20f));
        if (seconds >= 3600)
            return (seconds / 3600) + " h " + String.format("%02d", (seconds % 3600) / 60) + " min";
        if (seconds >= 60)
            return (seconds / 60) + " min " + (seconds % 60) + " s";
        return seconds + " s";
    }
}
