package io.github.sst.remake.event.impl.game.player;

import io.github.sst.remake.event.Cancellable;
import lombok.RequiredArgsConstructor;
import net.minecraft.util.math.Vec3d;


public class MoveEvent extends Cancellable {
    public Vec3d movement;
    public boolean isOnGround;

    public MoveEvent(Vec3d movement, boolean isOnGround) {
        this.movement = movement;
        this.isOnGround = isOnGround;
    }



    public double getX() {
        return movement.x;
    }

    public double getY() {
        return movement.y;
    }

    public double getZ() {
        return movement.z;
    }

    public void setX(double x) {
        movement.x = x;
    }

    public void setY(double y) {
        movement.y = y;
    }

    public void setZ(double z) {
        movement.z = z;
    }
}