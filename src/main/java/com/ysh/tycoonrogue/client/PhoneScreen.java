package com.ysh.tycoonrogue.client;

import com.ysh.tycoonrogue.data.FeedEntry;
import com.ysh.tycoonrogue.net.FeedPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A smartphone. Portrait body with a notch, a status bar (clock, signal, battery), an app header
 * with the factory's rating, a feed of posts or headlines, and a tab bar at the bottom.
 * Mouse wheel scrolls, arrow keys switch tabs.
 */
public class PhoneScreen extends Screen {
    private static final int W = 150;
    private static final int H = 252;
    private static final int PAD = 6;

    private final FeedPayload payload;
    private boolean newsTab;
    private int scroll;

    public PhoneScreen(FeedPayload payload) {
        super(Component.translatable("screen.tycoonrogue.phone"));
        this.payload = payload;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private float scale() {
        return Math.max(0.7F, Math.min(1.6F, (this.height - 16) / (float) H));
    }

    private float left(float s) {
        return (this.width - W * s) / 2.0F;
    }

    private float top(float s) {
        return (this.height - H * s) / 2.0F;
    }

    /** Entries of the current tab. The server keeps the feed newest first. */
    private List<FeedEntry> shown() {
        List<FeedEntry> list = new ArrayList<>();
        for (FeedEntry entry : payload.entries()) {
            if (entry.isNews() == newsTab) {
                list.add(entry);
            }
        }
        return list;
    }

    private static void roundRect(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        g.fill(x0 + 1, y0, x1 - 1, y1, color);
        g.fill(x0, y0 + 1, x1, y1 - 1, color);
    }

    /** A rectangle with properly rounded corners of the given radius (1 to 8). */
    private static void rounded(GuiGraphics g, int x0, int y0, int x1, int y1, int r, int color) {
        r = Math.max(1, Math.min(r, Math.min((x1 - x0) / 2, (y1 - y0) / 2)));
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5D;
            int inset = (int) Math.round(r - Math.sqrt(Math.max(0.0D, r * r - dy * dy)));
            g.fill(x0 + inset, y0 + i, x1 - inset, y0 + i + 1, color);
            g.fill(x0 + inset, y1 - i - 1, x1 - inset, y1 - i, color);
        }
        g.fill(x0, y0 + r, x1, y1 - r, color);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fillGradient(0, 0, this.width, this.height, 0x8826324A, 0x88101826);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);
        Font font = this.font;
        float s = scale();
        var pose = g.pose();
        pose.pushPose();
        pose.translate(left(s), top(s), 0.0F);
        pose.scale(s, s, 1.0F);

        // Soft drop shadow, steel frame with a highlight, black bezel, then the bright screen.
        for (int k = 5; k >= 1; k--) {
            rounded(g, -2 - k, -2 - k + 3, W + 2 + k, H + 2 + k + 3, 16 + k, (14 - k * 2) << 24);
        }
        // Side buttons.
        g.fill(-4, 44, -2, 62, 0xFF6C7282);
        g.fill(-4, 70, -2, 94, 0xFF6C7282);
        g.fill(-4, 100, -2, 124, 0xFF6C7282);
        g.fill(W + 2, 62, W + 4, 100, 0xFF6C7282);
        rounded(g, -2, -2, W + 2, H + 2, 15, 0xFFB9BFCC);
        rounded(g, -1, -1, W + 1, H + 1, 14, 0xFF4A4F5C);
        rounded(g, 0, 0, W, H, 13, 0xFF07080C);
        int sx = 3;
        int sy = 3;
        int sw = W - 6;
        int sh = H - 6;
        rounded(g, sx, sy, sx + sw, sy + sh, 11, newsTab ? 0xFFFBF4F2 : 0xFFF2F4FA);
        // Glass reflection across the top of the screen.
        g.fillGradient(sx + 6, sy + 1, sx + sw - 6, sy + 3, 0x40FFFFFF, 0x00FFFFFF);
        // Dynamic island with the front camera.
        rounded(g, W / 2 - 17, 6, W / 2 + 17, 15, 4, 0xFF000000);
        g.fill(W / 2 + 9, 9, W / 2 + 13, 13, 0xFF1B2440);
        g.fill(W / 2 + 10, 10, W / 2 + 11, 11, 0xFF4F6BD0);

        drawStatusBar(g, font);
        drawHeader(g, font);
        drawFeed(g, font);
        drawTabBar(g, font);
        pose.popPose();

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawStatusBar(GuiGraphics g, Font font) {
        var pose = g.pose();
        pose.pushPose();
        pose.scale(0.8F, 0.8F, 1.0F);
        LocalTime now = LocalTime.now();
        String time = String.format("%02d:%02d", now.getHour(), now.getMinute());
        g.drawString(font, time, (int) (13 / 0.8F), (int) (6 / 0.8F), 0xFF15171E, false);
        pose.popPose();
        // Signal bars, wifi dots and battery on the right.
        for (int i = 0; i < 4; i++) {
            g.fill(W - 50 + i * 3, 13 - i * 2 - 1, W - 48 + i * 3, 14, 0xFF15171E);
        }
        rounded(g, W - 30, 7, W - 12, 14, 2, 0xFF15171E);
        rounded(g, W - 29, 8, W - 13, 13, 1, 0xFFE8EAF0);
        g.fill(W - 29, 8, W - 19, 13, 0xFF34C759);
        g.fill(W - 11, 9, W - 10, 12, 0xFF15171E);
    }

