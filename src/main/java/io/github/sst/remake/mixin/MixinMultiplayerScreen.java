package io.github.sst.remake.mixin;

import io.github.sst.remake.Client;
import io.github.sst.remake.util.math.color.ClientColors;
import io.github.sst.remake.util.math.color.ColorHelper;
import io.github.sst.remake.util.viaversion.ViaInstance;
import io.github.sst.remake.util.viaversion.ViaProtocols;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiplayerScreen.class)
public abstract class MixinMultiplayerScreen extends Screen {
    @Unique
    private ClickableWidget versionSelectorWidget;

    @Unique
    private int selectedVersionIndex;

    protected MixinMultiplayerScreen(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void injectInit(CallbackInfo ci) {
        if (!ViaInstance.VIAVERSION_EXISTS) return;

        selectedVersionIndex = ViaInstance.getTargetVersion().ordinal();

        versionSelectorWidget = this.addDrawableChild(ButtonWidget.builder(
                Text.literal(ViaProtocols.getByIndex(selectedVersionIndex).name),
                button -> {
                    selectedVersionIndex = (selectedVersionIndex + 1) % ViaProtocols.values().length;
                    button.setMessage(Text.literal(ViaProtocols.getByIndex(selectedVersionIndex).name));
                    Client.INSTANCE.viaManager.onVersionChange(null, (double) selectedVersionIndex);
                }
        ).dimensions(width / 2 + 40, 7, 114, 20).build());
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void injectRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!ViaInstance.VIAVERSION_EXISTS) return;

        context.drawTextWithShadow(textRenderer, "Jello Portal:", width / 2 - 30, 13, ColorHelper.applyAlpha(ClientColors.LIGHT_GREYISH_BLUE.getColor(), 0.5F));
    }
}
