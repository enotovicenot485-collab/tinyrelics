package dev.tinyrelics;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A weapon with fixed melee damage. If it was given durability it wears by one point for every
 * hit and every use of an ability; without durability (Thor's hammer) it never breaks.
 */
public class RelicWeaponItem extends Item {
    private final Multimap<Attribute, AttributeModifier> modifiers;

    /**
     * @param damage total melee damage shown in the tooltip
     * @param speed  attack speed modifier (-2.4 is a sword, -3.0 is an axe)
     */
    public RelicWeaponItem(Properties properties, double damage, double speed) {
        super(properties);
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        // A bare hand already deals 1 damage, the modifier adds the rest.
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", damage - 1.0D, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", speed, AttributeModifier.Operation.ADDITION));
        this.modifiers = builder.build();
    }

    @Override
    @SuppressWarnings("deprecation")
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? this.modifiers : super.getDefaultAttributeModifiers(slot);
    }

    /** One point of wear. Does nothing on the client, in creative mode, or for a weapon without durability. */
    protected static void wear(ItemStack stack, LivingEntity holder) {
        stack.hurtAndBreak(1, holder, entity -> entity.broadcastBreakEvent(EquipmentSlot.MAINHAND));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        wear(stack, attacker);
        return true;
    }
}