    private void drawHeader(GuiGraphics g, Font font) {
        int top = 19;
        int c0 = newsTab ? 0xFFFF5A4F : 0xFF4F7CFF;
        int c1 = newsTab ? 0xFFE0307A : 0xFF9A5CFF;
        g.fillGradient(3, top, W - 3, top + 36, c0, c1);
        // Light sheen so the banner feels glossy.
        g.fillGradient(3, top, W - 3, top + 12, 0x30FFFFFF, 0x00FFFFFF);
        Component title = Component.translatable(newsTab ? "screen.tycoonrogue.phone.news_title"
                : "screen.tycoonrogue.phone.sns_title").withStyle(ChatFormatting.BOLD);
        g.drawString(font, title, PAD + 4, top + 5, 0xFFFFFFFF, true);

        String avg = payload.averageRating() <= 0 ? "-" : String.format("%.1f", payload.averageRating());
        var pose = g.pose();
        pose.pushPose();
        pose.translate(PAD + 4, top + 18, 0);
        pose.scale(0.8F, 0.8F, 1.0F);
        g.drawString(font, Component.literal("★ " + avg + "   ")
                .append(Component.translatable("screen.tycoonrogue.phone.rep", payload.reputation()))
                .append("   ")
                .append(Component.translatable("screen.tycoonrogue.phone.rumor", payload.rumor())),
                0, 0, 0xFFFFFFFF, true);
        pose.popPose();
        if (!payload.trendJob().isEmpty()) {
            pose.pushPose();
            pose.translate(PAD + 4, top + 27, 0);
            pose.scale(0.8F, 0.8F, 1.0F);
            g.drawString(font, Component.literal("▲ ").append(Component.translatable(
                    "screen.tycoonrogue.phone.trend",
                    Component.translatable("job.tycoonrogue." + payload.trendJob()))), 0, 0, 0xFFE6FFE0, true);
            pose.popPose();
        }
    }

