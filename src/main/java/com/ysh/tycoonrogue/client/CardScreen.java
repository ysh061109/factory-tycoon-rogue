package com.ysh.tycoonrogue.client;

import com.ysh.tycoonrogue.data.CardRarity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;
import com.ysh.tycoonrogue.net.PickCardPayload;

import java.util.List;

/** Shown at the end of a successful round: pick one of the offered cards. */
public class CardScreen extends Screen {
    private static final int CARD_WIDTH = 116;
    private static final int CARD_HEIGHT = 168;
    private static final int GAP = 14;

    private final int round;
    private final List<ResourceLocation> cards;
    private final List<Integer> rarities;

    public CardScreen(int round, List<ResourceLocation> cards, List<Integer> rarities) {
        super(Component.translatable("screen.tycoonrogue.cards.title", round));
        this.round = round;
        this.cards = cards;
        this.rarities = rarities;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** A plain dim backdrop (no blur, so it is safe whether the game calls this once or twice). */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xB0080A10);
    }

    private int cardX(int index) {
        int total = cards.size() * CARD_WIDTH + (cards.size() - 1) * GAP;
        return (this.width - total) / 2 + index * (CARD_WIDTH + GAP);
    }

    private int cardY() {
        return (this.height - CARD_HEIGHT) / 2 + 10;
    }

    private int cardAt(double mouseX, double mouseY) {
        for (int i = 0; i < cards.size(); i++) {
            int x = cardX(i);
            int y = cardY();
            if (mouseX >= x && mouseX < x + CARD_WIDTH && mouseY >= y && mouseY < y + CARD_HEIGHT) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        Font font = this.font;
        graphics.drawCenteredString(font, this.title.copy().withStyle(ChatFormatting.BOLD),
                this.width / 2, cardY() - 34, 0xFFFFE08A);
        graphics.drawCenteredString(font, Component.translatable("screen.tycoonrogue.cards.hint"),
                this.width / 2, cardY() - 20, 0xFFB8BEC8);

        int hovered = cardAt(mouseX, mouseY);
        for (int i = 0; i < cards.size(); i++) {
            drawCard(graphics, font, i, i == hovered);
        }
    }

    private void drawCard(GuiGraphics graphics, Font font, int index, boolean hovered) {
        ResourceLocation id = cards.get(index);
        CardRarity rarity = CardRarity.byIndex(rarities.get(index));
        int x = cardX(index);
        int y = cardY() - (hovered ? 6 : 0);
        int border = hovered ? 0xFFFFFFFF : rarity.color();

        // Glow, border, body.
        if (hovered) {
            graphics.fill(x - 3, y - 3, x + CARD_WIDTH + 3, y + CARD_HEIGHT + 3, withAlpha(rarity.color(), 0x66));
        }
        graphics.fill(x - 2, y - 2, x + CARD_WIDTH + 2, y + CARD_HEIGHT + 2, border);
        graphics.fill(x, y, x + CARD_WIDTH, y + CARD_HEIGHT, 0xFF161A22);

        // Banner with the rarity.
        graphics.fill(x, y, x + CARD_WIDTH, y + 20, withAlpha(rarity.color(), 0xCC));
        graphics.drawCenteredString(font, Component.translatable(rarity.translationKey()),
                x + CARD_WIDTH / 2, y + 6, 0xFF101418);

        // Big symbol in the middle of the art area.
        graphics.fill(x + 8, y + 26, x + CARD_WIDTH - 8, y + 66, 0xFF1F2530);
        graphics.drawCenteredString(font, Component.literal("★".repeat(rarity.ordinal() + 1)),
                x + CARD_WIDTH / 2, y + 42, rarity.color());

        // Name and description.
        String base = "card." + id.getNamespace() + "." + id.getPath();
        Component name = Component.translatable(base + ".name").withStyle(ChatFormatting.BOLD);
        int nameY = y + 74;
        for (FormattedCharSequence line : font.split(name, CARD_WIDTH - 12)) {
            graphics.drawCenteredString(font, line, x + CARD_WIDTH / 2, nameY, 0xFFFFFFFF);
            nameY += 10;
        }
        int descY = nameY + 6;
        for (FormattedCharSequence line : font.split(Component.translatable(base + ".desc"), CARD_WIDTH - 14)) {
            graphics.drawCenteredString(font, line, x + CARD_WIDTH / 2, descY, 0xFFC8D0DC);
            descY += 10;
        }
    }

    private static int withAlpha(int argb, int alpha) {
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int index = cardAt(mouseX, mouseY);
        if (index >= 0 && button == 0) {
            PacketDistributor.sendToServer(new PickCardPayload(cards.get(index)));
            this.onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
