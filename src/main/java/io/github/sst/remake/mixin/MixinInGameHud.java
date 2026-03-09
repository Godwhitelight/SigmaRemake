package io.github.sst.remake.mixin;

import io.github.sst.remake.Client;
import io.github.sst.remake.event.impl.game.render.RenderHudEvent;
import io.github.sst.remake.event.impl.game.render.RenderScoreboardEvent;
import io.github.sst.remake.util.render.RenderCompat;
import io.github.sst.remake.util.render.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerInventory;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class MixinInGameHud {
    @Shadow
    @Final
    private MinecraftClient client;

    // In 1.21, render method signature is: render(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci)
    // The renderScoreboardSidebar target also changed significantly.
    // TODO: The exact mixin targets for renderScoreboardSidebar and getArmorStack need verification
    // after Gradle sync with 1.21 mappings. The method signatures and injection points may differ.

    // Scoreboard sidebar rendering was moved to a separate method in 1.20+.
    // The old target: Lnet/minecraft/client/gui/hud/InGameHud;renderScoreboardSidebar(MatrixStack, ScoreboardObjective)V
    // In 1.21, this is: renderScoreboardSidebar(DrawContext, ScoreboardObjective) or similar.
    // For now, we target the render method directly and cancel/allow scoreboard rendering.

    @Inject(method = "render", at = @At("HEAD"))
    private void injectRenderHead(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        // Placeholder — scoreboard event will be handled when exact method targets are confirmed
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerInventory;getArmorStack(I)Lnet/minecraft/item/ItemStack;", shift = At.Shift.BEFORE))
    private void injectAfterRenderVignette(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        GL11.glPushMatrix();

        double scale = client.getWindow().getScaleFactor() / (double) ((float) Math.pow(client.getWindow().getScaleFactor(), 2.0));
        GL11.glScaled(scale, scale, scale);
        GL11.glScaled(Client.INSTANCE.screenManager.scaleFactor, Client.INSTANCE.screenManager.scaleFactor, Client.INSTANCE.screenManager.scaleFactor);
        GL11.glDisable(2912);

        RenderCompat.disableDepthTest();
        // RenderSystem.translatef removed in 1.17+
        // RenderSystem.alphaFunc removed in 1.17+
        RenderCompat.enableBlend();
        RenderCompat.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glDisable(2896);
        RenderCompat.blendFuncSeparate(
                GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ZERO
        );

        new RenderHudEvent().call();
        RenderUtils.resetHudGlState();

        RenderCompat.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderCompat.enableCull();
        RenderCompat.disableDepthTest();
        RenderCompat.enableBlend();
        // RenderSystem.alphaFunc removed in 1.17+

        GL11.glPopMatrix();
    }
}
