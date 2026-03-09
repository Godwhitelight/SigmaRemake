package io.github.sst.remake.util.render.shader;

import io.github.sst.remake.Client;
import io.github.sst.remake.util.IMinecraft;

/**
 * ShaderUtils - stubbed out for 1.21.
 *
 * In 1.16, this used ShaderEffect (client.gameRenderer.getShader/loadShader/shader/forcedShaderIndex).
 * All of those were removed in 1.17+. The replacement is PostEffectProcessor, but its API is
 * fundamentally different and requires a full rewrite of the blur pipeline.
 *
 * TODO: Rewrite using PostEffectProcessor for 1.21 blur support.
 */
public class ShaderUtils implements IMinecraft {
    private static boolean canBlur() {
        return Client.INSTANCE.configManager.guiBlur;
    }

    public static void applyBlurShader() {
        // TODO: PostEffectProcessor-based blur implementation
        // Old code: client.gameRenderer.loadShader(BLUR_SHADER) + set uniforms
    }

    public static void resetShader() {
        // TODO: PostEffectProcessor-based shader reset
        // Old code: client.gameRenderer.forcedShaderIndex / shader field access
    }

    public static void setShaderRadius(int radius) {
        // TODO: PostEffectProcessor-based radius setting
        // Old code: getShader().passes.get(0).getProgram().getUniformByName("Radius").set(...)
    }

    public static void setShaderRadiusRounded(float radius) {
        setShaderRadius(Math.round(radius * 20.0F));
    }
}
