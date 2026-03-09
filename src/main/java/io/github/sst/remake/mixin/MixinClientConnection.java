package io.github.sst.remake.mixin;

import io.github.sst.remake.event.impl.game.net.ReceivePacketEvent;
import io.github.sst.remake.event.impl.game.net.SendPacketEvent;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.PacketCallbacks;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class MixinClientConnection {
    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/packet/Packet;)V", at = @At(value = "HEAD"), cancellable = true)
    private void injectBeforeHandle(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo ci) {
        ReceivePacketEvent event = new ReceivePacketEvent(packet);
        event.call();

        if (event.cancelled)
            ci.cancel();
    }

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/PacketCallbacks;)V", at = @At(value = "HEAD"), cancellable = true)
    private void injectBeforeSend(Packet<?> packet, @Nullable PacketCallbacks callbacks, CallbackInfo ci) {
        SendPacketEvent event = new SendPacketEvent(packet);
        event.call();

        if (event.cancelled)
            ci.cancel();
    }
}
