package net.exylia.exyliaStaff.models;

import org.bukkit.inventory.ItemStack;
import java.util.UUID;

public class StaffPlayer {
    private UUID uuid;
    private boolean vanished;
    private boolean inStaffMode;
    private ItemStack[] inventory;
    private ItemStack[] armor;
    private ItemStack offHandItem;
    private float exp;
    private int level;

    public StaffPlayer(UUID uuid, boolean vanished, boolean inStaffMode) {
        this.uuid = uuid;
        this.vanished = vanished;
        this.inStaffMode = inStaffMode;
        this.inventory = null;
        this.armor = null;
        this.offHandItem = null;
        this.exp = 0;
        this.level = 0;
    }

    public StaffPlayer(UUID uuid, boolean vanished, boolean inStaffMode,
                       ItemStack[] inventory, ItemStack[] armor, ItemStack offHandItem,
                       float exp, int level) {
        this.uuid = uuid;
        this.vanished = vanished;
        this.inStaffMode = inStaffMode;
        this.inventory = inventory;
        this.armor = armor;
        this.offHandItem = offHandItem;
        this.exp = exp;
        this.level = level;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public boolean isVanished() {
        return vanished;
    }

    public void setVanished(boolean vanished) {
        this.vanished = vanished;
    }

    public boolean isInStaffMode() {
        return inStaffMode;
    }

    public void setInStaffMode(boolean inStaffMode) {
        this.inStaffMode = inStaffMode;
    }

    public ItemStack[] getInventory() {
        return inventory;
    }

    public void setInventory(ItemStack[] inventory) {
        this.inventory = inventory;
    }

    public ItemStack[] getArmor() {
        return armor;
    }

    public void setArmor(ItemStack[] armor) {
        this.armor = armor;
    }

    public ItemStack getOffHandItem() {
        return offHandItem;
    }

    public void setOffHandItem(ItemStack offHandItem) {
        this.offHandItem = offHandItem;
    }

    public float getExp() {
        return exp;
    }

    public void setExp(float exp) {
        this.exp = exp;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public boolean hasStoredInventory() {
        return inventory != null;
    }
}