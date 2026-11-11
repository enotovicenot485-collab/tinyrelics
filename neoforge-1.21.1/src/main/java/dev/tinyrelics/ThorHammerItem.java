package dev.tinyrelics;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class ThorHammerItem extends Item {
    /** Half of the 35 block wide square around the player. */
    private static final double HALF_WIDTH = 17.5D;
    /** How far up and down the strike reaches. */
    private static final double HALF_HEIGHT = 12.0D;
    /** Damage of one lightning strike (20 = 10 hearts). */
    private static final float STRIKE_DAMAGE = 20.0F;
    private static final int MAX_TARGETS = 128;

    public ThorHammerItem(Properties properties) {
        super(properties);
    }

    public static ItemAttributeModifiers createAttributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 13.0D, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(BASE_ATTACK_SPEED_ID, -3.0D, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            AABB area = player.getBoundingBox().inflate(HALF_WIDTH, HALF_HEIGHT, HALF_WIDTH);
            List<LivingEntity> targets = server.getEntitiesOfClass(LivingEntity.class, area, e -> isTarget(e, player));
            DamageSource source = new DamageSource(
                    server.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.LIGHTNING_BOLT),
                    player);

            int count = 0;
            for (LivingEntity target : targets) {
                if (count++ >= MAX_TARGETS) {
                    break;
                }
                // The bolt is only for looks, so it never burns the ground or hits the wielder.
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(server);
                if (bolt != null) {
                    bolt.moveTo(target.getX(), target.getY(), target.getZ());
                    bolt.setVisualOnly(true);
                    server.addFreshEntity(bolt);
                }
                target.hurt(source, STRIKE_DAMAGE);
                target.igniteForSeconds(4.0F);
            }

            server.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    player.getX(), player.getY() + player.getBbHeight() * 0.6D, player.getZ(),
                    40, 0.6D, 0.6D, 0.6D, 0.25D);
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private static boolean isTarget(LivingEntity entity, Player wielder) {
        if (entity == wielder || !entity.isAlive() || entity.isSpectator()) {
            return false;
        }
        if (entity instanceof ArmorStand) {
            return false;
        }
        if (entity instanceof Player other && other.isCreative()) {
            return false;
        }
        // Do not fry the wielder's own pets.
        return !(entity instanceof TamableAnimal pet && pet.isOwnedBy(wielder));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level instanceof ServerLevel server && entity instanceof LivingEntity living
                && (selected || living.getOffhandItem() == stack) && entity.tickCount % 3 == 0) {
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    entity.getX(), entity.getY() + entity.getBbHeight() * 0.55D, entity.getZ(),
                    2, 0.35D, 0.4D, 0.35D, 0.02D);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tinyrelics.thor_hammer.tooltip").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.tinyrelics.thor_hammer.tooltip2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
