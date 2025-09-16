package com.strikepractice.customkitaddon.models;

import org.bukkit.inventory.ItemStack;

public class CustomItem {

    private final ItemStack itemStack;
    private final int slot;

    public CustomItem(ItemStack itemStack, int slot) {
        this.itemStack = itemStack;
        this.slot = slot;
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
}