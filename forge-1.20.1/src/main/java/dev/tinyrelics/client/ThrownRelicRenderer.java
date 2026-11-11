package dev.tinyrelics.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.tinyrelics.ThrownRelic;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;

/** Draws a flying weapon as its own item, lying flat and spinning. */
public class ThrownRelicRenderer extends EntityRenderer<ThrownRelic> {
    private final ItemRenderer itemRenderer;

    public ThrownRelicRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(ThrownRelic entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0.0D, 0.3D, 0.0D);
        pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        pose.mulPose(Axis.ZP.rotationDegrees((entity.tickCount + partialTick) * 45.0F));
        pose.scale(1.4F, 1.4F, 1.4F);
        this.itemRenderer.renderStatic(entity.getItem(), ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
                pose, buffers, entity.level(), entity.getId());
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(ThrownRelic entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
