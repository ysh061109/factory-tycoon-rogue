package com.ysh.tycoonrogue.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.ysh.tycoonrogue.TycoonRogue;
import com.ysh.tycoonrogue.data.CardRarity;
import com.ysh.tycoonrogue.net.PickCardPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Locale;

/**
 * Shown at the end of a successful round: pick one of the offered cards.
 *
 * Cards are drawn from textures (an ornate frame per rarity and one illustration per card),
 * with text on top. Cards slide in one after another, float and glow when hovered, and rare
 * cards shimmer.
 */
public class CardScreen extends Screen {
    private static final int CARD_WIDTH = 116;
    private static final int CARD_HEIGHT = 168;
    private static final int GAP = 16;

    private final int round;
    private final List<ResourceLocation> cards;
    private final List<Integer> rarities;
    private final float[] lift;
    private final long openedAt = Util.getMillis();
    private int lastHovered = -1;

    public CardScreen(int round, List<ResourceLocation> cards, List<Integer> rarities) {
        super(Component.translatable("screen.tycoonrogue.cards.title", round));
        this.round = round;
        this.cards = cards;
        this.rarities = rarities;
        this.lift = new float[cards.size()];
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private float scale() {
        float byHeight = (this.height - 90) / (float) CARD_HEIGHT;
        float byWidth = (this.width - 30) / (float) (cards.size() * (CARD_WIDTH + GAP));
        return Math.max(0.6F, Math.min(1.7F, Math.min(byHeight, byWidth)));
    }

    private float cardX(int index, float s) {
        float total = (cards.size() * CARD_WIDTH + (cards.size() - 1) * GAP) * s;
        return (this.width - total) / 2.0F + index * (CARD_WIDTH + GAP) * s;
    }

    private float cardY(float s) {
        return (this.height - CARD_HEIGHT * s) / 2.0F + 14;
    }

    private int cardAt(double mouseX, double mouseY) {
        float s = scale();
        for (int i = 0; i < cards.size(); i++) {
            float x = cardX(i, s);
            float y = cardY(s) - lift[i] * 8.0F * s;
            if (mouseX >= x && mouseX < x + CARD_WIDTH * s && mouseY >= y && mouseY < y + CARD_HEIGHT * s) {
                return i;
            }
        }
        return -1;
    }

    /** A dark backdrop with drifting magic dust. No blur, so it is safe to call more than once. */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fillGradient(0, 0, this.width, this.height, 0xD0120A20, 0xD0060A14);
        long t = Util.getMillis();
        for (int i = 0; i < 44; i++) {
            double seed = i * 12.9898D;
            float x = (float) (Math.abs(Math.sin(seed) * 43758.5453D) % 1.0D) * this.width;
            float speed = 0.012F + (i % 5) * 0.004F;
            float y = this.height - ((t * speed + i * 37) % (this.height + 20));
            float sway = (float) Math.sin(t / 900.0D + i) * 6.0F;
            int alpha = 40 + (int) (40 * Math.sin(t / 400.0D + i));
            int size = i % 7 == 0 ? 2 : 1;
            g.fill((int) (x + sway), (int) y, (int) (x + sway) + size, (int) y + size, (alpha << 24) | 0xFFE9A8);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g, mouseX, mouseY, partialTick);
        Font font = this.font;
        float s = scale();

        Component title = Component.literal("✦ ").append(this.title.copy().withStyle(ChatFormatting.BOLD))
                .append(" ✦");
        int titleY = (int) (cardY(s) - 38);
        g.drawCenteredString(font, title, this.width / 2, titleY, 0xFFFFE08A);
        g.drawCenteredString(font, Component.translatable("screen.tycoonrogue.cards.hint"),
                this.width / 2, titleY + 14, 0xFFB8BEC8);

        int hovered = cardAt(mouseX, mouseY);
        if (hovered != lastHovered) {
            if (hovered >= 0) {
                Minecraft.getInstance().getSoundManager().play(
                        SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 1.4F + hovered * 0.15F, 0.5F));
            }
            lastHovered = hovered;
        }
        for (int i = 0; i < cards.size(); i++) {
            lift[i] += ((i == hovered ? 1.0F : 0.0F) - lift[i]) * 0.25F;
            drawCard(g, font, i, s, mouseX, mouseY);
        }
    }

    private static float easeOut(float t) {
        float u = 1.0F - Math.max(0.0F, Math.min(1.0F, t));
        return 1.0F - u * u * u;
    }

    private void drawCard(GuiGraphics g, Font font, int index, float s, int mouseX, int mouseY) {
        ResourceLocation id = cards.get(index);
        CardRarity rarity = CardRarity.byIndex(rarities.get(index));
        String rarityName = rarity.name().toLowerCase(Locale.ROOT);
        long now = Util.getMillis();

        float appear = easeOut((now - openedAt - index * 140L) / 420.0F);
        if (appear <= 0.0F) {
            return;
        }
        float hoverLift = lift[index];
        float x = cardX(index, s);
        float y = cardY(s) - hoverLift * 8.0F * s + (1.0F - appear) * 50.0F;
        float bob = (float) Math.sin(now / 500.0D + index * 1.7D) * 1.2F * hoverLift;
        y += bob;
        int rc = rarity.color() & 0x00FFFFFF;

        // Glow behind the card: stronger when hovered, always faint for rare cards.
        float pulse = 0.75F + 0.25F * (float) Math.sin(now / 350.0D + index);
        float glowBase = hoverLift * 1.0F + (rarity == CardRarity.COMMON ? 0.0F : 0.35F);
        if (glowBase > 0.0F) {
            for (int k = 1; k <= 5; k++) {
                int a = (int) (Math.max(0, 46 - k * 8) * glowBase * pulse * appear);
                int pad = (int) (k * 2.5F * s);
                g.fill((int) x - pad, (int) y - pad, (int) (x + CARD_WIDTH * s) + pad,
                        (int) (y + CARD_HEIGHT * s) + pad, (a << 24) | rc);
            }
        }

        var pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0.0F);
        // Tilt a little toward the mouse for a "held card" feel.
        if (hoverLift > 0.05F) {
            float cx = x + CARD_WIDTH * s / 2.0F;
            float tilt = Math.max(-1.0F, Math.min(1.0F, (mouseX - cx) / (CARD_WIDTH * s))) * 3.0F * hoverLift;
            pose.translate(CARD_WIDTH * s / 2.0F, CARD_HEIGHT * s / 2.0F, 0.0F);
            pose.mulPose(Axis.ZP.rotationDegrees(tilt));
            pose.translate(-CARD_WIDTH * s / 2.0F, -CARD_HEIGHT * s / 2.0F, 0.0F);
        }
        pose.scale(s, s, 1.0F);

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, appear);
        ResourceLocation frame = ResourceLocation.fromNamespaceAndPath(TycoonRogue.MODID,
                "textures/gui/card/frame_" + rarityName + ".png");
        ResourceLocation art = ResourceLocation.fromNamespaceAndPath(id.getNamespace(),
                "textures/gui/card/art/" + id.getPath() + ".png");
        g.blit(art, 12, 26, 0, 0, 92, 58, 92, 58);
        g.blit(frame, 0, 0, 0, 0, CARD_WIDTH, CARD_HEIGHT, CARD_WIDTH, CARD_HEIGHT);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // Shimmer sweep over the art window of rare and legendary cards (and any hovered card).
        if (rarity != CardRarity.COMMON || hoverLift > 0.1F) {
            float period = rarity == CardRarity.LEGENDARY ? 2200.0F : 3400.0F;
            float pos = ((now % (long) period) / period) * 190.0F - 50.0F;
            for (int row = 0; row < 58; row += 2) {
                int x0 = Math.max(12, (int) (pos - row * 0.8F));
                int x1 = Math.min(104, (int) (pos - row * 0.8F) + 14);
                if (x1 > x0) {
                    g.fill(x0, 26 + row, x1, 28 + row, 0x38FFFFFF);
                }
            }
        }

        if (appear > 0.7F) {
            drawText(g, font, id, rarity);
        }
        pose.popPose();
    }

    private void drawText(GuiGraphics g, Font font, ResourceLocation id, CardRarity rarity) {
        // Rarity on the top plate.
        Component rarityName = Component.translatable(rarity.translationKey()).withStyle(ChatFormatting.BOLD);
        g.drawString(font, rarityName, (CARD_WIDTH - font.width(rarityName)) / 2, 9, 0xFF140E1E, false);

        String base = "card." + id.getNamespace() + "." + id.getPath();
        // Name on the ribbon.
        Component name = Component.translatable(base + ".name").withStyle(ChatFormatting.BOLD);
        List<FormattedCharSequence> nameLines = font.split(name, CARD_WIDTH - 28);
        int lines = Math.min(2, nameLines.size());
        int nameY = 90 + (18 - lines * 9) / 2;
        for (int i = 0; i < lines; i++) {
            g.drawCenteredString(font, nameLines.get(i), CARD_WIDTH / 2, nameY + i * 9, 0xFFFFF2C8);
        }

        // Description in the lower panel; shrinks a little when it is long.
        Component desc = Component.translatable(base + ".desc");
        float ds = 1.0F;
        List<FormattedCharSequence> descLines = font.split(desc, 92);
        if (descLines.size() > 4) {
            ds = 0.8F;
            descLines = font.split(desc, (int) (92 / ds));
        }
        int lineHeight = (int) (10 * ds);
        int blockHeight = Math.min(descLines.size(), 5) * lineHeight;
        int startY = 114 + Math.max(0, (46 - blockHeight) / 2);
        var pose = g.pose();
        pose.pushPose();
        pose.translate(CARD_WIDTH / 2.0F, startY, 0.0F);
        pose.scale(ds, ds, 1.0F);
        for (int i = 0; i < descLines.size() && i < 5; i++) {
            g.drawCenteredString(font, descLines.get(i), 0, i * 10, 0xFFDDE3EE);
        }
        pose.popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int index = cardAt(mouseX, mouseY);
        if (index >= 0 && button == 0) {
            CardRarity rarity = CardRarity.byIndex(rarities.get(index));
            var sounds = Minecraft.getInstance().getSoundManager();
            sounds.play(SimpleSoundInstance.forUI(rarity == CardRarity.COMMON
                    ? SoundEvents.ENCHANTMENT_TABLE_USE : SoundEvents.PLAYER_LEVELUP, 1.0F, 0.6F));
            PacketDistributor.sendToServer(new PickCardPayload(cards.get(index)));
            this.onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
