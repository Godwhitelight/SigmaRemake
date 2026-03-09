package io.github.sst.remake.manager.impl;

import io.github.sst.remake.Client;
import io.github.sst.remake.data.bus.Priority;
import io.github.sst.remake.data.bus.Subscribe;
import io.github.sst.remake.event.impl.client.RenderClient2DEvent;
import io.github.sst.remake.event.impl.game.net.ReceivePacketEvent;
import io.github.sst.remake.event.impl.game.render.Render2DEvent;
import io.github.sst.remake.event.impl.game.render.Render3DEvent;
import io.github.sst.remake.manager.Manager;
import io.github.sst.remake.util.IMinecraft;
import io.github.sst.remake.util.render.RenderCompat;
import io.github.sst.remake.util.render.RenderUtils;
import io.github.sst.remake.util.render.ScissorUtils;
import io.github.sst.remake.util.render.shader.ShaderUtils;
import io.github.sst.remake.util.render.image.Resources;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import org.lwjgl.opengl.GL11;

public final class HUDManager extends Manager implements IMinecraft {
    // TODO: ShaderEffect was removed in 1.21. Blur shader needs PostEffectProcessor rewrite.
    // private static PostEffectProcessor blurShaderGroup;

    private static Framebuffer blurFramebuffer;
    private static Framebuffer blurSwapFramebuffer;

    private static int maxBlurX = 0;
    private static int maxBlurY = 0;
    private static int blurWidth = client.getFramebuffer().textureWidth;
    private static int blurHeight = client.getFramebuffer().textureHeight;

    @Subscribe(priority = Priority.HIGH)
    public void onRender(Render2DEvent event) {
        GL11.glPushMatrix();

        double localScaleFactor = client.getWindow().getScaleFactor() / (double) ((float) Math.pow(client.getWindow().getScaleFactor(), 2.0));
        GL11.glScaled(localScaleFactor, localScaleFactor, 1.0);
        GL11.glScaled(Client.INSTANCE.screenManager.scaleFactor, Client.INSTANCE.screenManager.scaleFactor, 1.0);
        RenderCompat.disableDepthTest();
        GL11.glPushMatrix();
        GL11.glTranslatef(0.0F, 0.0F, 1000.0F);

        if (client.world != null) {
            GL11.glDisable(GL11.GL_LIGHTING);
            int x = 0;
            int y = 0;

            int imageWidth = 170;

            if (client.getDebugHud().shouldShowDebugHud()) {
                x = client.getWindow().getWidth() / 2 - imageWidth / 2;
            }

            GL11.glAlphaFunc(519, 0.0F);

            RenderUtils.drawImage((float) x, y, 170.0F, 104.0F,
                    !(Client.INSTANCE.screenManager.scaleFactor > 1.0F) ? Resources.WATERMARK
                            : Resources.WATERMARK_2X);

            new RenderClient2DEvent().call();
        }

        if (Client.INSTANCE.screenManager.currentScreen != null && client.overlay == null) {
            Client.INSTANCE.screenManager.currentScreen.draw(1.0F);
        }

        GL11.glPopMatrix();
        RenderCompat.enableDepthTest();
        // enableAlphaTest removed in 1.17+
        GL11.glAlphaFunc(GL11.GL_GEQUAL, 0.1F);

        // bindTexture(TextureManager.MISSING_IDENTIFIER) removed — no longer needed

        GL11.glPopMatrix();
    }

    @Subscribe
    public void onPacketReceive(ReceivePacketEvent event) {
        if (event.packet instanceof CloseScreenS2CPacket) {
            ShaderUtils.resetShader();
        }
    }

    @Subscribe(priority = Priority.LOWEST)
    public void onRender3D(Render3DEvent event) {
        // TODO: The entire blur shader pipeline needs rewriting for 1.21.
        // ShaderEffect was removed. PostEffectProcessor is the replacement but has a very different API.
        // For now, the blur functionality is stubbed out.
        // The old code used ShaderEffect + SigmaBlurShader ResourceManager + custom framebuffers.

        if (Client.INSTANCE.configManager.hqBlur
                && blurWidth < maxBlurX
                && blurHeight < maxBlurY) {

            // TODO: Implement blur using PostEffectProcessor when shader system is rewritten
            // Old code: new ShaderEffect(...) -> setupDimensions -> set uniforms -> render
        }

        blurWidth = client.getFramebuffer().textureWidth;
        blurHeight = client.getFramebuffer().textureHeight;
        maxBlurX = 0;
        maxBlurY = 0;
    }

    public static void registerBlurArea(int x, int y, int width, int height) {
        blurWidth = Math.min(x, blurWidth);
        blurHeight = Math.min(y, blurHeight);
        maxBlurX = Math.max(x + width, maxBlurX);
        maxBlurY = Math.max(y + height, maxBlurY);
    }

    public static void renderFinalBlur() {
        if (blurFramebuffer == null) {
            return;
        }

        // TODO: Blur framebuffer drawing needs PostEffectProcessor rewrite
        // Old code used RenderSystem.matrixMode/loadIdentity/ortho which are all removed in 1.17+
        // Old code used beginWrite(true) which is removed in 1.21
        // For now, just blit back to main framebuffer
        client.getFramebuffer().blitToScreen();
    }

}
