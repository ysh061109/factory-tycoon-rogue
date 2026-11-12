package com.ysh.tycoonrogue.client;

import com.ysh.tycoonrogue.entity.Currency;
import com.ysh.tycoonrogue.net.BuyOfferPayload;
import com.ysh.tycoonrogue.shop.ShopOffer;
import com.ysh.tycoonrogue.shop.ShopOffers;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** The Supply Desk: a list of things to buy with the money your customers pay you. */
public class ShopScreen extends Screen {
    private static final int W = 270;
    private static final int ROW = 30;
    private static final int HEADER = 34;
    private static final int FOOTER = 20;

    private int balance;
    private final List<ShopOffer> offers = new ArrayList<>();
    private int scroll;

    public ShopScreen(int balance, List<String> ids) {
        super(Component.translatable("screen.tycoonrogue.shop"));
        this.balance = balance;
        for (String id : ids) {
            ShopOffer offer = ShopOffers.byId(id);
            if (offer != null) {
                offers.add(offer);
            }
        }
    }

    /** Called when the server sends a fresh balance after a purchase. */
    public void update(int balance, List<String> ids) {
        this.balance = balance;
        offers.clear();
        for (String id : ids) {
            ShopOffer offer = ShopOffers.byId(id);
            if (offer != null) {
                offers.add(offer);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int panelHeight() {
        return Math.min(this.height - 20, HEADER + FOOTER + offers.size() * ROW + 6);
    }

    private int visibleRows() {
        return Math.max(1, (panelHeight() - HEADER - FOOTER - 6) / ROW);
    }

    private int left() {
        return (this.width - W) / 2;
    }

    private int top() {
        return (this.height - panelHeight()) / 2;
    }

    private int rowAt(double mx, double my) {
        int x = left() + 6;
        int y0 = top() + HEADER;
        if (mx < x || mx >= x + W - 12 || my < y0) {
            return -1;
        }
        int row = (int) ((my - y0) / ROW);
        if (row < 0 || row >= visibleRows() || scroll + row >= offers.size()) {
            return -1;
        }
        return scroll + row;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fillGradient(0, 0, this.width, this.height, 0xC0120A20, 0xC0060A14);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);
        int l = left();
        int t = top();
        int h = panelHeight();
        // Frame: gold edge, dark violet body.
        g.fill(l - 2, t - 2, l + W + 2, t + h + 2, 0xFF0D0A12);
        g.fill(l - 1, t - 1, l + W + 1, t + h + 1, 0xFFC08A10);
        g.fill(l, t, l + W, t + h, 0xFF1B1426);
        g.fillGradient(l, t, l + W, t + 26, 0xFF3A2A5A, 0xFF241838);
        g.drawString(font, Component.translatable("screen.tycoonrogue.shop").withStyle(ChatFormatting.BOLD),
                l + 8, t + 5, 0xFFFFE08A, false);
        Component bal = Component.translatable(Currency.usesCoins() ? "screen.tycoonrogue.shop.balance_coins"
                : "screen.tycoonrogue.shop.balance", balance);
        g.drawString(font, bal, l + W - 8 - font.width(bal), t + 5, 0xFF7CFC9A, false);
        g.drawString(font, Component.translatable("screen.tycoonrogue.shop.hint"), l + 8, t + 16, 0xFF9AA3B2, false);

        int hovered = rowAt(mouseX, mouseY);
        List<Component> tooltip = null;
        int rows = Math.min(visibleRows(), offers.size() - scroll);
        for (int i = 0; i < rows; i++) {
            ShopOffer offer = offers.get(scroll + i);
            int y = t + HEADER + i * ROW;
            boolean affordable = balance >= offer.price();
            boolean over = scroll + i == hovered;
            g.fill(l + 6, y, l + W - 6, y + ROW - 3, over ? (affordable ? 0xFF2E3F36 : 0xFF3A2428) : 0xFF241C32);
            g.fill(l + 6, y, l + 8, y + ROW - 3, affordable ? 0xFF52C26A : 0xFF8A3A3A);
            ItemStack icon = offer.icon().get();
            g.renderItem(icon, l + 12, y + 5);
            g.drawString(font, Component.translatable(offer.nameKey()).withStyle(ChatFormatting.BOLD),
                    l + 34, y + 4, affordable ? 0xFFFFF2C8 : 0xFFB0A8A0, false);
            List<FormattedCharSequence> desc = font.split(Component.translatable(offer.descKey()), (int) ((W - 120) / 0.75F));
            var pose = g.pose();
            pose.pushPose();
            pose.translate(l + 34, y + 15, 0);
            pose.scale(0.75F, 0.75F, 1.0F);
            for (int k = 0; k < Math.min(2, desc.size()); k++) {
                g.drawString(font, desc.get(k), 0, k * 9, 0xFFB8C0D0, false);
            }
            pose.popPose();
            // Price with an emerald icon.
            String price = String.valueOf(offer.price());
            int px = l + W - 12 - font.width(price) - 18;
            g.renderItem(new ItemStack(Items.EMERALD), px, y + 5);
            g.drawString(font, price, px + 18, y + 9, affordable ? 0xFF7CFC9A : 0xFFFF6B6B, false);
            if (over) {
                tooltip = new ArrayList<>();
                tooltip.add(Component.translatable(offer.nameKey()).withStyle(ChatFormatting.GOLD));
                tooltip.add(Component.translatable(offer.descKey()).withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.translatable(affordable ? "screen.tycoonrogue.shop.click"
                        : "screen.tycoonrogue.shop.poor").withStyle(affordable ? ChatFormatting.GREEN : ChatFormatting.RED));
            }
        }
        if (offers.size() > visibleRows()) {
            int trackH = visibleRows() * ROW;
            int thumbH = Math.max(12, trackH * visibleRows() / offers.size());
            int thumbY = t + HEADER + (trackH - thumbH) * scroll / Math.max(1, offers.size() - visibleRows());
            g.fill(l + W - 4, t + HEADER, l + W - 3, t + HEADER + trackH, 0x33FFFFFF);
            g.fill(l + W - 4, thumbY, l + W - 3, thumbY + thumbH, 0xAAFFFFFF);
        }
        super.render(g, mouseX, mouseY, partialTick);
        if (tooltip != null) {
            g.renderComponentTooltip(font, tooltip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int row = rowAt(mouseX, mouseY);
        if (button == 0 && row >= 0) {
            ShopOffer offer = offers.get(row);
            if (balance >= offer.price()) {
                Minecraft.getInstance().getSoundManager().play(
                        SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 0.6F));
                PacketDistributor.sendToServer(new BuyOfferPayload(offer.id()));
            } else {
                Minecraft.getInstance().getSoundManager().play(
                        SimpleSoundInstance.forUI(SoundEvents.VILLAGER_NO, 1.0F, 0.6F));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        int max = Math.max(0, offers.size() - visibleRows());
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(dy)));
        return true;
    }
}
