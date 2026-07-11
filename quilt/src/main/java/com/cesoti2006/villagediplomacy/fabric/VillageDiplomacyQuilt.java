package com.cesoti2006.villagediplomacy.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import com.cesoti2006.villagediplomacy.VillageDiplomacy;
import com.cesoti2006.villagediplomacy.commands.DiplomacyCommands;
import com.cesoti2006.villagediplomacy.config.VillageDiplomacyConfig;
import com.cesoti2006.villagediplomacy.fabric.events.FabricEventHandler;
import com.cesoti2006.villagediplomacy.fabric.events.FabricPersonalityBehaviorHandler;
import com.cesoti2006.villagediplomacy.fabric.events.FabricReputationRestrictionsHandler;
import com.cesoti2006.villagediplomacy.fabric.events.FabricVillagerBehaviorHandler;
import com.cesoti2006.villagediplomacy.fabric.events.TradeModifierHandler;
import com.cesoti2006.villagediplomacy.fabric.events.PlacementRestrictionsHandler;
import com.cesoti2006.villagediplomacy.fabric.events.FabricGolemBehaviorHandler;
import com.cesoti2006.villagediplomacy.fabric.events.FabricFireDamageHandler;

public class VillageDiplomacyQuilt implements ModInitializer {

    @Override
    public void onInitialize() {
        VillageDiplomacyConfig.loadConfig();
        VillageDiplomacy.LOGGER.info("Village Diplomacy initializing on Quilt!");

        
        FabricEventHandler eventHandler = new FabricEventHandler();
        eventHandler.registerEvents();

        
        TradeModifierHandler tradeHandler = new TradeModifierHandler();
        tradeHandler.registerEvents();

        
        FabricReputationRestrictionsHandler repRestrictions = new FabricReputationRestrictionsHandler();
        repRestrictions.registerEvents();

        
        FabricPersonalityBehaviorHandler personalityHandler = new FabricPersonalityBehaviorHandler();
        personalityHandler.registerEvents();

        
        FabricVillagerBehaviorHandler villagerBehavior = new FabricVillagerBehaviorHandler();
        villagerBehavior.registerEvents();

        
        PlacementRestrictionsHandler placementHandler = new PlacementRestrictionsHandler();
        placementHandler.registerEvents();

        
        FabricGolemBehaviorHandler golemHandler = new FabricGolemBehaviorHandler();
        golemHandler.registerEvents();

        FabricFireDamageHandler fireHandler = new FabricFireDamageHandler();
        fireHandler.registerEvents();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            DiplomacyCommands.register(dispatcher);
        });
    }
}
