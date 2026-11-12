package com.ysh.tycoonrogue.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ysh.tycoonrogue.TycoonRogue;
import com.ysh.tycoonrogue.shop.Hanok;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * The hologram guide. Draws glowing boxes where the hanok pavilion still needs a block, in the
 * colour of the block that belongs there. Boxes disappear as the right blocks are placed; a
 * red box means something else is standing in that spot.
 */
@EventBusSubscriber(modid = TycoonRogue.MODID, value = Dist.CLIENT)
public final class ClientHologram {
    private static boolean active;
    private static BlockPos origin = BlockPos.ZERO;
    private static int rotation;
    private static boolean wasComplete;

    private ClientHologram() {
    }

    public static void set(BlockPos pos, int quarterTurns, boolean on) {
        origin = pos;
        rotation = quarterTurns;
        active = on;
        wasComplete = false;
    }

    private static BlockPos at(Hanok.Part part) {
        int[] r = Hanok.rotate(part.dx(), part.dz(), rotation);
        return origin.offset(r[0], part.dy(), r[1]);
    }

    private static float[] colour(Block block) {
        if (block == Blocks.RED_TERRACOTTA || block == Blocks.STRIPPED_CRIMSON_STEM) {
            return new float[]{1.0F, 0.35F, 0.3F};
        }
        if (block == Blocks.BLUE_TERRACOTTA) {
            return new float[]{0.35F, 0.6F, 1.0F};
        }
        if (block == Blocks.GREEN_TERRACOTTA) {
            return new float[]{0.35F, 1.0F, 0.55F};
        }
        if (block == Blocks.YELLOW_TERRACOTTA) {
            return new float[]{1.0F, 0.9F, 0.3F};
        }
        if (block == Blocks.STONE_BRICKS) {
            return new float[]{0.75F, 0.8F, 0.85F};
        }
        return new float[]{0.45F, 0.95F, 1.0F}; // roof tiles
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
        VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        double maxDist = 56.0D * 56.0D;
        for (Hanok.Part part : Hanok.parts()) {
            BlockPos pos = at(part);
            if (pos.distToCenterSqr(cam) > maxDist) {
                continue;
            }
            BlockState current = mc.level.getBlockState(pos);
            if (current.is(part.block())) {
                continue;
            }
            float[] c = current.isAir() ? colour(part.block()) : new float[]{1.0F, 0.15F, 0.15F};
            LevelRenderer.renderLineBox(pose, lines,
                    pos.getX() - cam.x + 0.02, pos.getY() - cam.y + 0.02, pos.getZ() - cam.z + 0.02,
                    pos.getX() - cam.x + 0.98, pos.getY() - cam.y + 0.98, pos.getZ() - cam.z + 0.98,
                    c[0], c[1], c[2], 0.9F);
        }
        buffers.endBatch(RenderType.lines());
    }

    /** Shows how much of the pavilion is built while the player is nearby. */
    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!active || mc.level == null || mc.player == null || mc.level.getGameTime() % 10 != 0
                || mc.player.blockPosition().distSqr(origin) > 40 * 40) {
            return;
        }
        int done = 0;
        int total = Hanok.parts().size();
        for (Hanok.Part part : Hanok.parts()) {
            if (mc.level.getBlockState(at(part)).is(part.block())) {
                done++;
            }
        }
        if (done == total) {
            if (!wasComplete) {
                wasComplete = true;
                mc.gui.setOverlayMessage(Component.translatable("message.tycoonrogue.hologram.done"), false);
            }
        } else {
            mc.gui.setOverlayMessage(Component.translatable("message.tycoonrogue.hologram.progress", done, total), false);
        }
    }
}
