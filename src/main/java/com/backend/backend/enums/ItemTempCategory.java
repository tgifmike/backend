package com.backend.backend.enums;

public enum ItemTempCategory {
    FROZEN(-100, 32),
    REFRIGERATED(33, 41),
    ROOM_TEMP(42, 90),
    HOT_HOLDING(120, 200);

    private final double min;
    private final double max;

    ItemTempCategory(double min, double max) {
        this.min = min;
        this.max = max;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }
}

