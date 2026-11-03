package com.cesoti2006.villagediplomacy.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public final class VillageDiplomacyNetwork {

    private static boolean registered = false;

    private VillageDiplomacyNetwork() {}

    public static void register() {
        if (registered) return;
        registered = true;

        PayloadTypeRegistry.playS2C().register(OpenVillageHudPacket.TYPE, OpenVillageHudPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(CloseVillageHudPacket.TYPE, CloseVillageHudPacket.STREAM_CODEC);
    }

    public static void sendOpenHud(ServerPlayer player, String villageSerialized, int reputation, String relationKey) {
        ServerPlayNetworking.send(player, new OpenVillageHudPacket(villageSerialized, reputation, relationKey));
    }

    public static void sendCloseHud(ServerPlayer player) {
        ServerPlayNetworking.send(player, CloseVillageHudPacket.INSTANCE);
    }
}
