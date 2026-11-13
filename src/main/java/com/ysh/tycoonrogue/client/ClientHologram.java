package com.ysh.tycoonrogue.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ysh.tycoonrogue.TycoonRogue;
import com.ysh.tycoonrogue.shop.Hanok;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The hologram guide. It builds the pavilion one layer at a time:
 * <ul>
 *   <li>the layer you should build now is drawn as bright, filled boxes in the colour of the block
 *       that belongs there, with floating labels saying which block and how many;</li>
 *   <li>layers above are shown as faint outlines so you can see the whole shape;</li>
 *   <li>a red box with a label means the wrong block is standing in that spot;</li>
 *   <li>a legend on the screen lists every material with its colour, icon and progress.</li>
 * </ul>
 */
@EventBusSubscriber(modid = TycoonRogue.MODID, value = Dist.CLIENT)
public final class ClientHologram {
    private static boolean active;
    private static BlockPos origin = BlockPos.ZERO;
    private static int rotation;
    private static boolean wasComplete;

    // Progress, refreshed twice a second.
    private static int currentLayer;
    private static int builtTotal;
    private static final Map<Block, int[]> PROGRESS = new LinkedHashMap<>(); // block -> {placed, needed}
    private static final Map<Block, Integer> MISSING_NOW = new LinkedHashMap<>();
    private static int layerCount = 1;
    private static boolean complete;

    private ClientHologram() {
    }

    public static void set(BlockPos pos, int quarterTurns, boolean on) {
        origin = pos;
        rotation = quarterTurns;
        active = on;
        wasComplete = false;
        refresh();
    }

    private static BlockPos at(Hanok.Part part) {
        int[] r = Hanok.rotate(part.dx(), part.dz(), rotation);
        return origin.offset(r[0], part.dy(), r[1]);
    }

    /** Every block gets its own colour so the legend, boxes and labels match. */
    private static int colourOf(Block block) {
        if (block == Blocks.STONE_BRICKS) {
            return 0xB8C0CC;
        }
        if (block == Blocks.STRIPPED_CRIMSON_STEM) {
            return 0xFF4A3F;
        }
        if (block == Blocks.RED_TERRACOTTA) {
            return 0xFF7A66;
        }
        if (block == Blocks.BLUE_TERRACOTTA) {
            return 0x4C8CFF;
        }
        if (block == Blocks.GREEN_TERRACOTTA) {
            return 0x3CE070;
        }
        if (block == Blocks.YELLOW_TERRACOTTA) {
            return 0xFFE040;
        }
        if (block == Blocks.WHITE_TERRACOTTA) {
            return 0xFFFFFF;
        }
        if (block == Blocks.DEEPSLATE_TILE_SLAB) {
            return 0x36D6D0;
        }
        if (block == Blocks.DEEPSLATE_TILE_STAIRS) {
            return 0xB070FF;
        }
        return 0x70F0FF;
    }

    private static float[] rgb(int c) {
        return new float[]{((c >> 16) & 255) / 255.0F, ((c >> 8) & 255) / 255.0F, (c & 255) / 255.0F};
    }

