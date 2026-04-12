package com.qisumei.c4.network;

import com.qisumei.c4.client.ClientMessageRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UIMessagePacket {
    private final String message;
    private final int duration;

    public UIMessagePacket(String message, int duration) {
        this.message = message;
        this.duration = duration;
    }

    public static void encode(UIMessagePacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.message);
        buf.writeInt(packet.duration);
    }

    public static UIMessagePacket decode(FriendlyByteBuf buf) {
        return new UIMessagePacket(buf.readUtf(), buf.readInt());
    }

    public static void handle(UIMessagePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientMessageRenderer.showMessage(Component.literal(packet.message), packet.duration);
        });
        ctx.get().setPacketHandled(true);
    }
}