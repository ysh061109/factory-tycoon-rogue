package com.ysh.tycoonrogue.client;

import com.ysh.tycoonrogue.data.FeedEntry;
import com.ysh.tycoonrogue.net.FeedPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;

/** The phone screen with an SNS tab and a news tab. Newest posts first; scroll with the mouse wheel. */
public class PhoneScreen extends Screen {
    private static final int WIDTH = 190;
    private static final int HEIGHT = 220;

    private final FeedPayload payload;
    private boolean newsTab;
    private int scroll;

    public PhoneScreen(FeedPayload payload) {
        super(Component.translatable("screen.tycoonrogue.phone"));
        this.payload = payload;
    }

    @Override
    protected void init() {
        int left = (this.width - WIDTH) / 2;
        int top = (this.height - HEIGHT) / 2;
        addRenderableWidget(Button.builder(Component.translatable("screen.tycoonrogue.phone.sns"), b -> {
            newsTab = false;
            scroll = 0;
        }).bounds(left + 6, top + 26, 88, 16).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.tycoonrogue.phone.news"), b -> {
            newsTab = true;
            scroll = 0;
        }).bounds(left + 96, top + 26, 88, 16).build());
    }

    private List<FeedEntry> shown() {
        List<FeedEntry> list = new ArrayList<>();
        for (int i = payload.entries().size() - 1; i >= 0; i--) {
            FeedEntry entry = payload.entries().get(i);
            if (entry.isNews() == newsTab) {
                list.add(entry);
            }
        }
        return list;
    }

    private static Component text(FeedEntry entry) {
        Object[] args = new Object[entry.args().size()];
        for (int i = 0; i < args.length; i++) {
            String arg = entry.args().get(i);
            args[i] = arg.startsWith("$") ? Component.translatable(arg.substring(1)) : arg;
        }
        return Component.translatable(entry.key(), args);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        int left = (this.width - WIDTH) / 2;
        int top = (this.height - HEIGHT) / 2;
        g.fill(left, top, left + WIDTH, top + HEIGHT, 0xEE1B1B22);
        g.fill(left, top, left + WIDTH, top + 20, 0xFF2D6CDF);
        g.drawString(font, this.title, left + 6, top + 6, 0xFFFFFF, false);
        String avg = payload.averageRating() <= 0 ? "-" : String.format("%.1f", payload.averageRating());
        Component status = Component.translatable("screen.tycoonrogue.phone.status", avg,
                payload.reputation(), payload.rumor());
        g.drawString(font, status, left + 6, top + 46, 0xFFD54F, false);
        if (!payload.trendJob().isEmpty()) {
            g.drawString(font, Component.translatable("screen.tycoonrogue.phone.trend",
                    Component.translatable("job.tycoonrogue." + payload.trendJob())), left + 6, top + 57, 0x80E27E, false);
        }

        // Re-add the buttons on top of the background.
        this.renderables.forEach(r -> r.render(g, mouseX, mouseY, partial));

        List<FeedEntry> list = shown();
        int y = top + 70;
        int bottom = top + HEIGHT - 6;
        if (list.isEmpty()) {
            g.drawString(font, Component.translatable("screen.tycoonrogue.phone.empty"), left + 6, y, 0x9E9E9E, false);
            return;
        }
        for (int i = scroll; i < list.size(); i++) {
            FeedEntry entry = list.get(i);
            List<net.minecraft.util.FormattedCharSequence> lines = font.split(text(entry), WIDTH - 12);
            int need = lines.size() * 10 + (entry.isNews() ? 4 : 14);
            if (y + need > bottom) {
                break;
            }
            if (!entry.isNews()) {
                MutableComponent head = Component.literal("@")
                        .append(Component.translatable("job.tycoonrogue." + entry.job()))
                        .append(entry.num() + "  " + "★".repeat(entry.stars()));
                g.drawString(font, head, left + 6, y, 0x64B5F6, false);
                y += 10;
            }
            for (var line : lines) {
                g.drawString(font, line, left + 6, y, 0xEEEEEE, false);
                y += 10;
            }
            y += 4;
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        int max = Math.max(0, shown().size() - 1);
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(dy)));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
