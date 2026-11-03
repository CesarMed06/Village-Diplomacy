package com.cesoti2006.villagediplomacy.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public class OpenVillageHudPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenVillageHudPacket> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath("villagediplomacy", "open_hud"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenVillageHudPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, OpenVillageHudPacket::getVillageNameSerialized,
                    ByteBufCodecs.VAR_INT, OpenVillageHudPacket::getReputation,
                    ByteBufCodecs.STRING_UTF8, OpenVillageHudPacket::getRelationKey,
                    OpenVillageHudPacket::new);

    private final String villageNameSerialized;
    private final int reputation;
    private final String relationKey;

    public OpenVillageHudPacket(String villageNameSerialized, int reputation, String relationKey) {
        this.villageNameSerialized = villageNameSerialized;
        this.reputation = reputation;
        this.relationKey = relationKey;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public String getVillageNameSerialized() { return villageNameSerialized; }
    public int getReputation() { return reputation; }
    public String getRelationKey() { return relationKey; }
}
