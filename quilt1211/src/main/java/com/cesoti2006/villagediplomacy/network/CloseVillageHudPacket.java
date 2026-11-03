package com.cesoti2006.villagediplomacy.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public class CloseVillageHudPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CloseVillageHudPacket> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath("villagediplomacy", "close_hud"));

    public static final CloseVillageHudPacket INSTANCE = new CloseVillageHudPacket();

    public static final StreamCodec<RegistryFriendlyByteBuf, CloseVillageHudPacket> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    public CloseVillageHudPacket() {}

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
