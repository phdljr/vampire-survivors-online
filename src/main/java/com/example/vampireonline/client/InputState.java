package com.example.vampireonline.client;

import com.example.vampireonline.common.net.InputFrame;

final public class InputState {
    private long sequence;
    private boolean up;
    private boolean down;
    private boolean left;
    private boolean right;
    private boolean firing;
    private double aimX;
    private double aimY;

    synchronized void setUp(boolean up) {
        this.up = up;
    }

    synchronized void setDown(boolean down) {
        this.down = down;
    }

    synchronized void setLeft(boolean left) {
        this.left = left;
    }

    synchronized void setRight(boolean right) {
        this.right = right;
    }

    synchronized void setFiring(boolean firing) {
        this.firing = firing;
    }

    synchronized void setAim(double aimX, double aimY) {
        this.aimX = aimX;
        this.aimY = aimY;
    }

    synchronized InputFrame nextFrame(double worldX, double worldY, double scale) {
        return new InputFrame(++sequence, up, down, left, right, firing, worldX + aimX / scale, worldY + aimY / scale);
    }
}

