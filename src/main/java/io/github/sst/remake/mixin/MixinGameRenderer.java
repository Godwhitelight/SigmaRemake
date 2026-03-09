package io.github.sst.remake.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.sst.remake.Client;
import io.github.sst.remake.event.impl.game.player.MovementFovEvent;
import io.github.sst.remake.event.impl.game.render.Render2DEvent;
import io.github.sst.remake.event.impl.game.render.Render3DEvent;
import io.github.sst.remake.event.impl.game.render.RenderLevelEvent;
import io.github.sst.remake.module.impl.gui.RearViewModule;
import io.github.sst.remake.util.render.RenderCompat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.lwjgl.opengl.GL11;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
    @Shadow
    @Final
    private MinecraftClient client;

    // In 1.21, render signature is: render(RenderTickCounter tickCounter, boolean tick)
    // renderWorld signature is: renderWorld(RenderTickCounter tickCounter)
    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;renderWorld(Lnet/minecraft/client/render/RenderTickCounter;)V"))
    private void injectRender(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        // Called right after getProfiler().push("level");
        float tickDelta = tickCounter.getTickDelta(true);
        new RenderLevelEvent(tickDelta, 0L).call();
    }

    @Inject(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;overlay:Lnet/minecraft/client/gui/screen/Overlay;", ordinal = 0, shift = At.Shift.BEFORE, opcode = Opcodes.GETFIELD))
    private void injectBeforeUIRender(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        float tickDelta = tickCounter.getTickDelta(true);
        new Render2DEvent(tickDelta, 0L, tick).call();
    }

    // In 1.21, renderWorld signature is: renderWorld(RenderTickCounter tickCounter)
    @Inject(method = "renderWorld", at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/GameRenderer;renderHand:Z", ordinal = 0, shift = At.Shift.BEFORE, opcode = Opcodes.GETFIELD))
    private void injectBeforeRenderHand(RenderTickCounter tickCounter, CallbackInfo ci) {
        if (RearViewModule.RENDERING_REAR_VIEW) {
            return;
        }

        GL11.glPushMatrix();
        // In 1.21, MatrixStack-based multMatrix is gone from RenderSystem.
        // The world rendering now uses a different matrix pipeline.
        // For immediate-mode 3D overlays, we just push GL state directly.
        if (client != null && client.world != null && client.player != null) {
            GL11.glTranslatef(0.0F, 0.0F, 0.0F);
            RenderCompat.disableDepthTest();
            RenderCompat.depthMask(false);
            GL11.glDisable(2896);
            new Render3DEvent().call();
            RenderCompat.enableDepthTest();
            RenderCompat.depthMask(true);
            // Removed: client.getTextureManager().bindTexture(TextureManager.MISSING_IDENTIFIER)
            // bindTexture(Identifier) was removed in 1.21
        }
        GL11.glPopMatrix();
    }

    @WrapOperation(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;currentScreen:Lnet/minecraft/client/gui/screen/Screen;", ordinal = 1, opcode = Opcodes.GETFIELD))
    private Screen wrapRender(MinecraftClient instance, Operation<Screen> original) {
        if (Client.INSTANCE.screenManager.currentScreen == null) {
            return original.call(this.client);
        } else {
            return null;
        }
    }

    @ModifyExpressionValue(method = "updateMovementFovMultiplier", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;getSpeed()F"))
    private float modifyGetSpeed(float original) {
        MovementFovEvent event = new MovementFovEvent(original);
        event.call();
        return event.speed;
    }
}
