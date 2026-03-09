package io.github.sst.remake.util;

import io.github.sst.remake.util.client.TimerSpeedAccess;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public interface IMinecraft {
    MinecraftClient client = MinecraftClient.getInstance();

    default float getTimer() {
        return ((TimerSpeedAccess) client.renderTickCounter).getTimerSpeed();
    }

    default void setTimer(float speed) {
        ((TimerSpeedAccess) client.renderTickCounter).setTimerSpeed(speed);
    }

    default void addChatMessage(String text) {
        client.inGameHud.getChatHud().addMessage(Text.of(text));
    }

    default void sendChatMessage(String text) {
        if (client.getNetworkHandler() != null) {
            client.getNetworkHandler().sendChatMessage(text);
        }
    }
}