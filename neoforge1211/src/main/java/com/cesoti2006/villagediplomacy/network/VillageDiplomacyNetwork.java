package com.cesoti2006.villagediplomacy.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class VillageDiplomacyNetwork {

    private static final String PROTOCOL = "1";

    private VillageDiplomacyNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL);

        registrar.playToClient(
                OpenVillageHudPacket.TYPE,
                OpenVillageHudPacket.STREAM_CODEC,
                OpenVillageHudPacket::handle);

        registrar.playToClient(
                CloseVillageHudPacket.TYPE,
                CloseVillageHudPacket.STREAM_CODEC,
                CloseVillageHudPacket::handle);
    }

    public static void sendOpenHud(ServerPlayer player, String villageSerialized, int reputation, String relationKey) {
        PacketDistributor.sendToPlayer(player, new OpenVillageHudPacket(villageSerialized, reputation, relationKey));
    }

    public static void sendCloseHud(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, CloseVillageHudPacket.INSTANCE);
    }
}
