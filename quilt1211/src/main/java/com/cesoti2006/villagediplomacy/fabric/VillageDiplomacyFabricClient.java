package com.cesoti2006.villagediplomacy.fabric;

import com.cesoti2006.villagediplomacy.VillageDiplomacy;
import com.cesoti2006.villagediplomacy.fabric.events.VillageHUDHandler;
import com.cesoti2006.villagediplomacy.network.CloseVillageHudPacket;
import com.cesoti2006.villagediplomacy.network.OpenVillageHudPacket;
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

        ClientPlayNetworking.registerGlobalReceiver(OpenVillageHudPacket.TYPE, (payload, context) ->
                context.client().execute(() -> VillageHUDHandler.onPlayerEnterVillage(
                        payload.getVillageNameSerialized(), payload.getReputation(), payload.getRelationKey())));

        ClientPlayNetworking.registerGlobalReceiver(CloseVillageHudPacket.TYPE, (payload, context) ->
                context.client().execute(VillageHUDHandler::onPlayerLeaveVillage));
    }
}
