package io.github.sst.remake.mixin;

import io.github.sst.remake.tracker.impl.RotationTracker;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends EntityRenderer<T, S> {
    // In 1.21, EntityRenderer constructor takes EntityRendererFactory.Context instead of EntityRenderDispatcher
    protected MixinLivingEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Unique
    private LivingEntity captured;

    // In 1.21, the render method signature changed. LivingEntityRenderer.render now takes the render state.
    // The updateRenderState method is where we can capture the entity.
    // Target: updateRenderState(LivingEntity, LivingEntityRenderState, float)
    @Inject(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
            at = @At("HEAD"))
    private void injectUpdateRenderState(T livingEntity, S state, float tickDelta, CallbackInfo ci) {
        captured = livingEntity;
    }

    // In 1.21, the yaw/pitch interpolation still happens via MathHelper.lerpAngleDegrees/lerp calls
    // but now inside updateRenderState rather than render. We modify them there.
    @ModifyArgs(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerpAngleDegrees(FFF)F", ordinal = 1))
    private void modifyYawArgs(Args args) {
        if (captured instanceof ClientPlayerEntity && !RotationTracker.renderingGui) {
            args.set(1, RotationTracker.lastYaw);
            args.set(2, RotationTracker.yaw);
        }
    }

    @ModifyArgs(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerp(FFF)F", ordinal = 0))
    private void modifyPitchArgs(Args args) {
        if (captured instanceof ClientPlayerEntity && !RotationTracker.renderingGui) {
            args.set(1, RotationTracker.lastPitch);
            args.set(2, RotationTracker.pitch);
        }
    }
}
