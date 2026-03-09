package io.github.sst.remake.gui.screen.options;

import io.github.sst.remake.Client;
import io.github.sst.remake.gui.screen.holder.OptionsHolder;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class InGameOptionsScreen extends GameMenuScreen {
    private static long lastSaveTime;

    public InGameOptionsScreen() {
        super();

        long now = System.currentTimeMillis();
        if (now - lastSaveTime >= 3000L) {
            lastSaveTime = now;

            Client.LOGGER.info("Saving profiles...");

            Client.INSTANCE.configManager.saveProfile(Client.INSTANCE.configManager.currentProfile, false);
            Client.INSTANCE.configManager.saveScreenConfig(true);
            Client.INSTANCE.configManager.saveClientConfig();
        }
    }

    @Override
    public void init() {
        super.init();

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Jello for Fabric Options"),
                w -> client.setScreen(new OptionsHolder())
        ).dimensions(width / 2 - 102, height - 45, 204, 20).build());

        // Remove the "Open to LAN" button row if present
        this.children().removeIf(widget ->
                widget instanceof ButtonWidget && ((ButtonWidget) widget).getY() == this.height / 4 + 72 - 16
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
