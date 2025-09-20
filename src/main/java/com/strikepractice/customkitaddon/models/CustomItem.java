package com.strikepractice.customkitaddon.models;

import org.bukkit.inventory.ItemStack;

public class CustomItem {

    private final ItemStack itemStack;
    private final int slot;
    private boolean enchantGui;

    public CustomItem(ItemStack itemStack, int slot) {
        this.itemStack = itemStack;
        this.slot = slot;
        this.enchantGui = false;
    }

    public ItemStack getItemStack() {
        return itemStack.clone();
    }

    public int getSlot() {
        return slot;
    }

    public boolean hasSpecificSlot() {
        return slot >= 0;
    }

    public boolean hasEnchantGui() {
        return enchantGui;
    }

    public void setEnchantGui(boolean enchantGui) {
        this.enchantGui = enchantGui;
    }
}