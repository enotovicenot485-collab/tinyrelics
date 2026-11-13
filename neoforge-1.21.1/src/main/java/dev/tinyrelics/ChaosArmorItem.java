package dev.tinyrelics;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** One piece of the Chaos Assassin armor. The four pieces together give 13 armor points. */
public class ChaosArmorItem extends ArmorItem {
    public ChaosArmorItem(Type type, Properties properties) {
        super(TinyRelics.CHAOS_MATERIAL, type, properties.durability(type.getDurability(30)));
    }

    /** Frames of the shimmer that runs over the violet details. */
    private static final int FRAMES = 32;
    private static final Map<EquipmentSlot, ResourceLocation[]> TEXTURES = new EnumMap<>(EquipmentSlot.class);

    private static ResourceLocation[] frames(EquipmentSlot slot) {
        ResourceLocation[] frames = new ResourceLocation[FRAMES];
        for (int i = 0; i < FRAMES; i++) {
            frames[i] = ResourceLocation.fromNamespaceAndPath(TinyRelics.MODID,
                    "textures/models/armor/chaos_" + slot.getName() + "_" + i + ".png");
        }
        return frames;
    }

    /**
     * Every slot has its own picture, and each picture comes in 32 frames. The game asks
     * for the texture every time it draws the armor, so answering with the frame for the
     * current moment animates it.
     */
    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        int frame = entity == null ? 0 : (entity.tickCount / 2) % FRAMES;
        return TEXTURES.computeIfAbsent(slot, ChaosArmorItem::frames)[frame];
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tinyrelics.chaos_armor.tooltip").withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.translatable("item.tinyrelics.chaos_armor.tooltip2", Component.keybind("key.tinyrelics.armor")).withStyle(ChatFormatting.DARK_PURPLE));
    }
}
