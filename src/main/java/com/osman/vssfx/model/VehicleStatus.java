package com.osman.vssfx.model;

public enum VehicleStatus {
    ACTIVE,
    IN_SERVICE,
    ASSIGNED,
    INACTIVE;

    public static VehicleStatus fromDb(String s) {
        if (s == null || s.isBlank()) return ACTIVE;
        return VehicleStatus.valueOf(s.trim().toUpperCase());
    }

    public String toDb() {
        return name();
    }

    @Override
    public String toString() {
        String s = name().toLowerCase().replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
