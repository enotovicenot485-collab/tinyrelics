package dev.tinyrelics.client;

import dev.tinyrelics.TinyRelics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Client-only visuals. Never touched on a dedicated server. */
@EventBusSubscriber(modid = TinyRelics.MODID, value = Dist.CLIENT)
public final class ClientEffects {
    /** Ticks left until the charm shatters; -1 when nothing is playing. */
    private static int shatterTimer = -1;

    private ClientEffects() {
    }

    /** The charm floats up in front of the screen, like a totem of undying. */
    public static void charmBroken() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        mc.gameRenderer.displayItemActivation(new ItemStack(TinyRelics.KEEPING_CHARM.get()));
        mc.particleEngine.createTrackingEmitter(player, ParticleTypes.TOTEM_OF_UNDYING, 30);
        mc.level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.6F, 1.2F, false);
        shatterTimer = 36;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (shatterTimer < 0) {
            return;
        }
        if (--shatterTimer >= 0) {
            return;
        }
        shatter();
    }

    /** At the end of the animation the charm breaks into pieces in front of the camera. */
    private static void shatter() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        ItemStack stack = new ItemStack(TinyRelics.KEEPING_CHARM.get());
        ItemParticleOption crumbs = new ItemParticleOption(ParticleTypes.ITEM, stack);
        RandomSource random = player.getRandom();
        Vec3 centre = player.getEyePosition().add(player.getLookAngle().scale(0.7D));
        for (int i = 0; i < 30; i++) {
            mc.level.addParticle(crumbs,
                    centre.x + (random.nextDouble() - 0.5D) * 0.3D,
                    centre.y + (random.nextDouble() - 0.5D) * 0.3D,
                    centre.z + (random.nextDouble() - 0.5D) * 0.3D,
                    (random.nextDouble() - 0.5D) * 0.2D,
                    random.nextDouble() * 0.15D,
                    (random.nextDouble() - 0.5D) * 0.2D);
        }
        mc.level.playLocalSound(player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1.0F, 0.9F, false);
    }
}
