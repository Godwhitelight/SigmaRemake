package io.github.sst.remake.util.render;

import com.mojang.blaze3d.opengl.GlStateManager;
import org.lwjgl.opengl.GL11;

/**
 * Compatibility layer for rendering calls that were removed from RenderSystem in 1.21.
 * <p>
 * In Minecraft 1.21, RenderSystem was massively stripped down. Methods like enableBlend(),
 * disableBlend(), setShaderColor(), blendFuncSeparate(), enableDepthTest(), disableDepthTest(),
 * etc. were all removed. The new rendering pipeline uses RenderPipeline/RenderLayer objects.
 * <p>
 * This utility wraps GlStateManager (moved to com.mojang.blaze3d.opengl.GlStateManager in 1.21)
 * to provide the same API surface that RenderSystem used to have, making it easy to migrate
 * existing rendering code.
 * <p>
 * For calls that have no equivalent in 1.21 (like matrixMode, loadIdentity, ortho, translatef,
 * scalef, pushMatrix, popMatrix), we delegate to GL11 directly since those are fixed-function
 * pipeline calls that were already just thin wrappers.
 */
public final class RenderCompat {

    private RenderCompat() {}

    // ========== Blend ==========

    public static void enableBlend() {
        GlStateManager._enableBlend();
    }

    public static void disableBlend() {
        GlStateManager._disableBlend();
    }

    public static void blendFuncSeparate(int srcFactor, int dstFactor, int srcAlpha, int dstAlpha) {
        GlStateManager._blendFuncSeparate(srcFactor, dstFactor, srcAlpha, dstAlpha);
    }

    public static void defaultBlendFunc() {
        GlStateManager._blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
    }

    // ========== Depth ==========

    public static void enableDepthTest() {
        GlStateManager._enableDepthTest();
    }

    public static void disableDepthTest() {
        GlStateManager._disableDepthTest();
    }

    public static void depthFunc(int func) {
        GlStateManager._depthFunc(func);
    }

    public static void depthMask(boolean mask) {
        GlStateManager._depthMask(mask);
    }

    // ========== Cull ==========

    public static void enableCull() {
        GlStateManager._enableCull();
    }

    public static void disableCull() {
        GlStateManager._disableCull();
    }

    // ========== Color ==========

    /**
     * Replacement for RenderSystem.setShaderColor(r, g, b, a).
     * In 1.21, shader color uniforms are gone. We use GL11.glColor4f as a fallback
     * for immediate-mode rendering.
     */
    public static void setShaderColor(float r, float g, float b, float a) {
        GL11.glColor4f(r, g, b, a);
    }

    public static void colorMask(boolean r, boolean g, boolean b, boolean a) {
        GlStateManager._colorMask(r, g, b, a);
    }

    // ========== Scissor ==========

    public static void enableScissorTest() {
        GlStateManager._enableScissorTest();
    }

    public static void disableScissorTest() {
        GlStateManager._disableScissorTest();
    }

    // ========== Polygon Offset ==========

    public static void enablePolygonOffset() {
        GlStateManager._enablePolygonOffset();
    }

    public static void disablePolygonOffset() {
        GlStateManager._disablePolygonOffset();
    }

    public static void polygonOffset(float factor, float units) {
        GlStateManager._polygonOffset(factor, units);
    }

    // ========== Matrix (fixed-function via GL11) ==========

    /**
     * Replacement for RenderSystem.pushMatrix().
     * Removed in 1.17+; uses GL11 directly.
     */
    public static void pushMatrix() {
        GL11.glPushMatrix();
    }

    /**
     * Replacement for RenderSystem.popMatrix().
     * Removed in 1.17+; uses GL11 directly.
     */
    public static void popMatrix() {
        GL11.glPopMatrix();
    }

    /**
     * Replacement for RenderSystem.translatef(x, y, z).
     * Removed in 1.17+; uses GL11 directly.
     */
    public static void translatef(float x, float y, float z) {
        GL11.glTranslatef(x, y, z);
    }

