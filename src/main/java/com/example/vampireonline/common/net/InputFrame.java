package com.example.vampireonline.common.net;

public record InputFrame(
        long sequence,
        boolean up,
        boolean down,
        boolean left,
        boolean right,
        boolean firing,
        double aimX,
        double aimY
) {
    public static InputFrame idle() {
        return new InputFrame(0, false, false, false, false, false, 0.0, 0.0);
    }
}

