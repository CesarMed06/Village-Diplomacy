package com.cesoti2006.villagediplomacy.fabric;

import net.fabricmc.api.ClientModInitializer;

public class VillageDiplomacyQuiltClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        new VillageDiplomacyFabricClient().onInitializeClient();
    }
}
