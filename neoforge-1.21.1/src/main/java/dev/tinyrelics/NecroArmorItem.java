package dev.tinyrelics;

import java.util.List;
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

/** One piece of the necromancer armor. The four pieces together give 11 armor points. */
public class NecroArmorItem extends ArmorItem {
    public NecroArmorItem(Type type, Properties properties) {
        super(TinyRelics.NECRO_MATERIAL, type, properties.durability(type.getDurability(30)));
    }

    /** Every slot has its own picture; all four share the layout of the 3D model. */
    @Override
    public ResourceLocation getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, ArmorMaterial.Layer layer, boolean innerModel) {
        return ResourceLocation.fromNamespaceAndPath(TinyRelics.MODID, "textures/models/armor/necromancer_" + slot.getName() + ".png");
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tinyrelics.necro_armor.tooltip").withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.translatable("item.tinyrelics.necro_armor.tooltip2").withStyle(ChatFormatting.DARK_AQUA));
    }
}
