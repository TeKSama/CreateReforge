package com.tek_sama.createreadyforwar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.tek_sama.createreadyforwar.affix.Affix;
import com.tek_sama.createreadyforwar.forge.AffixForge;
import com.tek_sama.createreadyforwar.forge.ForgeTier;
import com.tek_sama.createreadyforwar.forge.ReforgeCategory;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /reforge <targets> <affix> [<tier>] [<slot>]}, in the spirit of {@code /enchant}: forces an
 * affix onto the item the targets hold in their main hand.
 *
 * <ul>
 * <li>{@code affix}: an affix id; {@code lifesteal} is short for {@code createreadyforwar:lifesteal}</li>
 * <li>{@code tier}: 1 (iron), 2 (diamond) or 3 (netherite); 1 by default</li>
 * <li>{@code slot}: 1 to 3; by default the first empty slot, or slot 1 when all three are taken</li>
 * </ul>
 */
@EventBusSubscriber(modid = Createreadyforwar.MODID)
public class ReforgeCommand {

    private static final DynamicCommandExceptionType UNKNOWN_AFFIX = new DynamicCommandExceptionType(
        id -> Component.translatable("commands.createreadyforwar.reforge.unknown_affix", id));
    private static final DynamicCommandExceptionType NO_ITEM = new DynamicCommandExceptionType(
        name -> Component.translatable("commands.createreadyforwar.reforge.no_item", name));
    private static final DynamicCommandExceptionType NOT_REFORGEABLE = new DynamicCommandExceptionType(
        name -> Component.translatable("commands.createreadyforwar.reforge.not_reforgeable", name));
    private static final DynamicCommandExceptionType INCOMPATIBLE = new DynamicCommandExceptionType(
        name -> Component.translatable("commands.createreadyforwar.reforge.incompatible", name));
    private static final DynamicCommandExceptionType NOTHING_REFORGED = new DynamicCommandExceptionType(
        count -> Component.translatable("commands.createreadyforwar.reforge.nothing", count));

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("reforge")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("targets", EntityArgument.entities())
                .then(Commands.argument("affix", ResourceLocationArgument.id())
                    .suggests((context, builder) -> SharedSuggestionProvider.suggest(affixNames(context.getSource()), builder))
                    .executes(context -> reforge(context, 1, -1))
                    .then(Commands.argument("tier", IntegerArgumentType.integer(1, ForgeTier.values().length))
                        .executes(context -> reforge(context, IntegerArgumentType.getInteger(context, "tier"), -1))
                        .then(Commands.argument("slot", IntegerArgumentType.integer(1, 3))
                            .executes(context -> reforge(context, IntegerArgumentType.getInteger(context, "tier"),
                                IntegerArgumentType.getInteger(context, "slot") - 1)))))));
    }

    /** Affixes of this mod are suggested by their short name, the others with their namespace. */
    private static List<String> affixNames(CommandSourceStack source) {
        List<String> names = new ArrayList<>();
        for (ResourceLocation id : source.registryAccess().registryOrThrow(Affix.REGISTRY_KEY).keySet())
            names.add(id.getNamespace().equals(Createreadyforwar.MODID) ? id.getPath() : id.toString());
        return names;
    }

    private static int reforge(CommandContext<CommandSourceStack> context, int tierLevel, int requestedSlot)
        throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        Collection<? extends Entity> targets = EntityArgument.getEntities(context, "targets");

        Registry<Affix> registry = source.registryAccess().registryOrThrow(Affix.REGISTRY_KEY);
        ResourceLocation id = ResourceLocationArgument.getId(context, "affix");
        Affix affix = registry.get(id);
        if (affix == null && id.getNamespace().equals("minecraft")) {
            id = ResourceLocation.fromNamespaceAndPath(Createreadyforwar.MODID, id.getPath());
            affix = registry.get(id);
        }
        if (affix == null)
            throw UNKNOWN_AFFIX.create(id.toString());

        ForgeTier tier = ForgeTier.values()[tierLevel - 1];
        int reforged = 0;
        CommandSyntaxException lastFailure = null;

        for (Entity target : targets) {
            if (!(target instanceof LivingEntity living))
                continue;
            ItemStack stack = living.getMainHandItem();
            Component name = target.getDisplayName();
            if (stack.isEmpty()) {
                lastFailure = NO_ITEM.create(name);
                continue;
            }
            if (!ReforgeCategory.isReforgeable(stack)) {
                lastFailure = NOT_REFORGEABLE.create(stack.getHoverName());
                continue;
            }
            if (!AffixForge.fits(stack, affix)) {
                lastFailure = INCOMPATIBLE.create(stack.getHoverName());
                continue;
            }

            int slot = requestedSlot >= 0 ? requestedSlot : Math.max(0, AffixForge.firstEmptySlot(stack));
            AffixForge.apply(stack, id, affix, tier, slot, living.getRandom());
            reforged++;

            Component affixName = Component.translatable(Affix.nameKey(id));
            Component tierName = Component.translatable(tier.translationKey()).withStyle(tier.color());
            if (targets.size() == 1)
                source.sendSuccess(() -> Component.translatable("commands.createreadyforwar.reforge.success.single",
                    affixName, tierName, slot + 1, stack.getDisplayName(), name), true);
        }

        if (reforged == 0) {
            if (targets.size() == 1 && lastFailure != null)
                throw lastFailure;
            throw NOTHING_REFORGED.create(targets.size());
        }
        if (targets.size() > 1) {
            int count = reforged;
            source.sendSuccess(() -> Component.translatable("commands.createreadyforwar.reforge.success.multiple", count), true);
        }
        return reforged;
    }
}
