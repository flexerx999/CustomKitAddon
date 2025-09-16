package com.strikepractice.customkitaddon.utils;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
            try {
                Attribute attribute = Attribute.valueOf(attributeName.toUpperCase());
                AttributeModifier.Operation op = AttributeModifier.Operation.valueOf(operation.toUpperCase());

                // In 1.21+, we need to use EquipmentSlotGroup instead of EquipmentSlot
                org.bukkit.inventory.EquipmentSlotGroup slotGroup = null;
                if (slot != null) {
                    switch (slot.toUpperCase()) {
                        case "HAND":
                            slotGroup = org.bukkit.inventory.EquipmentSlotGroup.MAINHAND;
                            break;
                        case "OFF_HAND":
                            slotGroup = org.bukkit.inventory.EquipmentSlotGroup.OFFHAND;
                            break;
                        case "HEAD":
                            slotGroup = org.bukkit.inventory.EquipmentSlotGroup.HEAD;
                            break;
                        case "CHEST":
                            slotGroup = org.bukkit.inventory.EquipmentSlotGroup.CHEST;
                            break;
                        case "LEGS":
                            slotGroup = org.bukkit.inventory.EquipmentSlotGroup.LEGS;
                            break;
                        case "FEET":
                            slotGroup = org.bukkit.inventory.EquipmentSlotGroup.FEET;
                            break;
                        default:
                            slotGroup = org.bukkit.inventory.EquipmentSlotGroup.ANY;
                    }
                } else {
                    slotGroup = org.bukkit.inventory.EquipmentSlotGroup.ANY;
                }

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
            }
        }
        return this;
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