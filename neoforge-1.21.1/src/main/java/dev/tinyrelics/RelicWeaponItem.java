package dev.tinyrelics;

import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/** A weapon with fixed melee damage and no durability, so it never breaks. */
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
}
