package com.cesoti2006.villagediplomacy.network;

import com.cesoti2006.villagediplomacy.events.VillageHUDHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class CloseVillageHudPacket {

    public CloseVillageHudPacket() {}

    public CloseVillageHudPacket(FriendlyByteBuf buf) {}

    public void write(FriendlyByteBuf buf) {}

    public static void handle(CloseVillageHudPacket pkt, CustomPayloadEvent.Context ctx) {
        VillageHUDHandler.onPlayerLeaveVillage();
    }
}
