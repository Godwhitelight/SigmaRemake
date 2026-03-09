package io.github.sst.remake.mixin;

import com.mojang.authlib.GameProfile;
import io.github.sst.remake.data.bus.State;
import io.github.sst.remake.event.impl.game.player.ClientPlayerTickEvent;
import io.github.sst.remake.event.impl.game.player.MotionEvent;
import io.github.sst.remake.event.impl.game.player.MoveEvent;
import io.github.sst.remake.tracker.impl.RotationTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.MovementType;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(ClientPlayerEntity.class)
public abstract class MixinClientPlayerEntity extends AbstractClientPlayerEntity {
    @Shadow
    private boolean lastSprinting;
    @Shadow
    @Final
    public ClientPlayNetworkHandler networkHandler;
    @Shadow
    private boolean lastSneaking;

    @Shadow
    protected abstract boolean isCamera();

    @Shadow
    private double lastX;
    @Shadow
    private double lastBaseY;
    @Shadow
    private double lastZ;
    @Shadow
    private float lastYaw;
    @Shadow
    private float lastPitch;
    @Shadow
    private int ticksSinceLastPositionPacketSent;
    @Shadow
    private boolean lastOnGround;
    @Shadow
    private boolean autoJumpEnabled;
    @Shadow
    @Final
    protected MinecraftClient client;

    @Unique
    private ClientPlayerTickEvent clientPlayerTickEvent;

    @Unique
    private MoveEvent moveEvent;

    public MixinClientPlayerEntity(ClientWorld world, GameProfile profile) {
        super(world, profile);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void injectTick(CallbackInfo ci) {
        clientPlayerTickEvent = new ClientPlayerTickEvent();
        clientPlayerTickEvent.call();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void injectTickEnd(CallbackInfo ci) {
        clientPlayerTickEvent.state = State.POST;
        clientPlayerTickEvent.call();
    }

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void injectMove(MovementType movementType, Vec3d movement, CallbackInfo ci) {
        moveEvent = new MoveEvent(movement, this.isOnGround());
        moveEvent.call();

        if (moveEvent.cancelled) {
            ci.cancel();
        }
    }

    @ModifyArgs(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;move(Lnet/minecraft/entity/MovementType;Lnet/minecraft/util/math/Vec3d;)V"))
    private void injectMoveAccessVec3d(Args args) {
        args.set(1, moveEvent.movement);
    }

    @Inject(method = "sendMovementPackets", at = @At("HEAD"), cancellable = true)
    private void injectSendMovementPackets(CallbackInfo ci) {
        ci.cancel();

        MotionEvent motionEvent = new MotionEvent(getX(), getY(), getZ(), RotationTracker.yaw, RotationTracker.pitch, isOnGround());
        motionEvent.call();

        boolean sneaking = this.isSneaking();
        boolean sprinting = this.isSprinting();

        if (sprinting != this.lastSprinting) {
            ClientCommandC2SPacket.Mode mode = sprinting ? ClientCommandC2SPacket.Mode.START_SPRINTING : ClientCommandC2SPacket.Mode.STOP_SPRINTING;
            this.networkHandler.sendPacket(new ClientCommandC2SPacket(this, mode));
            this.lastSprinting = sprinting;
        }

        if (sneaking != this.lastSneaking) {
this.networkHandler.sendPacket(new net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket(new PlayerInput(false, false, false, false, false, true, false)));
            this.lastSneaking = sneaking;
        }

        if (this.isCamera()) {
            double dX = motionEvent.x - this.lastX;
            double dY = motionEvent.y - this.lastBaseY;
            double dZ = motionEvent.z - this.lastZ;
            double dYaw = RotationTracker.yaw - this.lastYaw;
            double dPitch = RotationTracker.pitch - this.lastPitch;

            ++this.ticksSinceLastPositionPacketSent;

            boolean moving = motionEvent.moving || dX * dX + dY * dY + dZ * dZ > 9.0E-4 || this.ticksSinceLastPositionPacketSent >= 20;
            boolean looking = dYaw != 0.0 || dPitch != 0.0;

            if (this.hasVehicle()) {
                Vec3d vec3d = this.getVelocity();
                this.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(vec3d.x, -999, vec3d.getZ(), RotationTracker.yaw, RotationTracker.pitch, this.isOnGround(), this.horizontalCollision));
                moving = false;
            } else if (moving && looking) {
                this.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(motionEvent.x, motionEvent.y, motionEvent.z, RotationTracker.yaw, RotationTracker.pitch, motionEvent.isOnGround(), false));
            } else if (moving) {
                this.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(motionEvent.x, motionEvent.y, motionEvent.z, RotationTracker.yaw, RotationTracker.pitch, motionEvent.isOnGround(), this.horizontalCollision));
            } else if (looking) {
                this.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(RotationTracker.yaw, RotationTracker.pitch, false, false));
            } else if (this.lastOnGround != motionEvent.isOnGround()) {
                this.networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(motionEvent.isOnGround(), false));
            }

            if (moving) {
                this.lastX = motionEvent.x;
                this.lastBaseY = motionEvent.y;
                this.lastZ = motionEvent.z;
                this.ticksSinceLastPositionPacketSent = 0;
            }

            if (looking) {
                this.lastYaw = RotationTracker.yaw;
                this.lastPitch = RotationTracker.pitch;
            }

            this.lastOnGround = motionEvent.isOnGround();
            this.autoJumpEnabled = this.client.options.getAutoJump().getValue();
        }

        motionEvent.state = State.POST;
        motionEvent.call();
    }
}