package com.strikepractice.customkitaddon.utils;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ItemBuilder {

    private ItemStack item;
    private ItemMeta meta;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder(ItemStack item) {
        this.item = item.clone();
        this.meta = this.item.getItemMeta();
    }

    public ItemBuilder setName(String name) {
        if (meta != null) {
            meta.setDisplayName(name);
        }
        return this;
    }

    public ItemBuilder setLore(String... lore) {
        if (meta != null) {
            meta.setLore(Arrays.asList(lore));
        }
        return this;
    }

    public ItemBuilder setLore(List<String> lore) {
        if (meta != null) {
            meta.setLore(lore);
        }
        return this;
    }

    public ItemBuilder addLoreLine(String line) {
        if (meta != null) {
            List<String> lore = meta.getLore();
            if (lore == null) {
                lore = new ArrayList<>();
            }
            lore.add(line);
            meta.setLore(lore);
        }
        return this;
    }

    public ItemBuilder setAmount(int amount) {
        item.setAmount(amount);
        return this;
    }

    public ItemBuilder setDurability(short durability) {
        item.setDurability(durability);
        return this;
    }

    public ItemBuilder addEnchantment(Enchantment enchantment, int level) {
        meta.addEnchant(enchantment, level, true);
        return this;
    }

    public ItemBuilder addItemFlag(ItemFlag... flags) {
        if (meta != null) {
            meta.addItemFlags(flags);
        }
        return this;
    }

    public ItemBuilder setUnbreakable(boolean unbreakable) {
        if (meta != null) {
            meta.setUnbreakable(unbreakable);
        }
        return this;
    }

    public ItemBuilder setCustomModelData(int data) {
        if (meta != null) {
            meta.setCustomModelData(data);
        }
        return this;
    }

    public ItemBuilder addAttributeModifier(String attributeName, double amount, String operation, String slot) {
        if (meta != null) {
            Attribute attribute = Attribute.valueOf(attributeName.toUpperCase());
            AttributeModifier.Operation op = AttributeModifier.Operation.valueOf(operation.toUpperCase());
            try {
                // In 1.21+, we need to use EquipmentSlotGroup instead of EquipmentSlot
                EquipmentSlotGroup slotGroup = getEquipmentSlotGroup(slot);

                // Use the new constructor for 1.21+
                AttributeModifier modifier = new AttributeModifier(
                        org.bukkit.NamespacedKey.minecraft(attributeName.toLowerCase().replace("_", ".")),
                        amount,
                        op,
                        slotGroup
                );

                meta.addAttributeModifier(attribute, modifier);
            } catch (Exception e) {
                // Invalid attribute, operation, or slot - skip
                // Silent fail to avoid dependencies on plugin logger

                AttributeModifier modifier = new AttributeModifier(
                        UUID.randomUUID(),
                        attributeName,
                        amount, op, getEquipmentSlot(slot)
                );

                meta.addAttributeModifier(attribute, modifier);
            }
        }
        return this;
    }

    private static @NotNull EquipmentSlotGroup getEquipmentSlotGroup(String slot) {
        EquipmentSlotGroup slotGroup;
        if (slot != null) {
            switch (slot.toUpperCase()) {
                case "HAND": return EquipmentSlotGroup.MAINHAND;
                case "OFF_HAND": return EquipmentSlotGroup.OFFHAND;
                case "HEAD": return EquipmentSlotGroup.HEAD;
                case "CHEST": return EquipmentSlotGroup.CHEST;
                case "LEGS": return EquipmentSlotGroup.LEGS;
                case "FEET": return EquipmentSlotGroup.FEET;
                default: return EquipmentSlotGroup.ANY;
            }
        } else {
            slotGroup = EquipmentSlotGroup.ANY;
        }
        return slotGroup;
    }

    private static @NotNull EquipmentSlot getEquipmentSlot(String s) {
        EquipmentSlot slot;
        if (s != null) {
            switch (s.toUpperCase()) {
                case "HAND": return EquipmentSlot.HAND;
                case "OFF_HAND": return EquipmentSlot.OFF_HAND;
                case "HEAD": return EquipmentSlot.HEAD;
                case "CHEST": return EquipmentSlot.CHEST;
                case "LEGS": return EquipmentSlot.LEGS;
                case "FEET": return EquipmentSlot.FEET;
                default: return EquipmentSlot.BODY;
            }
        } else {
            slot = EquipmentSlot.BODY;
        }
        return slot;
    }

    public ItemBuilder hideAllFlags() {
        if (meta != null) {
            meta.addItemFlags(ItemFlag.values());
        }
        return this;
    }

    public ItemStack build() {
        if (meta != null) {
            item.setItemMeta(meta);
        }
        return item;
    }
}