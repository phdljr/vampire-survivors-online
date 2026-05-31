package com.example.vampireonline.common.math;

public record Vec2(double x, double y) {
    public static final Vec2 ZERO = new Vec2(0.0, 0.0);

    public Vec2 add(Vec2 other) {
        return new Vec2(x + other.x, y + other.y);
    }

    public Vec2 subtract(Vec2 other) {
        return new Vec2(x - other.x, y - other.y);
    }

    public Vec2 scale(double scalar) {
        return new Vec2(x * scalar, y * scalar);
    }

    public double lengthSquared() {
        return x * x + y * y;
    }

    public double length() {
        return Math.sqrt(lengthSquared());
    }

    public Vec2 normalizeOrZero() {
        double length = length();
        if (length < 0.0001) {
            return ZERO;
        }
        return new Vec2(x / length, y / length);
    }

    public Vec2 clamp(double minX, double minY, double maxX, double maxY) {
        return new Vec2(Math.clamp(x, minX, maxX), Math.clamp(y, minY, maxY));
    }
}

