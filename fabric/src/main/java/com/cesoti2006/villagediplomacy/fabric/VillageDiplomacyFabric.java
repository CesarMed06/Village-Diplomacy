package com.cesoti2006.villagediplomacy.fabric;

import net.fabricmc.api.ModInitializer;
import com.cesoti2006.villagediplomacy.VillageDiplomacy;
import com.cesoti2006.villagediplomacy.fabric.events.FabricEventHandler;

public class VillageDiplomacyFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        VillageDiplomacy.LOGGER.info("Village Diplomacy initializing on Fabric!");
        
        // Register Fabric event handlers
        FabricEventHandler.registerEventHandlers();
    }
}
