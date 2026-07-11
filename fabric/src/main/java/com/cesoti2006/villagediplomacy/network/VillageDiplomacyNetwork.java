package com.cesoti2006.villagediplomacy.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;

public final class VillageDiplomacyNetwork {

    public static final ResourceLocation OPEN_HUD_PACKET_ID = new ResourceLocation("villagediplomacy", "open_hud");
    public static final ResourceLocation CLOSE_HUD_PACKET_ID = new ResourceLocation("villagediplomacy", "close_hud");

    private VillageDiplomacyNetwork() {}

    public static void sendOpenHud(ServerPlayer player, String villageSerialized, int reputation, String relationKey) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUtf(villageSerialized);
        buf.writeInt(reputation);
        buf.writeUtf(relationKey);
        ServerPlayNetworking.send(player, OPEN_HUD_PACKET_ID, buf);
    }

    public static void sendCloseHud(ServerPlayer player) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        ServerPlayNetworking.send(player, CLOSE_HUD_PACKET_ID, buf);
    }
}
