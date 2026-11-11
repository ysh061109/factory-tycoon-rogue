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

/** Phone-style notification banners that slide in at the top right when a post or headline arrives. */
public final class ToastOverlay {
    private static final int WIDTH = 176;
    private static final long SLIDE_MS = 320L;
    private static final long SHOW_MS = 5200L;
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
        if (entry.isNews()) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL.value(), 1.3F, 0.7F));
        } else {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), 1.9F, 0.7F));
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_CHIME.value(), 2.0F, 0.5F));
        }
    }

    public static void render(GuiGraphics g, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (TOASTS.isEmpty() || mc.player == null || mc.options.hideGui) {
            return;
        }
        long now = Util.getMillis();
        TOASTS.removeIf(t -> now - t.start() > SHOW_MS + SLIDE_MS);
        Font font = mc.font;
        int y = 8;
        int shown = 0;
        for (Toast toast : TOASTS) {
            if (shown++ >= MAX) {
                break;
            }
            y += draw(g, font, toast, now, y) + 4;
        }
    }

    private static int draw(GuiGraphics g, Font font, Toast toast, long now, int y) {
        FeedEntry e = toast.entry();
        long age = now - toast.start();
        float in = Math.min(1.0F, age / (float) SLIDE_MS);
        float out = age > SHOW_MS ? Math.min(1.0F, (age - SHOW_MS) / (float) SLIDE_MS) : 0.0F;
        float slide = (1.0F - (1.0F - in) * (1.0F - in)) * (1.0F - out * out);
        int w = g.guiWidth();
        int x = w - 6 - (int) (WIDTH * slide);

        List<FormattedCharSequence> body = font.split(FeedText.text(e), WIDTH - 36);
        int lines = Math.min(2, body.size());
        int h = 24 + lines * 9 + 4;

        // Card: dark glass, one pixel lighter edge, rounded corners by clipping them.
        g.fill(x + 1, y, x + WIDTH - 1, y + h, 0xEE141822);
        g.fill(x, y + 1, x + WIDTH, y + h - 1, 0xEE141822);
        g.fill(x + 1, y, x + WIDTH - 1, y + 1, 0x33FFFFFF);

        // App icon.
        int iconColor = e.isNews() ? 0xFFE0443C : 0xFF5B6CFF;
        g.fill(x + 6, y + 6, x + 24, y + 24, iconColor);
        g.fill(x + 6, y + 6, x + 24, y + 7, 0x55FFFFFF);
        g.drawCenteredString(font, e.isNews() ? "N" : "@", x + 15, y + 11, 0xFFFFFFFF);

        // App name and time.
        Component app = Component.translatable(e.isNews() ? "screen.tycoonrogue.phone.news" : "screen.tycoonrogue.phone.sns");
        g.drawString(font, app, x + 30, y + 5, 0xFF9AA3B2, false);
        Component now2 = Component.translatable("screen.tycoonrogue.phone.now");
        g.drawString(font, now2, x + WIDTH - 6 - font.width(now2), y + 5, 0xFF7A8294, false);

        // Title line.
        String title = e.isNews() ? "TYCOON NEWS" : FeedText.user(e) + "  " + "★".repeat(Math.max(0, e.stars()));
        g.drawString(font, title, x + 30, y + 15, e.isNews() ? 0xFFFF8A80 : 0xFFFFFFFF, false);

        for (int i = 0; i < lines; i++) {
            g.drawString(font, body.get(i), x + 30, y + 26 + i * 9, 0xFFD6DCE8, false);
        }
        return h;
    }
}
