package net.exylia.exyliaStaff.models;

import java.util.UUID;

public class StaffPlayer {
    private UUID uuid;
    private boolean vanished;
    private boolean inStaffMode;

    public StaffPlayer(UUID uuid, boolean vanished, boolean inStaffMode) {
        this.uuid = uuid;
        this.vanished = vanished;
        this.inStaffMode = inStaffMode;
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
}
