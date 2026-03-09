package io.github.sst.remake;

public class PlayerInput {
    private final float horizontalSpeed;
    private final float verticalSpeed;
    private final boolean jumping;
    private final boolean sneaking;

    public PlayerInput(float horizontalSpeed, float verticalSpeed, boolean jumping, boolean sneaking) {
        this.horizontalSpeed = horizontalSpeed;
        this.verticalSpeed = verticalSpeed;
        this.jumping = jumping;
        this.sneaking = sneaking;
    }

    // Getters can be added here if necessary
}