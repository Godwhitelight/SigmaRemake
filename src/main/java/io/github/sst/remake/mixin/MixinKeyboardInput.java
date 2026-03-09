package io.github.sst.remake.mixin;

import io.github.sst.remake.event.impl.client.InputEvent;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.option.GameOptions;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class MixinKeyboardInput extends Input {
    @Shadow
    @Final
    private GameOptions settings;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTickTail(CallbackInfo ci) {
        ci.cancel();

        // Read key states using 1.21 field names
        boolean pressingForward = this.settings.forwardKey.isPressed();
        boolean pressingBack = this.settings.backKey.isPressed();
        boolean pressingLeft = this.settings.leftKey.isPressed();
        boolean pressingRight = this.settings.rightKey.isPressed();
        boolean jumping = this.settings.jumpKey.isPressed();
        boolean sneaking = this.settings.sneakKey.isPressed();

        // Calculate movement values
        float movementForward = 0.0f;
        float movementSideways = 0.0f;

        if (pressingForward) {
            ++movementForward;
        }
        if (pressingBack) {
            --movementForward;
        }
        if (pressingLeft) {
            ++movementSideways;
        }
        if (pressingRight) {
            --movementSideways;
        }

        InputEvent event = new InputEvent(movementForward, movementSideways, jumping, sneaking, 0.3F);
        event.call();

        movementSideways = event.strafe;
        movementForward = event.forward;
        jumping = event.jumping;
        sneaking = event.sneaking;

        if (sneaking) {
            movementSideways *= event.sneakFactor;
            movementForward *= event.sneakFactor;
        }

        // Store results in the new 1.21 fields
        this.playerInput = new PlayerInput(
                pressingForward, pressingBack, pressingLeft, pressingRight,
                jumping, sneaking, this.settings.sprintKey.isPressed()
        );
        this.movementVector = new Vec2f(movementSideways, movementForward);
    }
}
