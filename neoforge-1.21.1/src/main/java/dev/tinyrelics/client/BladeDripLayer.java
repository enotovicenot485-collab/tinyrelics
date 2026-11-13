package dev.tinyrelics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.tinyrelics.BloodBladeItem;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Draws nothing. Every frame it works out where the notches of a held Blood Blade are in
 * the world, with the arm exactly as it is posed right now, so that the drops of blood
 * can fall from the blade itself. It repeats the steps the game takes to put an item in
 * a hand; the last of them are the numbers of models/item/blood_blade.json.
 */
public class BladeDripLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    /** The three notches on the lower edge of the blade, in sprite coordinates: 0..1 across, 0..1 up. */
    private static final float[][] NOTCHES = {{0.50F, 0.55F}, {0.66F, 0.72F}, {0.84F, 0.90F}};
    /** thirdperson_righthand of the item model: translation in sixteenths, rotation, scale. */
    private static final float MOVE_Y = 9.17F / 16.0F;
    private static final float MOVE_Z = -0.41F / 16.0F;
    private static final float SCALE = 1.55F;

    public BladeDripLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!(getParentModel() instanceof HumanoidModel<?> model) || entity.isSleeping() || entity.isFallFlying() || entity.isVisuallySwimming()) {
            return;
        }
        boolean main = entity.getMainHandItem().getItem() instanceof BloodBladeItem;
        if (!main && !(entity.getOffhandItem().getItem() instanceof BloodBladeItem)) {
            return;
        }
        boolean right = (entity.getMainArm() == HumanoidArm.RIGHT) == main;
        ModelPart arm = right ? model.rightArm : model.leftArm;
        float yaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        Matrix4f matrix = new Matrix4f()
                // the body: turned to where it faces, flipped the way all entity models are, set on its feet
                .rotateY((float) Math.toRadians(180.0F - yaw)).scale(-1.0F, -1.0F, 1.0F).scale(0.9375F).translate(0.0F, -1.501F, 0.0F)
                // the arm as posed this frame
                .translate(arm.x / 16.0F, arm.y / 16.0F, arm.z / 16.0F).rotateZYX(arm.zRot, arm.yRot, arm.xRot)
                // from the shoulder to the hand
                .rotateX((float) Math.toRadians(-90.0D)).rotateY((float) Math.toRadians(180.0D))
                .translate((right ? 1.0F : -1.0F) / 16.0F, 0.125F, -0.625F)
                // the item model's own placing
                .translate(0.0F, MOVE_Y, MOVE_Z).rotateXYZ(0.0F, (float) Math.toRadians(-90.0D), (float) Math.toRadians(55.0D))
                .scale(SCALE, SCALE, 1.0F).translate(-0.5F, -0.5F, -0.5F);
        double[] points = new double[10];
        for (int i = 0; i < NOTCHES.length; i++) {
            Vector3f at = matrix.transformPosition(new Vector3f(NOTCHES[i][0], NOTCHES[i][1], 0.5F));
            points[i * 3] = at.x;
            points[i * 3 + 1] = at.y - (entity.isCrouching() ? 0.125D : 0.0D);
            points[i * 3 + 2] = at.z;
        }
        points[9] = entity.level().getGameTime();
        ClientEffects.BLADE_POINTS.put(entity.getId(), points);
    }
}
