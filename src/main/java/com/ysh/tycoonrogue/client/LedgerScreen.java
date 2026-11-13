package com.ysh.tycoonrogue.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * The ledger: an open book. Left page = this round's sales against the quota and the clock.
 * Right page = reputation gauge, rumor and the cards you hold. Open with L.
 */
public class LedgerScreen extends Screen {
    private static final int PW = 150;
    private static final int PH = 190;

    public LedgerScreen() {
        super(Component.translatable("screen.tycoonrogue.ledger"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        g.fillGradient(0, 0, width, height, 0xC0120A06, 0xC0060403);
    }

    private void page(GuiGraphics g, int x, int y, boolean left) {
        g.fillGradient(x, y, x + PW, y + PH, 0xFFF4E8C8, 0xFFE8D6AC);
        // Paper shading toward the spine.
        for (int i = 0; i < 14; i++) {
            int a = (14 - i) * 3;
            int sx = left ? x + PW - 1 - i : x + i;
            g.fill(sx, y, sx + 1, y + PH, (a << 24) | 0x5A3A10);
        }
        // Ruled lines and the red margin.
        for (int ly = y + 34; ly < y + PH - 8; ly += 10) {
            g.fill(x + 8, ly, x + PW - 8, ly + 1, 0x2290A8C8);
        }
        g.fill(left ? x + 14 : x + 8, y + 6, (left ? x + 14 : x + 8) + 1, y + PH - 6, 0x44C04040);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g, mx, my, pt);
        float s = Math.min(1.7F, Math.min((width - 20) / (PW * 2.0F + 16), (height - 20) / (PH + 16.0F)));
        var pose = g.pose();
        pose.pushPose();
        pose.translate(width / 2.0F, height / 2.0F, 0);
        pose.scale(s, s, 1);
        pose.translate(-PW, -PH / 2.0F, 0);
        // Leather cover with brass corners.
        g.fill(-8, -8, PW * 2 + 8, PH + 8, 0xFF3A1E10);
        g.fill(-6, -6, PW * 2 + 6, PH + 6, 0xFF5A301A);
        g.renderOutline(-6, -6, PW * 2 + 12, PH + 12, 0xFFE2B24A);
        for (int cx : new int[]{-8, PW * 2 - 4}) {
            for (int cy : new int[]{-8, PH - 4}) {
                g.fill(cx, cy, cx + 12, cy + 12, 0xFFE2B24A);
                g.fill(cx + 2, cy + 2, cx + 10, cy + 10, 0xFF8A5E14);
            }
        }
        page(g, 0, 0, true);
        page(g, PW, 0, false);
        g.fill(PW - 1, 0, PW + 1, PH, 0x66000000);
        // Ribbon bookmark.
        g.fill(PW + 40, PH - 2, PW + 48, PH + 14, 0xFFC03030);
        g.fill(PW + 44, PH + 10, PW + 48, PH + 14, 0xFF8A2020);

        // ---- left page: sales ----
        int lx = 20;
        g.drawString(font, Component.translatable("hud.tycoonrogue.round", ClientRunState.round)
                .withStyle(ChatFormatting.BOLD), lx, 10, 0xFF3A1E10, false);
        int seconds = ClientRunState.ticksLeft / 20;
        Component time = Component.translatable("screen.tycoonrogue.ledger.time", String.format("%d:%02d", seconds / 60, seconds % 60));
        g.drawString(font, time, lx, 22, 0xFF6A4A2A, false);
        g.drawString(font, Component.translatable("screen.tycoonrogue.ledger.sales").withStyle(ChatFormatting.BOLD),
                lx, 44, 0xFF3A1E10, false);
        int quota = Math.max(1, ClientRunState.quota);
        boolean reached = ClientRunState.revenue >= quota;
        g.drawString(font, ClientRunState.revenue + " / " + quota, lx, 58, reached ? 0xFF1E7A3A : 0xFF9A5A10, false);
        gauge(g, lx, 72, PW - lx - 12, 10, ClientRunState.revenue / (float) quota, reached ? 0xFF3CB060 : 0xFFE0A030);
        // Where the clock says you should be by now.
        float elapsed = 1.0F - ClientRunState.ticksLeft / (float) com.ysh.tycoonrogue.data.TycoonData.ROUND_TICKS;
        int markX = lx + (int) ((PW - lx - 12) * Math.max(0, Math.min(1, elapsed)));
        g.fill(markX, 69, markX + 1, 85, 0xFF2A2A2A);
        boolean onPace = ClientRunState.revenue >= quota * elapsed || reached;
        g.drawString(font, Component.translatable(onPace ? "screen.tycoonrogue.ledger.pace_ok"
                : "screen.tycoonrogue.ledger.pace_late"), lx, 92, onPace ? 0xFF1E7A3A : 0xFFA02A2A, false);
        g.drawString(font, Component.translatable("screen.tycoonrogue.ledger.left",
                Math.max(0, quota - ClientRunState.revenue)), lx, 108, 0xFF6A4A2A, false);

        // ---- right page: reputation, rumor, cards ----
        int rx = PW + 18;
        g.drawString(font, Component.translatable("screen.tycoonrogue.ledger.reputation").withStyle(ChatFormatting.BOLD),
                rx, 10, 0xFF3A1E10, false);
        int rep = Math.max(0, Math.min(100, ClientRunState.reputation));
        int gw = PW - 30;
        g.fill(rx, 26, rx + gw * 30 / 100, 36, 0xFFC05040);
        g.fill(rx + gw * 30 / 100, 26, rx + gw * 60 / 100, 36, 0xFFE0B040);
        g.fill(rx + gw * 60 / 100, 26, rx + gw, 36, 0xFF50B060);
        g.renderOutline(rx - 1, 25, gw + 2, 12, 0xFF3A1E10);
        int mxp = rx + gw * rep / 100;
        g.fill(mxp - 1, 22, mxp + 2, 40, 0xFF1A1A1A);
        g.drawCenteredString(font, String.valueOf(rep), mxp, 41 + 0, 0xFF3A1E10);
        if (ClientRunState.rumor > 0) {
            g.drawString(font, Component.translatable("hud.tycoonrogue.rumor", ClientRunState.rumor), rx, 56, 0xFFA02A2A, false);
            gauge(g, rx, 68, gw, 6, Math.min(1.0F, ClientRunState.rumor / 100.0F), 0xFFC04040);
        }
        g.drawString(font, Component.translatable("hud.tycoonrogue.cards", ClientRunState.cards.size())
                .withStyle(ChatFormatting.BOLD), rx, 86, 0xFF3A1E10, false);
        List<ResourceLocation> cards = ClientRunState.cards;
        for (int i = 0; i < Math.min(cards.size(), 12); i++) {
            ResourceLocation id = cards.get(i);
            int cx = rx + (i % 4) * 29;
            int cy = 100 + (i / 4) * 24;
            RenderSystem.enableBlend();
            g.blit(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "textures/gui/card/art/" + id.getPath() + ".png"),
                    cx, cy, 27, 17, 0, 0, 92, 58, 92, 58);
            g.renderOutline(cx - 1, cy - 1, 29, 19, 0xFF8A5E14);
        }
        pose.popPose();
    }

    private static void gauge(GuiGraphics g, int x, int y, int w, int h, float frac, int color) {
        g.fill(x, y, x + w, y + h, 0xFFC8B68A);
        g.fill(x, y, x + (int) (w * Math.max(0, Math.min(1, frac))), y + h, color);
        g.fillGradient(x, y, x + (int) (w * Math.max(0, Math.min(1, frac))), y + h / 2, 0x55FFFFFF, 0x00FFFFFF);
        g.renderOutline(x - 1, y - 1, w + 2, h + 2, 0xFF3A1E10);
    }
}
