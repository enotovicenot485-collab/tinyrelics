package dev.tinyrelics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.tinyrelics.ShadowMachineBlock;
import dev.tinyrelics.ShadowMachineBlockEntity;
import dev.tinyrelics.TinyRelics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The parts of the shadow loom and the phantom spooler that move: the loom's shuttle and its two spools,
 * the spooler's reel and its crank. Each is the model of a block of its own (loom_shuttle, loom_spool,
 * spooler_reel, spooler_crank) that is never placed, drawn here.
 */
public class ShadowMachineRenderer implements BlockEntityRenderer<ShadowMachineBlockEntity> {
    /** The shuttle's place above the loom, in sixteenths of a block from the middle of the block: up, and towards the back. */
    private static final float SHUTTLE_UP = 7.3F;
    private static final float SHUTTLE_BACK = 0.5F;
    /** How far it runs to each side, and how many ticks there and back take. */
    private static final float SHUTTLE_RUN = 3.0F;
    private static final float SHUTTLE_TICKS = 36.0F;
    /** Degrees a tick. */
    private static final float REEL_SPEED = 9.0F;
    /** The loom's two spools: how far from the middle of the block to each side, and how far below it. */
    private static final float SPOOL_OUT = 6.0F;
    private static final float SPOOL_UP = -0.25F;

    public ShadowMachineRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ShadowMachineBlockEntity machine, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = machine.getBlockState();
        Level level = machine.getLevel();
        if (level == null || !(state.getBlock() instanceof ShadowMachineBlock)) {
            return;
        }
        boolean working = state.getValue(ShadowMachineBlock.LIT);
        float time = level.getGameTime() % 7200L + partialTick;
        int lit = LevelRenderer.getLightColor(level, machine.getBlockPos().above());
        pose.pushPose();
        // the block's model is turned to face its front; the moving part turns with it
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(-(state.getValue(ShadowMachineBlock.FACING).toYRot() + 180.0F)));
        pose.translate(-0.5F, -0.5F, -0.5F);
        float turn = working ? time * REEL_SPEED : 0.0F;
        if (state.is(TinyRelics.PHANTOM_SPOOLER.get())) {
            // the reel stands upright in the middle of the block and turns about its axle
            pose.pushPose();
            pose.translate(0.5F, 0.0F, 0.5F);
            pose.mulPose(Axis.YP.rotationDegrees(turn));
            pose.translate(-0.5F, 0.0F, -0.5F);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(TinyRelics.SPOOLER_REEL.get().defaultBlockState(), pose, buffers, lit, overlay);
            pose.popPose();
            // the crank on the side turns about the axle that runs across the block
            pose.translate(0.0F, 0.5F, 0.5F);
            pose.mulPose(Axis.XP.rotationDegrees(turn));
            pose.translate(0.0F, -0.5F, -0.5F);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(TinyRelics.SPOOLER_CRANK.get().defaultBlockState(), pose, buffers, lit, overlay);
        } else {
            // a spool of thread in the niche on each side, unwinding
            for (int side = -1; side <= 1; side += 2) {
                pose.pushPose();
                pose.translate(0.5F + side * SPOOL_OUT / 16.0F, 0.5F + SPOOL_UP / 16.0F, 0.0F);
                pose.mulPose(Axis.ZP.rotationDegrees(side * turn));
                pose.translate(-0.5F, -0.5F, 0.0F);
                Minecraft.getInstance().getBlockRenderer().renderSingleBlock(TinyRelics.LOOM_SPOOL.get().defaultBlockState(), pose, buffers,
                        LevelRenderer.getLightColor(level, machine.getBlockPos()), overlay);
                pose.popPose();
            }
            float run = working ? (time % SHUTTLE_TICKS) / SHUTTLE_TICKS : 0.0F;
            float across = run < 0.5F ? run * 2.0F : 2.0F - run * 2.0F;
            pose.translate((across * 2.0F - 1.0F) * SHUTTLE_RUN / 16.0F, SHUTTLE_UP / 16.0F, SHUTTLE_BACK / 16.0F);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(TinyRelics.LOOM_SHUTTLE.get().defaultBlockState(), pose, buffers, lit, overlay);
        }
        pose.popPose();
    }
}