    private void drawFeed(GuiGraphics g, Font font) {
        List<FeedEntry> list = shown();
        int y = 60;
        int bottom = H - 32;
        if (list.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("screen.tycoonrogue.phone.empty"), W / 2, 110, 0xFF6B7385);
            return;
        }
        scroll = Math.max(0, Math.min(scroll, list.size() - 1));
        for (int i = scroll; i < list.size(); i++) {
            FeedEntry entry = list.get(i);
            int used = entry.isNews() ? drawNews(g, font, entry, y, bottom) : drawPost(g, font, entry, y, bottom);
            if (used < 0) {
                break;
            }
            y += used + 4;
        }
        // Scroll hint.
        if (list.size() > 1) {
            int trackH = bottom - 60;
            int thumbH = Math.max(10, trackH / list.size());
            int thumbY = 60 + (trackH - thumbH) * scroll / Math.max(1, list.size() - 1);
            g.fill(W - 7, 60, W - 6, bottom, 0x22000000);
            g.fill(W - 7, thumbY, W - 6, thumbY + thumbH, 0x99606878);
        }
    }

    /** Draws one post and returns its height, or -1 if it does not fit. */
    private int drawPost(GuiGraphics g, Font font, FeedEntry e, int y, int bottom) {
        float ts = 0.8F;
        List<FormattedCharSequence> lines = font.split(FeedText.text(e), (int) ((W - 2 * PAD - 12) / ts));
        int textH = (int) (lines.size() * 10 * ts);
        int h = 8 + 14 + textH + 14;
        if (y + h > bottom) {
            return -1;
        }
        rounded(g, PAD, y + 1, W - PAD - 4, y + h + 1, 5, 0x18000000);
        rounded(g, PAD, y, W - PAD - 4, y + h, 5, 0xFFFFFFFF);
        // Avatar.
        int ax = PAD + 4;
        int ay = y + 5;
        rounded(g, ax, ay, ax + 14, ay + 14, 7, FeedText.avatarColor(e));
        g.drawCenteredString(font, FeedText.initial(e), ax + 7, ay + 3, 0xFF101418);
        var pose = g.pose();
        pose.pushPose();
        pose.translate(ax + 18, ay, 0);
        pose.scale(ts, ts, 1.0F);
        g.drawString(font, Component.literal(FeedText.user(e)).withStyle(ChatFormatting.BOLD), 0, 0, 0xFF1B1F2A, false);
        g.drawString(font, FeedText.stars(e), 0, 10, 0xFFF5A300, false);
        pose.popPose();
        // Text.
        pose.pushPose();
        pose.translate(PAD + 5, y + 22, 0);
        pose.scale(ts, ts, 1.0F);
        for (int i = 0; i < lines.size(); i++) {
            g.drawString(font, lines.get(i), 0, i * 10, 0xFF2B3140, false);
        }
        pose.popPose();
        // Footer: likes and round.
        pose.pushPose();
        pose.translate(PAD + 5, y + h - 11, 0);
        pose.scale(ts, ts, 1.0F);
        g.drawString(font, "♥ " + FeedText.likes(e), 0, 0, 0xFFE8355C, false);
        g.drawString(font, "R" + e.round(), 120, 0, 0xFF8A92A3, false);
        pose.popPose();
        return h;
    }

    private int drawNews(GuiGraphics g, Font font, FeedEntry e, int y, int bottom) {
        float ts = 0.85F;
        List<FormattedCharSequence> lines = font.split(
                FeedText.text(e).copy().withStyle(ChatFormatting.BOLD), (int) ((W - 2 * PAD - 12) / ts));
        int h = 8 + 11 + (int) (lines.size() * 10 * ts) + 4;
        if (y + h > bottom) {
            return -1;
        }
        rounded(g, PAD, y + 1, W - PAD - 4, y + h + 1, 5, 0x18000000);
        rounded(g, PAD, y, W - PAD - 4, y + h, 5, 0xFFFFFFFF);
        g.fill(PAD, y + 2, PAD + 2, y + h - 2, 0xFFE0443C);
        var pose = g.pose();
        pose.pushPose();
        pose.translate(PAD + 6, y + 4, 0);
        pose.scale(0.7F, 0.7F, 1.0F);
        g.fill(-2, -1, 40, 9, 0xFFE0443C);
        g.drawString(font, "TYCOON NEWS", 0, 0, 0xFFFFFFFF, false);
        g.drawString(font, "R" + e.round(), 100, 0, 0xFF8A8F9A, false);
        pose.popPose();
        pose.pushPose();
        pose.translate(PAD + 6, y + 15, 0);
        pose.scale(ts, ts, 1.0F);
        for (int i = 0; i < lines.size(); i++) {
            g.drawString(font, lines.get(i), 0, i * 10, 0xFF22252E, false);
        }
        pose.popPose();
        return h;
    }

    private void drawTabBar(GuiGraphics g, Font font) {
        int top = H - 29;
        g.fill(3, top, W - 3, H - 14, 0xFFFFFFFF);
        g.fill(3, top, W - 3, top + 1, 0x22000000);
        for (int i = 0; i < 2; i++) {
            boolean active = (i == 1) == newsTab;
            int cx = i == 0 ? W / 4 : W * 3 / 4;
            Component label = Component.translatable(i == 0 ? "screen.tycoonrogue.phone.sns" : "screen.tycoonrogue.phone.news");
            int color = active ? (i == 0 ? 0xFF3E63E0 : 0xFFE0443C) : 0xFF9AA1B0;
            // Tiny icon: a chat bubble for the feed, a newspaper for the news.
            if (i == 0) {
                rounded(g, cx - 5, top + 3, cx + 5, top + 10, 2, color);
                g.fill(cx - 3, top + 10, cx - 1, top + 12, color);
            } else {
                g.fill(cx - 5, top + 3, cx + 5, top + 11, color);
                g.fill(cx - 3, top + 5, cx, top + 6, 0xFFFFFFFF);
                g.fill(cx - 3, top + 8, cx + 3, top + 9, 0xFFFFFFFF);
            }
            var pose = g.pose();
            pose.pushPose();
            pose.translate(cx, top + 13, 0);
            pose.scale(0.75F, 0.75F, 1.0F);
            g.drawCenteredString(font, label, 0, 0, color);
            pose.popPose();
        }
        // Home indicator on a white strip.
        g.fill(3, H - 14, W - 3, H - 3, 0xFFFFFFFF);
        rounded(g, W / 2 - 18, H - 9, W / 2 + 18, H - 7, 1, 0xFF15171E);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float s = scale();
        double lx = (mouseX - left(s)) / s;
        double ly = (mouseY - top(s)) / s;
        if (button == 0 && ly >= H - 29 && ly <= H - 12 && lx >= 4 && lx <= W - 4) {
            boolean toNews = lx > W / 2.0D;
            if (toNews != newsTab) {
                newsTab = toNews;
                scroll = 0;
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        int max = Math.max(0, shown().size() - 1);
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(dy)));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 263 || keyCode == 262) { // left / right arrow
            newsTab = keyCode == 262;
            scroll = 0;
            return true;
        }
        if (keyCode == 265 || keyCode == 264) { // up / down arrow
            mouseScrolled(0, 0, 0, keyCode == 265 ? 1 : -1);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
