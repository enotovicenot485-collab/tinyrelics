package dev.tinyrelics;

import dev.tinyrelics.client.ChaosArmorModels;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

/** One piece of the Chaos Assassin armor. The four pieces together give 13 armor points. */
public class ChaosArmorItem extends ArmorItem {
    public ChaosArmorItem(Type type, Properties properties) {
        super(Material.INSTANCE, type, properties);
    }

    /** Frames of the shimmer that runs over the violet details. */
    private static final int FRAMES = 32;

    /**
     * Every slot has its own picture, and each picture comes in 32 frames. The game asks
     * for the texture every time it draws the armor, so answering with the frame for the
     * current moment animates it.
     */
    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        int frame = entity == null ? 0 : (entity.tickCount / 2) % FRAMES;
        return TinyRelics.MODID + ":textures/models/armor/chaos_" + slot.getName() + "_" + frame + ".png";
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                return ChaosArmorModels.get(slot);
            }
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tinyrelics.chaos_armor.tooltip").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("item.tinyrelics.chaos_armor.tooltip2", Component.keybind("key.tinyrelics.armor")).withStyle(ChatFormatting.DARK_PURPLE));
    }

    private enum Material implements ArmorMaterial {
        INSTANCE;

        @Override
        public int getDurabilityForType(Type type) {
            int base = switch (type) {
                case HELMET -> 11;
                case CHESTPLATE -> 16;
                case LEGGINGS -> 15;
                case BOOTS -> 13;
            };
            return base * 33;
        }

        @Override
        public int getDefenseForType(Type type) {
            return switch (type) {
                case HELMET -> 3;
                case CHESTPLATE -> 6;
                case LEGGINGS -> 3;
                case BOOTS -> 1;
            };
        }

        @Override
        public int getEnchantmentValue() {
            return 15;
        }

        @Override
        public SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_NETHERITE;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(Items.AMETHYST_SHARD);
        }

        @Override
        public String getName() {
            return TinyRelics.MODID + ":chaos";
        }

        @Override
        public float getToughness() {
            return 1.0F;
        }

        @Override
        public float getKnockbackResistance() {
            return 0.0F;
        }
    }
}
