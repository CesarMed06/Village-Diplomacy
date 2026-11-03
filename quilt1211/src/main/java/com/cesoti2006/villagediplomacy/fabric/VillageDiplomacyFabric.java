package com.cesoti2006.villagediplomacy.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import com.cesoti2006.villagediplomacy.VillageDiplomacy;
import com.cesoti2006.villagediplomacy.commands.DiplomacyCommands;
import com.cesoti2006.villagediplomacy.config.VillageDiplomacyConfig;
import com.cesoti2006.villagediplomacy.network.VillageDiplomacyNetwork;

public class VillageDiplomacyFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        VillageDiplomacyConfig.loadConfig();
        VillageDiplomacy.LOGGER.info("Village Diplomacy initializing on Fabric 1.5.0!");

        VillageDiplomacyNetwork.register();

        FabricCommonEvents.register();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                DiplomacyCommands.register(dispatcher));
    }
}