    /** Recounts what is built. Cheap enough to run twice a second. */
    private static void refresh() {
        Minecraft mc = Minecraft.getInstance();
        PROGRESS.clear();
        MISSING_NOW.clear();
        currentLayer = -1;
        builtTotal = 0;
        layerCount = 1;
        if (mc.level == null) {
            return;
        }
        for (Hanok.Part part : Hanok.parts()) {
            layerCount = Math.max(layerCount, part.dy() + 1);
            int[] p = PROGRESS.computeIfAbsent(part.block(), b -> new int[2]);
            p[1]++;
            if (mc.level.getBlockState(at(part)).is(part.block())) {
                p[0]++;
                builtTotal++;
            } else if (currentLayer < 0 || part.dy() < currentLayer) {
                currentLayer = part.dy();
            }
        }
        complete = currentLayer < 0;
        if (!complete) {
            for (Hanok.Part part : Hanok.parts()) {
                if (part.dy() == currentLayer && !mc.level.getBlockState(at(part)).is(part.block())) {
                    MISSING_NOW.merge(part.block(), 1, Integer::sum);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (!active || mc.level == null || event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer fill = buffers.getBuffer(RenderType.debugFilledBox());
        double maxDist = 64.0D * 64.0D;
        List<BlockPos> wrongSpots = new ArrayList<>();
        List<Block> wrongNeeds = new ArrayList<>();
        List<Object[]> labelNow = new ArrayList<>(); // {Block, BlockPos}
        // Pass 1: filled boxes.
        for (Hanok.Part part : Hanok.parts()) {
            BlockPos pos = at(part);
            if (pos.distToCenterSqr(cam) > maxDist) {
                continue;
            }
            BlockState current = mc.level.getBlockState(pos);
            if (current.is(part.block())) {
                continue;
            }
            boolean now = part.dy() == currentLayer;
            boolean next = part.dy() == currentLayer + 1;
            boolean wrong = !current.isAir() && !current.canBeReplaced();
            float[] c = wrong ? new float[]{1.0F, 0.12F, 0.12F} : rgb(colourOf(part.block()));
            float a = wrong ? 0.55F : now ? 0.38F : next ? 0.10F : 0.0F;
            if (a > 0.0F) {
                double pad = now ? 0.04D : 0.12D;
                LevelRenderer.addChainedFilledBoxVertices(pose, fill,
                        pos.getX() - cam.x + pad, pos.getY() - cam.y + pad, pos.getZ() - cam.z + pad,
                        pos.getX() - cam.x + 1.0D - pad, pos.getY() - cam.y + 1.0D - pad, pos.getZ() - cam.z + 1.0D - pad,
                        c[0], c[1], c[2], a);
            }
            if (wrong) {
                wrongSpots.add(pos);
                wrongNeeds.add(part.block());
            } else if (now) {
                labelNow.add(new Object[]{part.block(), pos});
            }
        }
        buffers.endBatch(RenderType.debugFilledBox());

        // Pass 2: outlines. The current layer is bright, everything above is faint.
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        for (Hanok.Part part : Hanok.parts()) {
            BlockPos pos = at(part);
            if (pos.distToCenterSqr(cam) > maxDist) {
                continue;
            }
            BlockState current = mc.level.getBlockState(pos);
            if (current.is(part.block())) {
                continue;
            }
            boolean now = part.dy() == currentLayer;
            boolean wrong = !current.isAir() && !current.canBeReplaced();
            float[] c = wrong ? new float[]{1.0F, 0.1F, 0.1F} : rgb(colourOf(part.block()));
            float a = wrong ? 1.0F : now ? 1.0F : part.dy() == currentLayer + 1 ? 0.45F : 0.16F;
            double pad = now ? 0.03D : 0.1D;
            LevelRenderer.renderLineBox(pose, lines,
                    pos.getX() - cam.x + pad, pos.getY() - cam.y + pad, pos.getZ() - cam.z + pad,
                    pos.getX() - cam.x + 1.0D - pad, pos.getY() - cam.y + 1.0D - pad, pos.getZ() - cam.z + 1.0D - pad,
                    c[0], c[1], c[2], a);
        }
        buffers.endBatch(RenderType.lines());

        // Pass 3: floating labels.
        Map<Block, List<BlockPos>> groups = new LinkedHashMap<>();
        for (Object[] entry : labelNow) {
            groups.computeIfAbsent((Block) entry[0], b -> new ArrayList<>()).add((BlockPos) entry[1]);
        }
        for (Map.Entry<Block, List<BlockPos>> group : groups.entrySet()) {
            List<BlockPos> list = group.getValue();
            Component name = group.getKey().getName();
            int col = colourOf(group.getKey());
            if (list.size() <= 6) {
                for (BlockPos p : list) {
                    label(pose, event, buffers, cam, p.getX() + 0.5D, p.getY() + 1.25D, p.getZ() + 0.5D,
                            name.copy().withStyle(ChatFormatting.BOLD), col);
                }
            } else {
                double sx = 0;
                double sz = 0;
                double top = -1.0E9D;
                for (BlockPos p : list) {
                    sx += p.getX() + 0.5D;
                    sz += p.getZ() + 0.5D;
                    top = Math.max(top, p.getY());
                }
                label(pose, event, buffers, cam, sx / list.size(), top + 1.8D, sz / list.size(),
                        Component.empty().append(name).append(" ×" + list.size()).withStyle(ChatFormatting.BOLD), col);
            }
        }
        for (int i = 0; i < wrongSpots.size(); i++) {
            BlockPos p = wrongSpots.get(i);
            label(pose, event, buffers, cam, p.getX() + 0.5D, p.getY() + 1.25D, p.getZ() + 0.5D,
                    Component.translatable("hologram.tycoonrogue.wrong", wrongNeeds.get(i).getName())
                            .withStyle(ChatFormatting.BOLD), 0xFF5555);
        }
        // A banner above the whole pavilion saying what to do right now.
        if (active && !complete) {
            label(pose, event, buffers, cam, origin.getX() + 0.5D, origin.getY() + layerCount + 3.0D,
                    origin.getZ() + 0.5D, Component.translatable("hologram.tycoonrogue.banner",
                            currentLayer + 1, layerCount, Component.translatable("hologram.tycoonrogue.layer." + currentLayer))
                            .withStyle(ChatFormatting.BOLD), 0xFFE08A);
        }
        buffers.endBatch();
    }

    /** A text label that always faces the player and shows through walls. */
    private static void label(PoseStack pose, RenderLevelStageEvent event, MultiBufferSource buffers, Vec3 cam,
                              double x, double y, double z, Component text, int rgb) {
        Minecraft mc = Minecraft.getInstance();
        double dx = x - cam.x;
        double dy = y - cam.y;
        double dz = z - cam.z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist > 48.0D) {
            return;
        }
        float size = 0.022F * (float) Math.max(1.0D, dist / 9.0D);
        pose.pushPose();
        pose.translate(dx, dy, dz);
        pose.mulPose(event.getCamera().rotation());
        pose.scale(-size, -size, size);
        Font font = mc.font;
        float w = font.width(text);
        font.drawInBatch(text, -w / 2.0F, 0.0F, 0xFF000000 | rgb, false, pose.last().pose(), buffers,
                Font.DisplayMode.SEE_THROUGH, 0xB0101018, LightTexture.FULL_BRIGHT);
        pose.popPose();
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!active || mc.level == null || mc.player == null || mc.level.getGameTime() % 10 != 0) {
            return;
        }
        refresh();
        if (mc.player.blockPosition().distSqr(origin) > 48 * 48) {
            return;
        }
        if (complete && !wasComplete) {
            wasComplete = true;
            mc.gui.setOverlayMessage(Component.translatable("message.tycoonrogue.hologram.done"), false);
        }
    }

    /** The on-screen legend: what to build now and every material with its colour and progress. */
    public static void renderLegend(GuiGraphics g, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (!active || mc.player == null || mc.options.hideGui || PROGRESS.isEmpty()
                || mc.player.blockPosition().distSqr(origin) > 90 * 90) {
            return;
        }
        Font font = mc.font;
        int x = 6;
        int y = ClientRunState.active ? 82 : 6;
        int rows = PROGRESS.size();
        int w = 168;
        int h = 40 + rows * 12 + 14;
        g.fill(x, y, x + w, y + h, 0xB0101418);
        g.fill(x, y, x + 2, y + h, 0xFF5CE0E0);
        int total = Hanok.parts().size();
        g.drawString(font, Component.translatable("hologram.tycoonrogue.title", builtTotal, total)
                .withStyle(ChatFormatting.BOLD), x + 8, y + 4, 0xFFFFFFFF, true);
        // Overall bar.
        g.fill(x + 8, y + 15, x + w - 8, y + 18, 0xFF2A2F38);
        g.fill(x + 8, y + 15, x + 8 + (w - 16) * builtTotal / Math.max(1, total), y + 18, 0xFF5CE0E0);
        if (complete) {
            g.drawString(font, Component.translatable("hologram.tycoonrogue.complete"), x + 8, y + 23, 0xFF7CFC7C, true);
        } else {
            Component step = Component.translatable("hologram.tycoonrogue.step", currentLayer + 1, layerCount,
                    Component.translatable("hologram.tycoonrogue.layer." + currentLayer));
            g.drawString(font, step, x + 8, y + 23, 0xFFFFE08A, true);
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<Block, Integer> e : MISSING_NOW.entrySet()) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(e.getKey().getName().getString()).append(" ×").append(e.getValue());
            }
            var pose = g.pose();
            pose.pushPose();
            pose.translate(x + 8, y + 33, 0);
            pose.scale(0.8F, 0.8F, 1.0F);
            g.drawString(font, "▶ " + sb, 0, 0, 0xFFE6F6FF, false);
            pose.popPose();
        }
        int ry = y + 46;
        for (Map.Entry<Block, int[]> e : PROGRESS.entrySet()) {
            int c = colourOf(e.getKey());
            int[] p = e.getValue();
            boolean full = p[0] >= p[1];
            g.fill(x + 8, ry + 1, x + 14, ry + 7, 0xFF000000 | c);
            g.renderOutline(x + 8, ry + 1, 6, 6, 0xFF000000);
            var pose = g.pose();
            pose.pushPose();
            pose.translate(x + 17, ry - 1, 0);
            pose.scale(0.7F, 0.7F, 1.0F);
            g.renderItem(new ItemStack(e.getKey().asItem()), 0, 0);
            pose.popPose();
            pose.pushPose();
            pose.translate(x + 30, ry, 0);
            pose.scale(0.8F, 0.8F, 1.0F);
            g.drawString(font, e.getKey().getName(), 0, 0, full ? 0xFF7CFC7C : 0xFFDDE3EE, false);
            pose.popPose();
            String count = (full ? "✔ " : "") + p[0] + "/" + p[1];
            g.drawString(font, count, x + w - 6 - font.width(count), ry, full ? 0xFF7CFC7C : 0xFFFFD36B, false);
            ry += 12;
        }
        var pose = g.pose();
        pose.pushPose();
        pose.translate(x + 8, ry + 2, 0);
        pose.scale(0.7F, 0.7F, 1.0F);
        g.drawString(font, Component.translatable("hologram.tycoonrogue.hint"), 0, 0, 0xFF9AA3B2, false);
        pose.popPose();
    }
}
