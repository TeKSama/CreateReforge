package com.tek_sama.createreadyforwar.client;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

import org.joml.Matrix4f;

import com.tek_sama.createreadyforwar.Createreadyforwar;
import com.tek_sama.createreadyforwar.affix.Affix;
import com.tek_sama.createreadyforwar.affix.AppliedAffix;
import com.tek_sama.createreadyforwar.affix.ForgedAffixes;
import com.tek_sama.createreadyforwar.forge.ForgeTier;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * The affix panel shown in a forged item's tooltip: one row per affix slot, each with a hotbar-style
 * slot frame holding the affix icon, then its name and value (in the tier's colour) over its
 * description. Empty slots show an empty frame.
 *
 * Icons are looked up at {@code <namespace>:textures/affix/<affix path>.png} (16x16); an affix
 * without a texture gets {@code createreadyforwar:textures/gui/affix_placeholder.png}.
 */
public class ClientAffixTooltip implements ClientTooltipComponent {

    private static final ResourceLocation HOTBAR = ResourceLocation.withDefaultNamespace("textures/gui/sprites/hud/hotbar.png");
    private static final ResourceLocation PLACEHOLDER =
        ResourceLocation.fromNamespaceAndPath(Createreadyforwar.MODID, "textures/gui/affix_placeholder.png");

    // One slot of the vanilla hotbar sprite (182x22): left border, interior, right divider.
    private static final int FRAME_U = 0;
    private static final int FRAME_W = 21;
    private static final int FRAME_H = 22;
    private static final int HOTBAR_W = 182;
    private static final int ICON_SIZE = 16;
    private static final int ICON_OFFSET = 3;
    private static final int ROW_HEIGHT = 24;
    private static final int TEXT_X = 26;

    private static final DecimalFormat FLAT = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.ROOT));
    private static final DecimalFormat PERCENT = new DecimalFormat("0.#", DecimalFormatSymbols.getInstance(Locale.ROOT));

    private static final Map<ResourceLocation, ResourceLocation> ICONS = new HashMap<>();

    private record Row(@Nullable AppliedAffix affix, Component title, @Nullable Component description) {}

    private final List<Row> rows = new ArrayList<>();

    public ClientAffixTooltip(AffixTooltipData data) {
        ForgedAffixes forged = data.affixes();
        for (int slot = 0; slot < ForgedAffixes.SLOTS; slot++) {
            AppliedAffix applied = forged.get(slot);
            if (applied == null) {
                rows.add(new Row(null,
                    Component.translatable("createreadyforwar.tooltip.empty_slot").withStyle(ChatFormatting.DARK_GRAY), null));
                continue;
            }
            ChatFormatting tierColor = ForgeTier.values()[applied.tier() - 1].color();
            Component title = Component.translatable(Affix.nameKey(applied.affix())).withStyle(tierColor)
                .append(Component.literal(" " + formatValue(applied)).withStyle(ChatFormatting.WHITE));
            Component description = Component.translatable(Affix.descriptionKey(applied.affix()))
                .withStyle(ChatFormatting.GRAY);
            rows.add(new Row(applied, title, description));
        }
    }

    /** "+4.3%" for percentage affixes, "+1.85" for flat ones, "-12%" for reductions. */
    private static String formatValue(AppliedAffix applied) {
        String sign = applied.value() >= 0 ? "+" : "";
        if (applied.percent())
            return sign + PERCENT.format(applied.value() * 100) + "%";
        return sign + FLAT.format(applied.value());
    }

    private static ResourceLocation iconFor(ResourceLocation affixId) {
        return ICONS.computeIfAbsent(affixId, id -> {
            ResourceLocation icon = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "textures/affix/" + id.getPath() + ".png");
            return Minecraft.getInstance().getResourceManager().getResource(icon).isPresent() ? icon : PLACEHOLDER;
        });
    }

    /** Forgets which affixes have an icon; called when resource packs reload. */
    public static void clearIconCache() {
        ICONS.clear();
    }

    @Override
    public int getHeight() {
        return rows.size() * ROW_HEIGHT;
    }

    @Override
    public int getWidth(Font font) {
        int width = 0;
        for (Row row : rows) {
            width = Math.max(width, font.width(row.title()));
            if (row.description() != null)
                width = Math.max(width, font.width(row.description()));
        }
        return TEXT_X + width;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        for (int i = 0; i < rows.size(); i++) {
            int rowY = y + i * ROW_HEIGHT;
            graphics.blit(HOTBAR, x, rowY, (float) FRAME_U, 0f, FRAME_W, FRAME_H, HOTBAR_W, FRAME_H);
            AppliedAffix affix = rows.get(i).affix();
            if (affix != null)
                graphics.blit(iconFor(affix.affix()), x + ICON_OFFSET, rowY + ICON_OFFSET, 0f, 0f,
                    ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        }
    }

    @Override
    public void renderText(Font font, int x, int y, Matrix4f matrix, MultiBufferSource.BufferSource buffer) {
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            int rowY = y + i * ROW_HEIGHT;
            if (row.description() == null) {
                font.drawInBatch(row.title(), x + TEXT_X, rowY + 7, -1, true, matrix, buffer,
                    Font.DisplayMode.NORMAL, 0, 15728880);
                continue;
            }
            font.drawInBatch(row.title(), x + TEXT_X, rowY + 3, -1, true, matrix, buffer,
                Font.DisplayMode.NORMAL, 0, 15728880);
            font.drawInBatch(row.description(), x + TEXT_X, rowY + 12, -1, true, matrix, buffer,
                Font.DisplayMode.NORMAL, 0, 15728880);
        }
    }
}
