package com.cesoti2006.villagediplomacy.network;

import com.cesoti2006.villagediplomacy.events.VillageHUDHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class OpenVillageHudPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenVillageHudPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("villagediplomacy", "open_hud"));

    public static final StreamCodec<FriendlyByteBuf, OpenVillageHudPacket> STREAM_CODEC =
            StreamCodec.of((buf, pkt) -> pkt.write(buf), OpenVillageHudPacket::new);

    private final String villageNameSerialized;
    private final int reputation;
    private final String relationKey;

    public OpenVillageHudPacket(String villageNameSerialized, int reputation, String relationKey) {
        this.villageNameSerialized = villageNameSerialized;
        this.reputation = reputation;
        this.relationKey = relationKey;
    }

    public OpenVillageHudPacket(FriendlyByteBuf buf) {
        this.villageNameSerialized = buf.readUtf();
        this.reputation = buf.readInt();
        this.relationKey = buf.readUtf();
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(villageNameSerialized);
        buf.writeInt(reputation);
        buf.writeUtf(relationKey);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> VillageHUDHandler.onPlayerEnterVillage(
                villageNameSerialized, reputation, relationKey));
    }
}
