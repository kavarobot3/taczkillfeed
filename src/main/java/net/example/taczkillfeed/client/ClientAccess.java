package net.example.taczkillfeed.client;

import net.example.taczkillfeed.network.KillfeedPacket;

public class ClientAccess {
    public static void handleKillfeed(KillfeedPacket packet) {
        KillfeedOverlay.addEntry(packet);
    }
}