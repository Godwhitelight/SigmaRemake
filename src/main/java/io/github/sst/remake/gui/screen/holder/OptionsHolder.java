package io.github.sst.remake.gui.screen.holder;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class OptionsHolder extends Screen {
    public OptionsHolder() {
        super(Text.literal("Jello Options"));
    }

    public boolean isPauseScreen() {
        return true;
    }
}
