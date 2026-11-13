package com.ysh.tycoonrogue.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Top-left panel: round timer, revenue against the quota, reputation, rumor and card count. */
public final class TycoonHud {
    private static final int X = 6;
    private static final int Y = 6;
    private static final int WIDTH = 150;
    private static final int BAR_WIDTH = WIDTH - 12;

    private TycoonHud() {
    }

    public static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (!ClientRunState.active || mc.player == null || mc.options.hideGui) {
            return;
        }
        Font font = mc.font;
        boolean showRumor = ClientRunState.rumor > 0;
        int height = showRumor ? 70 : 60;
        graphics.fill(X + 1, Y, X + WIDTH - 1, Y + height, 0xB0141018);
        graphics.fill(X, Y + 1, X + WIDTH, Y + height - 1, 0xB0141018);
        graphics.fillGradient(X + 1, Y, X + WIDTH - 1, Y + 12, 0x30FFFFFF, 0x00FFFFFF);
        graphics.fill(X, Y + 2, X + 2, Y + height - 2, 0xFFE0B030);
        graphics.fill(X + 2, Y + 14, X + WIDTH - 2, Y + 15, 0x33FFFFFF);

        int seconds = ClientRunState.ticksLeft / 20;
        String time = String.format("%d:%02d", seconds / 60, seconds % 60);
        graphics.drawString(font, Component.translatable("hud.tycoonrogue.round", ClientRunState.round),
                X + 8, Y + 4, 0xFFFFFFFF, true);
        graphics.drawString(font, time, X + WIDTH - 6 - font.width(time), Y + 4, 0xFFE0E0E0, true);

        // Revenue against the quota.
        boolean reached = ClientRunState.revenue >= ClientRunState.quota;
        graphics.drawString(font, Component.translatable("hud.tycoonrogue.revenue",
                ClientRunState.revenue, ClientRunState.quota), X + 8, Y + 16, reached ? 0xFF7CFC7C : 0xFFFFD36B, true);
        bar(graphics, X + 8, Y + 27, ClientRunState.revenue, Math.max(1, ClientRunState.quota),
                reached ? 0xFF4CAF50 : 0xFFE0A030);

        // Reputation: red when low, green when high.
        int rep = ClientRunState.reputation;
        graphics.drawString(font, Component.translatable("hud.tycoonrogue.reputation", rep),
                X + 8, Y + 34, 0xFFFFFFFF, true);
        bar(graphics, X + 8, Y + 45, rep, 100, rep < 30 ? 0xFFD04040 : rep < 60 ? 0xFFE0A030 : 0xFF4CAF50);

        graphics.drawString(font, Component.translatable("hud.tycoonrogue.cards", ClientRunState.cards.size()),
                X + 8, Y + 50, 0xFFB8BEC8, true);
        if (showRumor) {
            graphics.drawString(font, Component.translatable("hud.tycoonrogue.rumor", ClientRunState.rumor),
                    X + 8, Y + 60, 0xFFFF7070, true);
        }
    }

    private static void bar(GuiGraphics graphics, int x, int y, int value, int max, int color) {
        graphics.fill(x, y, x + BAR_WIDTH, y + 4, 0xFF0C0E12);
        int filled = Math.max(0, Math.min(BAR_WIDTH, BAR_WIDTH * value / max));
        graphics.fill(x, y, x + filled, y + 4, color);
        graphics.fillGradient(x, y, x + filled, y + 2, 0x66FFFFFF, 0x00FFFFFF);
        graphics.renderOutline(x - 1, y - 1, BAR_WIDTH + 2, 6, 0xFF3A404C);
    }
}
