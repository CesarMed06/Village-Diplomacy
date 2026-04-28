package com.cesoti2006.villagediplomacy.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class VillageDiplomacyNetwork {

    private static final String PROTOCOL = "2";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath("villagediplomacy", "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private VillageDiplomacyNetwork() {}

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(OpenVillageHudPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenVillageHudPacket::write)
                .decoder(OpenVillageHudPacket::new)
                .consumerMainThread(OpenVillageHudPacket::handle)
                .add();
        CHANNEL.messageBuilder(CloseVillageHudPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CloseVillageHudPacket::write)
                .decoder(CloseVillageHudPacket::new)
                .consumerMainThread(CloseVillageHudPacket::handle)
                .add();
    }

    public static void sendOpenHud(ServerPlayer player, String villageSerialized, int reputation, String relationKey) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new OpenVillageHudPacket(villageSerialized, reputation, relationKey));
    }

    public static void sendCloseHud(ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new CloseVillageHudPacket());
    }
}
