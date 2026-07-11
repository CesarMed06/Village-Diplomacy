package com.cesoti2006.villagediplomacy.network;

import net.minecraft.network.FriendlyByteBuf;

public class OpenVillageHudPacket {

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

    public String getVillageNameSerialized() { return villageNameSerialized; }
    public int getReputation() { return reputation; }
    public String getRelationKey() { return relationKey; }
}
