package com.cesoti2006.villagediplomacy.network;

import com.cesoti2006.villagediplomacy.events.VillageHUDHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class CloseVillageHudPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CloseVillageHudPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("villagediplomacy", "close_hud"));

    public static final StreamCodec<FriendlyByteBuf, CloseVillageHudPacket> STREAM_CODEC =
            StreamCodec.unit(new CloseVillageHudPacket());

    public CloseVillageHudPacket() {}

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(VillageHUDHandler::onPlayerLeaveVillage);
    }
}
