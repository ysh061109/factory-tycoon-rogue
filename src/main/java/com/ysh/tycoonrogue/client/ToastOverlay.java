package com.ysh.tycoonrogue.client;

import com.ysh.tycoonrogue.data.FeedEntry;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * Phone notification banners. Like a real phone they drop down from the top centre of the screen
 * with a little buzz, show the app icon, app name, "now", a bold title and the message, then slide
 * back up.
 */
public final class ToastOverlay {
    private static final int WIDTH = 196;
    private static final long SLIDE_MS = 380L;
    private static final long SHOW_MS = 5000L;
    private static final int MAX = 3;

    private record Toast(FeedEntry entry, long start) {
    }

    private static final List<Toast> TOASTS = new ArrayList<>();

    private ToastOverlay() {
    }

    public static void push(FeedEntry entry) {
        TOASTS.add(new Toast(entry, Util.getMillis()));
        while (TOASTS.size() > MAX + 2) {
            TOASTS.remove(0);
        }
        Minecraft mc = Minecraft.getInstance();
        var sm = mc.getSoundManager();
        if (entry.isNews()) {
            // Two-tone news alert.
            sm.play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL.value(), 1.3F, 0.7F));
            sm.play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL.value(), 1.68F, 0.55F));
        } else {
            // Message "ding-ding" plus a soft buzz, like a phone on the table.
            sm.play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), 1.9F, 0.7F));
            sm.play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_CHIME.value(), 2.0F, 0.5F));
        }
        sm.play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 0.7F, 0.25F));
    }

    public static void render(GuiGraphics g, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (TOASTS.isEmpty() || mc.player == null || mc.options.hideGui) {
            return;
        }
        long now = Util.getMillis();
        TOASTS.removeIf(t -> now - t.start() > SHOW_MS + SLIDE_MS);
        Font font = mc.font;
        int y = 6;
        int shown = 0;
        for (Toast toast : TOASTS) {
            if (shown++ >= MAX) {
                break;
            }
            y += draw(g, font, toast, now, y) + 3;
        }
    }

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

    private static int draw(GuiGraphics g, Font font, Toast toast, long now, int baseY) {
        FeedEntry e = toast.entry();
        long age = now - toast.start();
        float in = Math.min(1.0F, age / (float) SLIDE_MS);
        float out = age > SHOW_MS ? Math.min(1.0F, (age - SHOW_MS) / (float) SLIDE_MS) : 0.0F;
        // Drop in with a small overshoot, slide back up at the end.
        float overshoot = 1.0F + 0.12F * (float) Math.sin(in * Math.PI) * (1.0F - in * 0.3F);
        float drop = (1.0F - (1.0F - in) * (1.0F - in) * (1.0F - in)) * overshoot;
        float slide = drop * (1.0F - out * out);
        // A short buzz right after arriving.
        int buzz = age > SLIDE_MS && age < SLIDE_MS + 350L ? (int) Math.round(Math.sin(age / 22.0D) * 1.6D * (1.0D - (age - SLIDE_MS) / 350.0D)) : 0;

        List<FormattedCharSequence> body = font.split(FeedText.text(e), WIDTH - 44);
        int lines = Math.min(2, body.size());
        int h = 30 + lines * 9 + 2;
        int x = (g.guiWidth() - WIDTH) / 2 + buzz;
        int y = baseY - (int) ((1.0F - slide) * (h + baseY + 6));

        // Soft shadow, then the translucent white banner with a hairline border.
        for (int k = 3; k >= 1; k--) {
            rounded(g, x - k, y - k + 2, x + WIDTH + k, y + h + k + 2, 9 + k, (10 - k * 2) << 24);
        }
        rounded(g, x, y, x + WIDTH, y + h, 9, 0xFFD5D9E2);
        rounded(g, x + 1, y + 1, x + WIDTH - 1, y + h - 1, 8, 0xF2F7F8FC);

        // App icon: a rounded square with a glossy top half and a glyph.
        int ic0 = e.isNews() ? 0xFFFF5A4F : 0xFF4F7CFF;
        int ic1 = e.isNews() ? 0xFFD02060 : 0xFF8F5CFF;
        rounded(g, x + 7, y + 7, x + 29, y + 29, 6, ic1);
        rounded(g, x + 7, y + 7, x + 29, y + 22, 6, ic0);
        g.fillGradient(x + 9, y + 8, x + 27, y + 14, 0x40FFFFFF, 0x00FFFFFF);
        if (e.isNews()) {
            g.fill(x + 12, y + 12, x + 24, y + 24, 0xFFFFFFFF);
            g.fill(x + 14, y + 14, x + 22, y + 15, ic1);
            g.fill(x + 14, y + 17, x + 22, y + 18, 0xFF9AA0AE);
            g.fill(x + 14, y + 20, x + 20, y + 21, 0xFF9AA0AE);
        } else {
            rounded(g, x + 11, y + 11, x + 25, y + 21, 3, 0xFFFFFFFF);
            g.fill(x + 14, y + 21, x + 17, y + 24, 0xFFFFFFFF);
            g.fill(x + 14, y + 15, x + 22, y + 16, ic0);
        }

        // App name (small caps style) and time on the first line.
        var pose = g.pose();
        pose.pushPose();
        pose.translate(x + 35, y + 6, 0);
        pose.scale(0.75F, 0.75F, 1.0F);
        Component app = Component.translatable(e.isNews() ? "screen.tycoonrogue.phone.news" : "screen.tycoonrogue.phone.sns");
        g.drawString(font, app.copy().withStyle(net.minecraft.ChatFormatting.BOLD), 0, 0, 0xFF6B7385, false);
        Component stamp = Component.translatable("screen.tycoonrogue.phone.now");
        g.drawString(font, stamp, (int) ((WIDTH - 41 - font.width(stamp) * 0.75F) / 0.75F), 0, 0xFF8E95A5, false);
        pose.popPose();

        // Bold title.
        String title = e.isNews() ? "TYCOON NEWS" : FeedText.user(e) + "  " + "★".repeat(Math.max(0, e.stars()));
        g.drawString(font, Component.literal(title).withStyle(net.minecraft.ChatFormatting.BOLD),
                x + 35, y + 15, 0xFF15171E, false);
        for (int i = 0; i < lines; i++) {
            g.drawString(font, body.get(i), x + 35, y + 26 + i * 9, 0xFF3A4152, false);
        }
        return (int) (h * slide);
    }
}
