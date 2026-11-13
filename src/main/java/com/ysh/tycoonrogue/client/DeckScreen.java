package com.ysh.tycoonrogue.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.ysh.tycoonrogue.TycoonRogue;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Set;

/** The deck: every card you own this run, laid out as small cards. Hover one to read it. Open with K. */
public class DeckScreen extends Screen {
    private static final int CW = 116;
    private static final int CH = 168;
    // Rarity is data-driven on the server; the client only needs it to pick a frame.
    private static final Set<String> LEGENDARY = Set.of("tycoon_magnate", "fort_knox", "celebrity_visit", "grand_expo");
    private static final Set<String> RARE = Set.of("viral_post", "security_guard", "vip_lounge", "coffee_craze",
            "auto_show", "rush_hour", "tour_guide", "caltrop_net");
    private final long opened = Util.getMillis();

    public DeckScreen() {
        super(Component.translatable("screen.tycoonrogue.deck"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static String rarityOf(ResourceLocation id) {
        return LEGENDARY.contains(id.getPath()) ? "legendary" : RARE.contains(id.getPath()) ? "rare" : "common";
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        g.fillGradient(0, 0, this.width, this.height, 0xD0120A20, 0xD0060A14);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g, mx, my, pt);
        List<ResourceLocation> cards = ClientRunState.cards;
        g.drawCenteredString(font, Component.literal("✦ ").append(title.copy().withStyle(ChatFormatting.BOLD))
                .append(" ✦  " + cards.size()), width / 2, 10, 0xFFFFE08A);
        if (cards.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("screen.tycoonrogue.deck.empty"), width / 2, height / 2, 0xFF9AA3B2);
            return;
        }
        float s = Math.min(0.62F, (width - 20) / (float) (Math.max(1, Math.min(cards.size(), 8)) * (CW + 6)));
        int perRow = Math.max(1, (int) ((width - 20) / ((CW + 6) * s)));
        int rows = (cards.size() + perRow - 1) / perRow;
        s = Math.min(s, (height - 50) / (rows * (CH + 6.0F)));
        perRow = Math.max(1, (int) ((width - 20) / ((CW + 6) * s)));
        float totalW = Math.min(cards.size(), perRow) * (CW + 6) * s - 6 * s;
        float x0 = (width - totalW) / 2.0F;
        ResourceLocation hovered = null;
        for (int i = 0; i < cards.size(); i++) {
            ResourceLocation id = cards.get(i);
            int col = i % perRow;
            int row = i / perRow;
            float appear = Math.min(1.0F, (Util.getMillis() - opened - i * 40L) / 300.0F);
            float x = x0 + col * (CW + 6) * s;
            float y = 30 + row * (CH + 6) * s + (1.0F - appear) * 20.0F;
            boolean over = mx >= x && mx < x + CW * s && my >= y && my < y + CH * s;
            if (over) {
                hovered = id;
            }
            float grow = over ? 1.08F : 1.0F;
            var pose = g.pose();
            pose.pushPose();
            pose.translate(x - (grow - 1) * CW * s / 2, y - (grow - 1) * CH * s / 2 - (over ? 4 : 0), 0);
            pose.scale(s * grow, s * grow, 1.0F);
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(1, 1, 1, appear);
            g.blit(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "textures/gui/card/art/" + id.getPath() + ".png"),
                    12, 26, 0, 0, 92, 58, 92, 58);
            g.blit(ResourceLocation.fromNamespaceAndPath(TycoonRogue.MODID,
                    "textures/gui/card/frame_" + rarityOf(id) + ".png"), 0, 0, 0, 0, CW, CH, CW, CH);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            Component name = Component.translatable("card." + id.getNamespace() + "." + id.getPath() + ".name")
                    .withStyle(ChatFormatting.BOLD);
            var lines = font.split(name, CW - 28);
            for (int k = 0; k < Math.min(2, lines.size()); k++) {
                g.drawCenteredString(font, lines.get(k), CW / 2, 92 + k * 9, 0xFFFFF2C8);
            }
            pose.popPose();
        }
        if (hovered != null) {
            g.renderComponentTooltip(font, List.of(
                    Component.translatable("card." + hovered.getNamespace() + "." + hovered.getPath() + ".name")
                            .withStyle(ChatFormatting.GOLD),
                    Component.translatable("card." + hovered.getNamespace() + "." + hovered.getPath() + ".desc")
                            .withStyle(ChatFormatting.GRAY)), mx, my);
        }
    }
}
