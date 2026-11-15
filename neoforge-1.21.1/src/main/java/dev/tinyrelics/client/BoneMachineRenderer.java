package dev.tinyrelics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.tinyrelics.BoneMachineBlock;
import dev.tinyrelics.BoneMachineBlockEntity;
import dev.tinyrelics.TinyRelics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The two rollers in the crusher's hopper. They are not part of the block's model, because a model can not
 * turn: each is the model of the crusher_roller block, drawn here and turned while the crusher works.
 */
public class BoneMachineRenderer implements BlockEntityRenderer<BoneMachineBlockEntity> {
    /** Where the rollers' axles are, in sixteenths of a block: across the hopper, and how high. */
    private static final float[] ACROSS = {5.5F, 10.5F};
    private static final float HEIGHT = 12.75F;
    /** Degrees a tick. */
    private static final float SPEED = 14.0F;

    public BoneMachineRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(BoneMachineBlockEntity machine, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = machine.getBlockState();
        Level level = machine.getLevel();
        if (level == null || !state.is(TinyRelics.BONE_CRUSHER.get())) {
            return;
        }
        float angle = state.getValue(BoneMachineBlock.LIT) ? (level.getGameTime() % 3600L + partialTick) * SPEED : 0.0F;
        // the hopper is open to the sky above it, the block itself is not
        int lit = LevelRenderer.getLightColor(level, machine.getBlockPos().above());
        BlockState roller = TinyRelics.CRUSHER_ROLLER.get().defaultBlockState();
        pose.pushPose();
        if (state.getValue(BoneMachineBlock.FACING).getAxis() == Direction.Axis.X) {
            pose.translate(0.5F, 0.0F, 0.5F);
            pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            pose.translate(-0.5F, 0.0F, -0.5F);
        }
        for (int i = 0; i < 2; i++) {
            pose.pushPose();
            // the model lies along z through the middle of its block: move that axle to its place and turn about it
            pose.translate(ACROSS[i] / 16.0F, HEIGHT / 16.0F, 0.0F);
            pose.mulPose(Axis.ZP.rotationDegrees(i == 0 ? -angle : angle));
            pose.translate(-0.5F, -0.5F, 0.0F);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(roller, pose, buffers, lit, overlay);
            pose.popPose();
        }
        pose.popPose();
    }
}
