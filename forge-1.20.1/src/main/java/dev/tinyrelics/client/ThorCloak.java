package dev.tinyrelics.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * Moves the cloak of the Storm God armor. The cloak is two hinged pieces hanging from the
 * back of the neck (see ThorArmorModels); every time the armor is about to be drawn they
 * are turned to suit what the wearer is doing: lifted behind when running or falling,
 * swaying a little when standing, kept hanging straight down when the body leans to sneak.
 */
public final class ThorCloak {
    private ThorCloak() {
    }

    public static void pose(HumanoidModel<?> model, LivingEntity entity) {
        if (!model.body.hasChild("cloak")) {
            return;
        }
        ModelPart cloak = model.body.getChild("cloak");
        float speed = Math.min(1.0F, entity.walkAnimation.speed());
        float falling = Mth.clamp((float) -entity.getDeltaMovement().y, 0.0F, 1.0F);
        float sway = Mth.sin(entity.tickCount * 0.09F) * 0.03F;
        float lift = 0.07F + speed * 0.7F + falling * 0.9F + sway;
        if (entity.isCrouching()) {
            // the body leans forward by half a radian and the cloak is hung on it: lean it back
            lift -= 0.42F;
        }
        cloak.xRot = Math.min(lift, 1.3F);
        if (cloak.hasChild("cloak_low")) {
            // the lower half trails a little more than the upper one
            cloak.getChild("cloak_low").xRot = speed * 0.28F + falling * 0.3F + sway * 1.5F;
        }
    }
}
