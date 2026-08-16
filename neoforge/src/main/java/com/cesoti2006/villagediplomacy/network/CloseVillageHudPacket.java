package com.cesoti2006.villagediplomacy.network;

import com.cesoti2006.villagediplomacy.events.VillageHUDHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class CloseVillageHudPacket {

    public CloseVillageHudPacket() {}

    public CloseVillageHudPacket(FriendlyByteBuf buf) {}

    public void write(FriendlyByteBuf buf) {}

    public static void handle(CloseVillageHudPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(VillageHUDHandler::onPlayerLeaveVillage);
        ctx.get().setPacketHandled(true);
    }
}
