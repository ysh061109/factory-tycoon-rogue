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

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fillGradient(0, 0, this.width, this.height, 0xC0101018, 0xC0080810);
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

        // Body and bezel.
        roundRect(g, -2, -2, W + 2, H + 2, 0xFF3A3F4C);
        roundRect(g, 0, 0, W, H, 0xFF0A0B10);
        int sx = 4;
        int sy = 4;
        int sw = W - 8;
        int sh = H - 8;
        roundRect(g, sx, sy, sx + sw, sy + sh, newsTab ? 0xFF13100F : 0xFF0E1118);
        // Notch.
        roundRect(g, W / 2 - 18, 6, W / 2 + 18, 12, 0xFF000000);

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
        pose.translate(0, 0, 0);
        pose.scale(0.75F, 0.75F, 1.0F);
        LocalTime now = LocalTime.now();
        String time = String.format("%02d:%02d", now.getHour(), now.getMinute());
        g.drawString(font, time, (int) (12 / 0.75F), (int) (6 / 0.75F), 0xFFFFFFFF, false);
        pose.popPose();
        // Signal bars and battery on the right.
        for (int i = 0; i < 4; i++) {
            g.fill(W - 44 + i * 3, 12 - i, W - 42 + i * 3, 13, 0xFFFFFFFF);
        }
        roundRect(g, W - 28, 7, W - 12, 13, 0xFFCCCCCC);
        g.fill(W - 27, 8, W - 13, 12, 0xFF101010);
        g.fill(W - 27, 8, W - 18, 12, 0xFF52C26A);
        g.fill(W - 11, 9, W - 10, 11, 0xFFCCCCCC);
    }

    private void drawHeader(GuiGraphics g, Font font) {
        int top = 18;
        // Gradient banner.
        int c0 = newsTab ? 0xFFC83A32 : 0xFF3E6BEA;
        int c1 = newsTab ? 0xFF7A1F2B : 0xFF7A4BE0;
        g.fillGradient(4, top, W - 4, top + 34, c0, c1);
        Component title = Component.translatable(newsTab ? "screen.tycoonrogue.phone.news_title"
                : "screen.tycoonrogue.phone.sns_title").withStyle(ChatFormatting.BOLD);
        g.drawString(font, title, PAD + 4, top + 5, 0xFFFFFFFF, false);

        String avg = payload.averageRating() <= 0 ? "-" : String.format("%.1f", payload.averageRating());
        var pose = g.pose();
        pose.pushPose();
        pose.translate(PAD + 4, top + 18, 0);
        pose.scale(0.8F, 0.8F, 1.0F);
        g.drawString(font, Component.literal("★ " + avg + "   ")
                .append(Component.translatable("screen.tycoonrogue.phone.rep", payload.reputation()))
                .append("   ")
                .append(Component.translatable("screen.tycoonrogue.phone.rumor", payload.rumor())),
                0, 0, 0xFFFFE9A8, false);
        pose.popPose();
        if (!payload.trendJob().isEmpty()) {
            pose.pushPose();
            pose.translate(PAD + 4, top + 27, 0);
            pose.scale(0.8F, 0.8F, 1.0F);
            g.drawString(font, Component.literal("▲ ").append(Component.translatable(
                    "screen.tycoonrogue.phone.trend",
                    Component.translatable("job.tycoonrogue." + payload.trendJob()))), 0, 0, 0xFFB6FFB0, false);
            pose.popPose();
        }
    }

    private void drawFeed(GuiGraphics g, Font font) {
        List<FeedEntry> list = shown();
        int y = 58;
        int bottom = H - 30;
        if (list.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("screen.tycoonrogue.phone.empty"), W / 2, 110, 0xFF8892A4);
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
            int trackH = bottom - 58;
            int thumbH = Math.max(10, trackH / list.size());
            int thumbY = 58 + (trackH - thumbH) * scroll / Math.max(1, list.size() - 1);
            g.fill(W - 7, 58, W - 6, bottom, 0x33FFFFFF);
            g.fill(W - 7, thumbY, W - 6, thumbY + thumbH, 0xAAFFFFFF);
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
        roundRect(g, PAD, y, W - PAD - 4, y + h, 0xFF1A2030);
        // Avatar.
        int ax = PAD + 4;
        int ay = y + 5;
        roundRect(g, ax, ay, ax + 14, ay + 14, FeedText.avatarColor(e));
        g.drawCenteredString(font, FeedText.initial(e), ax + 7, ay + 3, 0xFF101418);
        var pose = g.pose();
        pose.pushPose();
        pose.translate(ax + 18, ay, 0);
        pose.scale(ts, ts, 1.0F);
        g.drawString(font, Component.literal(FeedText.user(e)).withStyle(ChatFormatting.BOLD), 0, 0, 0xFFFFFFFF, false);
        g.drawString(font, FeedText.stars(e), 0, 10, 0xFFFFD54F, false);
        pose.popPose();
        // Text.
        pose.pushPose();
        pose.translate(PAD + 5, y + 22, 0);
        pose.scale(ts, ts, 1.0F);
        for (int i = 0; i < lines.size(); i++) {
            g.drawString(font, lines.get(i), 0, i * 10, 0xFFDDE3EE, false);
        }
        pose.popPose();
        // Footer: likes and round.
        pose.pushPose();
        pose.translate(PAD + 5, y + h - 11, 0);
        pose.scale(ts, ts, 1.0F);
        g.drawString(font, "♥ " + FeedText.likes(e), 0, 0, 0xFFFF6B8A, false);
        g.drawString(font, "R" + e.round(), 120, 0, 0xFF7A8294, false);
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
        roundRect(g, PAD, y, W - PAD - 4, y + h, 0xFF221A1A);
        g.fill(PAD, y + 1, PAD + 2, y + h - 1, 0xFFE0443C);
        var pose = g.pose();
        pose.pushPose();
        pose.translate(PAD + 6, y + 4, 0);
        pose.scale(0.7F, 0.7F, 1.0F);
        g.fill(-2, -1, 40, 9, 0xFFE0443C);
        g.drawString(font, "TYCOON NEWS", 0, 0, 0xFFFFFFFF, false);
        g.drawString(font, "R" + e.round(), 100, 0, 0xFF8A7F7F, false);
        pose.popPose();
        pose.pushPose();
        pose.translate(PAD + 6, y + 15, 0);
        pose.scale(ts, ts, 1.0F);
        for (int i = 0; i < lines.size(); i++) {
            g.drawString(font, lines.get(i), 0, i * 10, 0xFFF1E6E6, false);
        }
        pose.popPose();
        return h;
    }

    private void drawTabBar(GuiGraphics g, Font font) {
        int top = H - 26;
        g.fill(4, top, W - 4, H - 4, 0xFF0A0C12);
        g.fill(4, top, W - 4, top + 1, 0x33FFFFFF);
        for (int i = 0; i < 2; i++) {
            boolean active = (i == 1) == newsTab;
            int cx = i == 0 ? W / 4 : W * 3 / 4;
            Component label = Component.translatable(i == 0 ? "screen.tycoonrogue.phone.sns" : "screen.tycoonrogue.phone.news");
            int color = active ? (i == 0 ? 0xFF8EA8FF : 0xFFFF8A80) : 0xFF6E7688;
            g.drawCenteredString(font, label, cx, top + 5, color);
            if (active) {
                g.fill(cx - 12, top + 15, cx + 12, top + 17, color);
            }
        }
        // Home indicator.
        roundRect(g, W / 2 - 14, H - 8, W / 2 + 14, H - 6, 0xFFEEEEEE);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float s = scale();
        double lx = (mouseX - left(s)) / s;
        double ly = (mouseY - top(s)) / s;
        if (button == 0 && ly >= H - 26 && ly <= H - 4 && lx >= 4 && lx <= W - 4) {
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
