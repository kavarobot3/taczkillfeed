package net.example.taczkillfeed.network;

import net.example.taczkillfeed.TaczKillfeed;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModMessages {
    public static SimpleChannel INSTANCE;
    private static int packetId = 0;
    private static int id() { return packetId++; }

    // Строгая привязка версии сетевого протокола
    private static final String PROTOCOL_VERSION = "3.0";

    public static void register() {
        INSTANCE = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(TaczKillfeed.MOD_ID + ":messages"))
                .networkProtocolVersion(() -> PROTOCOL_VERSION)
                .clientAcceptedVersions(PROTOCOL_VERSION::equals) // Разрешаем подключение только с версией 3.0
                .serverAcceptedVersions(PROTOCOL_VERSION::equals) // Разрешаем подключение только к серверу с версией 3.0
                .simpleChannel();

        INSTANCE.messageBuilder(KillfeedPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(KillfeedPacket::decode)
                .encoder(KillfeedPacket::encode)
                .consumerMainThread(KillfeedPacket::handle)
                .add();
    }

    public static void sendToCapableClients(KillfeedPacket packet, Iterable<ServerPlayer> players) {
        if (INSTANCE == null) return;
        for (ServerPlayer player : players) {
            if (INSTANCE.isRemotePresent(player.connection.connection)) {
                TaczKillfeed.LOGGER.debug("Sending killfeed packet to {}", player.getName().getString());
                INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
            }
        }
    }
}