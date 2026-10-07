package com.tek_sama.createreforge.block;

import java.util.List;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;
import com.tek_sama.createreforge.client.WarDrumEffects;
import com.tek_sama.createreforge.registry.ModBlockEntityTypes;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Beats as long as it turns. Every beat gives Strength to all the players within reach, and the
 * faster the drum spins the further it reaches and the faster it beats:
 *
 * <pre>
 *   speed      reach         beat every    effects
 *   any > 0    8 blocks      2 s           Strength I
 *   ...        (linear)      (linear)
 *   256 RPM    40 blocks     0.5 s         Strength II + Resistance I
 * </pre>
 */
public class WarDrumBlockEntity extends KineticBlockEntity {

    /** Create's top speed: the drum gives its full power there. */
    public static final float MAX_RPM = 256;
    public static final float MIN_RADIUS = 8;
    public static final float MAX_RADIUS = 40;
    /** Ticks between two beats at the lowest and at the top speed. */
    private static final int SLOWEST_BEAT = 40;
    private static final int FASTEST_BEAT = 10;
    /** Effects outlast the gap between two beats by this much, so they never flicker off. */
    private static final int EFFECT_MARGIN = 60;
    /** Stress units consumed per RPM: 2048 SU at top speed. */
    private static final float STRESS_PER_RPM = 8;
    /** Block event id of a beat; its parameter is the reach in blocks, plus 1000 at top speed. */
    public static final int BEAT_EVENT = 0;
    private static final int MAXED_FLAG = 1000;

    private int beatTimer;

    public WarDrumBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public WarDrumBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntityTypes.WAR_DRUM.get(), pos, state);
    }

    // ---- power curve --------------------------------------------------------------------------

    /** 0 when still, 1 at top speed. */
    private float power() {
        return Mth.clamp(Math.abs(getSpeed()) / MAX_RPM, 0f, 1f);
    }

    private boolean isMaxed() {
        return Math.abs(getSpeed()) >= MAX_RPM;
    }

    public float radius() {
        return Mth.lerp(power(), MIN_RADIUS, MAX_RADIUS);
    }

    public int beatInterval() {
        return Math.round(Mth.lerp(power(), SLOWEST_BEAT, FASTEST_BEAT));
    }

    @Override
    public float calculateStressApplied() {
        lastStressApplied = STRESS_PER_RPM;
        return STRESS_PER_RPM;
    }

    // ---- beating (server side) ----------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide)
            return;
        if (getSpeed() == 0 || isOverStressed()) {
            beatTimer = 0;
            return;
        }
        if (++beatTimer < beatInterval())
            return;
        beatTimer = 0;
        beat();
    }

    private void beat() {
        float radius = radius();
        boolean maxed = isMaxed();
        int duration = beatInterval() + EFFECT_MARGIN;

        Vec3 center = Vec3.atCenterOf(worldPosition);
        for (Player player : level.getEntitiesOfClass(Player.class, new AABB(worldPosition).inflate(radius),
            p -> p.isAlive() && !p.isSpectator() && p.distanceToSqr(center) <= radius * radius)) {
            // ambient = true, like a beacon: discreet particles on the player
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, maxed ? 1 : 0, true, true));
            if (maxed)
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, true, true));
        }

        // A deep thump that carries further as the drum speeds up (volume above 1 widens the range).
        level.playSound(null, worldPosition, SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.BLOCKS,
            1.0f + power() * 3.0f, 0.5f + level.random.nextFloat() * 0.1f);
        level.blockEvent(worldPosition, getBlockState().getBlock(), BEAT_EVENT,
            Math.round(radius) + (maxed ? MAXED_FLAG : 0));
    }

    /** Runs on both sides when a beat's block event arrives; the client draws the air wave. */
    public boolean onBeatEvent(int id, int param) {
        if (id != BEAT_EVENT)
            return false;
        if (level != null && level.isClientSide)
            WarDrumEffects.onBeat(level, worldPosition, param % MAXED_FLAG, param >= MAXED_FLAG);
        return true;
    }

    // ---- engineer's goggles -------------------------------------------------------------------

    /** Same trick as the War Forge: resolve our own keys first, CreateLang would prefix them with "create.". */
    private static LangBuilder ownText(String key, Object... args) {
        return CreateLang.text(Component.translatable(key, args).getString());
    }

    private static LangBuilder label(String key) {
        return ownText(key).style(ChatFormatting.GRAY).space();
    }

    private static String effectName(Holder<MobEffect> effect, int amplifier) {
        return Component.translatable(effect.value().getDescriptionId()).getString() + " "
            + Component.translatable("enchantment.level." + (amplifier + 1)).getString();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        ownText("createreforge.drum.header").style(ChatFormatting.GRAY).forGoggles(tooltip);
        if (getSpeed() == 0 || isOverStressed()) {
            ownText("createreforge.drum.silent").style(ChatFormatting.GOLD).forGoggles(tooltip, 1);
            return true;
        }

        label("createreforge.drum.reach")
            .add(ownText("createreforge.drum.blocks", Math.round(radius())).style(ChatFormatting.GOLD))
            .forGoggles(tooltip, 1);

        String effects = isMaxed()
            ? effectName(MobEffects.DAMAGE_BOOST, 1) + ", " + effectName(MobEffects.DAMAGE_RESISTANCE, 0)
            : effectName(MobEffects.DAMAGE_BOOST, 0);
        label("createreforge.drum.effects")
            .add(CreateLang.text(effects).style(ChatFormatting.AQUA))
            .forGoggles(tooltip, 1);

        label("createreforge.drum.tempo")
            .add(ownText("createreforge.drum.seconds", String.format("%.1f", beatInterval() / 20f))
                .style(ChatFormatting.WHITE))
            .forGoggles(tooltip, 1);

        if (!isMaxed())
            ownText("createreforge.drum.hint", Math.round(MAX_RPM)).style(ChatFormatting.DARK_GRAY)
                .forGoggles(tooltip, 1);
        return true;
    }
}
