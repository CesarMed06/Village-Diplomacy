package com.cesoti2006.villagediplomacy.fabric;

import net.fabricmc.api.ModInitializer;

public class VillageDiplomacyQuilt implements ModInitializer {

    @Override
    public void onInitialize() {
        new VillageDiplomacyFabric().onInitialize();
    }
}
