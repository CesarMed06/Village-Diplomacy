package com.cesoti2006.villagediplomacy.fabric;

import com.cesoti2006.villagediplomacy.VillageDiplomacy;
import com.cesoti2006.villagediplomacy.fabric.events.VillageHUDHandler;
import com.cesoti2006.villagediplomacy.network.VillageDiplomacyNetwork;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class VillageDiplomacyFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        VillageDiplomacy.LOGGER.info("Village Diplomacy client initializing!");

        
        HudRenderCallback.EVENT.register(VillageHUDHandler::render);

        
        ClientTickEvents.END_CLIENT_TICK.register(client -> VillageHUDHandler.tick());

        
        ClientPlayNetworking.registerGlobalReceiver(VillageDiplomacyNetwork.OPEN_HUD_PACKET_ID, (client, handler, buf, responseSender) -> {
            String villageName = buf.readUtf();
            int reputation = buf.readInt();
            String relation = buf.readUtf();
            client.execute(() -> VillageHUDHandler.onPlayerEnterVillage(villageName, reputation, relation));
        });

        ClientPlayNetworking.registerGlobalReceiver(VillageDiplomacyNetwork.CLOSE_HUD_PACKET_ID, (client, handler, buf, responseSender) -> {
            client.execute(VillageHUDHandler::onPlayerLeaveVillage);
        });
    }
}
