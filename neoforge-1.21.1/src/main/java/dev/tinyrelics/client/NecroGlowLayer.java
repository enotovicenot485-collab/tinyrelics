package dev.tinyrelics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.tinyrelics.NecroArmorItem;
import dev.tinyrelics.TinyRelics;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/**
 * The glow of the necromancer armor: the eyes, the soul gem and every other cyan
 * detail are drawn a second time at full brightness, slowly pulsing, the way a
 * spider's eyes shine in the dark.
 */
public class NecroGlowLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final Map<EquipmentSlot, ResourceLocation> TEXTURES = new EnumMap<>(EquipmentSlot.class);

    static {
        for (EquipmentSlot slot : SLOTS) {
            TEXTURES.put(slot, ResourceLocation.fromNamespaceAndPath(TinyRelics.MODID, "textures/models/armor/necromancer_" + slot.getName() + "_glow.png"));
        }
    }

    public NecroGlowLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!(getParentModel() instanceof HumanoidModel<?> parent)) {
            return;
        }
        // Swings from almost dark to a white-hot flare about once every second and a half,
        // strong enough to see in daylight.
        float pulse = 0.55F + 0.45F * Mth.sin(ageInTicks * 0.2F);
        for (EquipmentSlot slot : SLOTS) {
            if (!(entity.getItemBySlot(slot).getItem() instanceof NecroArmorItem)) {
                continue;
            }
            HumanoidModel<LivingEntity> model = NecroArmorModels.get(slot);
            copyPose(parent, model);
            showOnly(model, slot);
            VertexConsumer consumer = buffers.getBuffer(RenderType.eyes(TEXTURES.get(slot)));
            int level = (int) (pulse * 255.0F);
            int colour = 0xFF000000 | level << 16 | level << 8 | level;
            model.renderToBuffer(pose, consumer, 0xF000F0, OverlayTexture.NO_OVERLAY, colour);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void copyPose(HumanoidModel parent, HumanoidModel model) {
        parent.copyPropertiesTo(model);
    }

    private static void showOnly(HumanoidModel<?> model, EquipmentSlot slot) {
        model.setAllVisible(false);
        switch (slot) {
            case HEAD -> {
                model.head.visible = true;
                model.hat.visible = true;
            }
            case CHEST -> {
                model.body.visible = true;
                model.rightArm.visible = true;
                model.leftArm.visible = true;
            }
            case LEGS -> {
                model.body.visible = true;
                model.rightLeg.visible = true;
                model.leftLeg.visible = true;
            }
            default -> {
                model.rightLeg.visible = true;
                model.leftLeg.visible = true;
            }
        }
    }
}
