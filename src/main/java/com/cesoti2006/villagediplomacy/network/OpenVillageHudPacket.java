package com.cesoti2006.villagediplomacy.network;

import com.cesoti2006.villagediplomacy.events.VillageHUDHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenVillageHudPacket {

    private final String villageNameSerialized;
    private final int reputation;
    /** One of: ally, friendly, neutral, hostile, enemy */
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

    public static void handle(OpenVillageHudPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> VillageHUDHandler.onPlayerEnterVillage(
                pkt.villageNameSerialized, pkt.reputation, pkt.relationKey));
        ctx.get().setPacketHandled(true);
    }
}
