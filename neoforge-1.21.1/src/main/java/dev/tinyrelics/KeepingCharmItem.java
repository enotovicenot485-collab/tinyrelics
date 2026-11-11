package dev.tinyrelics;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class KeepingCharmItem extends Item {
    public KeepingCharmItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.tinyrelics.keeping_charm.tooltip").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.tinyrelics.keeping_charm.tooltip2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
