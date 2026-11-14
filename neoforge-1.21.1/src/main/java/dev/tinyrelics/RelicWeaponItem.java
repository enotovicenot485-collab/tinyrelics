package dev.tinyrelics;

import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/**
 * A weapon with fixed melee damage. If it was given durability it wears by one point for every
 * hit and every use of an ability; without durability (Thor's hammer) it never breaks.
 */
public class RelicWeaponItem extends Item {
    /**
     * @param damage total melee damage shown in the tooltip
     * @param speed  attack speed modifier (-2.4 is a sword, -3.0 is an axe)
     */
    public RelicWeaponItem(Properties properties, double damage, double speed) {
        super(properties.attributes(createAttributes(damage, speed)));
    }

    private static ItemAttributeModifiers createAttributes(double damage, double speed) {
        return ItemAttributeModifiers.builder()
                // A bare hand already deals 1 damage, the modifier adds the rest.
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(BASE_ATTACK_DAMAGE_ID, damage - 1.0D, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(BASE_ATTACK_SPEED_ID, speed, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    /** One point of wear. Does nothing on the client, in creative mode, or for a weapon without durability. */
    protected static void wear(ItemStack stack, LivingEntity holder) {
        stack.hurtAndBreak(1, holder, EquipmentSlot.MAINHAND);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        wear(stack, attacker);
        return true;
    }
}
