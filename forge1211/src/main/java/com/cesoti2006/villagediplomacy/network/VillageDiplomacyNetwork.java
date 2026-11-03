package com.cesoti2006.villagediplomacy.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public final class VillageDiplomacyNetwork {

    private static final int PROTOCOL = 1;

    public static final SimpleChannel CHANNEL = ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath("villagediplomacy", "main"))
            .networkProtocolVersion(PROTOCOL)
            .clientAcceptedVersions(Channel.VersionTest.exact(PROTOCOL))
            .serverAcceptedVersions(Channel.VersionTest.exact(PROTOCOL))
            .simpleChannel();

    private static boolean registered = false;

    private VillageDiplomacyNetwork() {}

    public static void register() {
        if (registered) return;
        registered = true;

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
        CHANNEL.send(new OpenVillageHudPacket(villageSerialized, reputation, relationKey),
                PacketDistributor.PLAYER.with(player));
    }

    public static void sendCloseHud(ServerPlayer player) {
        CHANNEL.send(new CloseVillageHudPacket(), PacketDistributor.PLAYER.with(player));
    }
}
