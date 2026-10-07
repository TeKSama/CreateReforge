package com.tek_sama.createreadyforwar.compat.bigcannons;

import java.util.EnumMap;
import java.util.Map;

import com.tek_sama.createreadyforwar.Createreadyforwar;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import rbasamoyai.createbigcannons.munitions.FuzedProjectileBlockItem;

/** Blocks, items and entity types of our shells. Only registered when Create Big Cannons is loaded. */
public final class ModShells {

    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Createreadyforwar.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Createreadyforwar.MODID);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(Registries.ENTITY_TYPE, Createreadyforwar.MODID);

    private static final Map<ShellKind, DeferredBlock<ShellBlock>> SHELL_BLOCKS = new EnumMap<>(ShellKind.class);
    private static final Map<ShellKind, DeferredItem<FuzedProjectileBlockItem>> SHELL_ITEMS = new EnumMap<>(ShellKind.class);
    private static final Map<ShellKind, DeferredHolder<EntityType<?>, EntityType<? extends ShellProjectile>>> PROJECTILES =
        new EnumMap<>(ShellKind.class);

    static {
        for (ShellKind kind : ShellKind.values()) {
            // Same feel as Create Big Cannons' shells: quick to break, stone sounds, a thin cylinder.
            DeferredBlock<ShellBlock> block = BLOCKS.register(kind.id(), () -> new ShellBlock(kind,
                BlockBehaviour.Properties.of()
                    .mapColor(kind.color())
                    .strength(2.0f, 3.0f)
                    .sound(SoundType.STONE)
                    .noOcclusion()));
            SHELL_BLOCKS.put(kind, block);
            SHELL_ITEMS.put(kind, ITEMS.register(kind.id(),
                () -> new FuzedProjectileBlockItem(block.get(), new Item.Properties())));
            PROJECTILES.put(kind, ENTITY_TYPES.register(kind.id(), () -> shellType(kind)));
        }
    }

    public static final DeferredHolder<EntityType<?>, EntityType<ClusterBomblet>> BOMBLET =
        ENTITY_TYPES.register("cluster_bomblet", () -> EntityType.Builder.<ClusterBomblet>of(ClusterBomblet::new,
                MobCategory.MISC)
            .sized(0.25f, 0.25f)
            .clientTrackingRange(8)
            .updateInterval(2)
            .build("cluster_bomblet"));

    private ModShells() {}

    /** Same entity settings as Create Big Cannons' own shells. */
    private static EntityType<? extends ShellProjectile> shellType(ShellKind kind) {
        return switch (kind) {
            case HE_PLUS -> build(HEPlusShellProjectile::new, kind.id());
            case FLASHBANG -> build(FlashbangShellProjectile::new, kind.id());
            case DISPERSAL -> build(DispersalShellProjectile::new, kind.id());
            case CLUSTER -> build(ClusterShellProjectile::new, kind.id());
        };
    }

    private static <T extends ShellProjectile> EntityType<T> build(EntityType.EntityFactory<T> factory, String id) {
        return EntityType.Builder.of(factory, MobCategory.MISC)
            .sized(0.8f, 0.8f)
            .fireImmune()
            .clientTrackingRange(16)
            .updateInterval(1)
            .setShouldReceiveVelocityUpdates(false)
            .build(id);
    }

    static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        ENTITY_TYPES.register(modEventBus);
    }

    public static ShellBlock block(ShellKind kind) {
        return SHELL_BLOCKS.get(kind).get();
    }

    public static FuzedProjectileBlockItem item(ShellKind kind) {
        return SHELL_ITEMS.get(kind).get();
    }

    public static EntityType<? extends ShellProjectile> projectile(ShellKind kind) {
        return PROJECTILES.get(kind).get();
    }

    public static Iterable<DeferredHolder<EntityType<?>, EntityType<? extends ShellProjectile>>> projectiles() {
        return PROJECTILES.values();
    }
}
