package io.github.sst.remake.util.render.shader.impl;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * JelloBlurJSON - provides the blur shader JSON definition.
 *
 * In 1.16, this implemented the Resource interface. In 1.19+, Resource changed from an interface
 * to a class, so we can no longer implement it. This is now a simple helper class that provides
 * the blur shader JSON as an InputStream.
 *
 * TODO: Integrate with PostEffectProcessor when blur shader system is rewritten.
 */
public class JelloBlurJSON {
    private static final String BLUR_JSON = "{\"targets\":[\"jelloswap\",\"jello\"],\"passes\":[{\"name\":\"blur\",\"intarget\":\"minecraft:main\",\"outtarget\":\"jelloswap\",\"uniforms\":[{\"name\":\"BlurDir\",\"values\":[1,0]},{\"name\":\"Radius\",\"values\":[20]}]},{\"name\":\"blur\",\"intarget\":\"jelloswap\",\"outtarget\":\"jello\",\"uniforms\":[{\"name\":\"BlurDir\",\"values\":[0,1]},{\"name\":\"Radius\",\"values\":[20]}]}]}";

    public InputStream getInputStream() {
        return new ByteArrayInputStream(BLUR_JSON.getBytes());
    }
}