    /**
     * Replacement for RenderSystem.scalef(x, y, z).
     * Removed in 1.17+; uses GL11 directly.
     */
    public static void scalef(float x, float y, float z) {
        GL11.glScalef(x, y, z);
    }

    /**
     * Replacement for RenderSystem.matrixMode(mode).
     * Removed in 1.17+; uses GL11 directly.
     */
    public static void matrixMode(int mode) {
        GL11.glMatrixMode(mode);
    }

    /**
     * Replacement for RenderSystem.loadIdentity().
     * Removed in 1.17+; uses GL11 directly.
     */
    public static void loadIdentity() {
        GL11.glLoadIdentity();
    }

    /**
     * Replacement for RenderSystem.ortho().
     * Removed in 1.17+; uses GL11 directly.
     */
    public static void ortho(double left, double right, double bottom, double top, double zNear, double zFar) {
        GL11.glOrtho(left, right, bottom, top, zNear, zFar);
    }

    /**
     * Replacement for RenderSystem.multMatrix(matrix).
     * In 1.21, this is no longer available. The caller should use MatrixStack instead.
     * For now, this is a no-op stub.
     */
    public static void multMatrix(org.joml.Matrix4f matrix) {
        // TODO: Implement if needed. In 1.21, MatrixStack-based rendering is preferred.
        // For immediate-mode GL, you'd need to convert to a float buffer and call glMultMatrixf.
        java.nio.FloatBuffer buf = org.lwjgl.BufferUtils.createFloatBuffer(16);
        matrix.get(buf);
        GL11.glMultMatrixf(buf);
    }

    // ========== Texture ==========

    /**
     * Replacement for RenderSystem.enableTexture().
     * Removed in 1.17+; uses GL11 directly.
     */
    public static void enableTexture() {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    /**
     * Replacement for RenderSystem.disableTexture().
     * Removed in 1.17+; uses GL11 directly.
     */
    public static void disableTexture() {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
    }

    // ========== Misc ==========

    /**
     * Replacement for RenderSystem.enableColorMaterial().
     * Removed in 1.17+.
     */
    public static void enableColorMaterial() {
        GL11.glEnable(GL11.GL_COLOR_MATERIAL);
    }

    /**
     * Replacement for RenderSystem.disableLighting().
     * Removed in 1.17+.
     */
    public static void disableLighting() {
        GL11.glDisable(GL11.GL_LIGHTING);
    }

    /**
     * Replacement for RenderSystem.enableAlphaTest().
     * Removed in 1.17+; alpha test is handled by shaders now.
     * Kept as no-op for compatibility.
     */
    public static void enableAlphaTest() {
        // No-op in 1.21 — alpha test removed in 1.17+
    }

    /**
     * Replacement for RenderSystem.disableAlphaTest().
     * Removed in 1.17+; alpha test is handled by shaders now.
     * Kept as no-op for compatibility.
     */
    public static void disableAlphaTest() {
        // No-op in 1.21 — alpha test removed in 1.17+
    }

    /**
     * Replacement for RenderSystem.color4f(r, g, b, a).
     * Removed in 1.17+; uses GL11 directly.
     */
    public static void color4f(float r, float g, float b, float a) {
        GL11.glColor4f(r, g, b, a);
    }

    /**
     * Replacement for RenderSystem.viewport(x, y, width, height).
     * Still exists on GlStateManager.
     */
    public static void viewport(int x, int y, int width, int height) {
        GlStateManager._viewport(x, y, width, height);
    }

    /**
     * Replacement for RenderSystem.clear(mask, isMac).
     * Uses GL11 directly.
     */
    public static void clear(int mask, boolean isMac) {
        GlStateManager._clear(mask);
    }

    /**
     * Replacement for RenderSystem.lineWidth(width).
     * Removed from RenderSystem in 1.21; uses GL11 directly.
     */
    public static void lineWidth(float width) {
        GL11.glLineWidth(width);
    }
}
