package com.cesoti2006.villagediplomacy;

import com.cesoti2006.villagediplomacy.commands.DiplomacyCommands;
import com.cesoti2006.villagediplomacy.events.VillagerEventHandler;
import com.cesoti2006.villagediplomacy.events.TradeModifierHandler;
import com.cesoti2006.villagediplomacy.events.VillagerBehaviorHandler;
import com.cesoti2006.villagediplomacy.network.VillageDiplomacyNetwork;
import com.cesoti2006.villagediplomacy.personality.PersonalityBehaviorHandler;
import com.cesoti2006.villagediplomacy.personality.VillagerActivityBehavior;
import com.cesoti2006.villagediplomacy.events.ReputationRestrictionsHandler;
import com.cesoti2006.villagediplomacy.events.FireDamageHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import com.cesoti2006.villagediplomacy.config.VillageDiplomacyConfig;

@Mod("villagediplomacy")
public class VillageDiplomacyMod {

    public static VillagerEventHandler eventHandler;

    public VillageDiplomacyMod(FMLJavaModLoadingContext context) {
        
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, VillageDiplomacyConfig.SPEC);
        
        IEventBus modBus = context.getModEventBus();
        modBus.addListener(this::commonSetup);

        eventHandler = new VillagerEventHandler();
        MinecraftForge.EVENT_BUS.register(eventHandler);
        MinecraftForge.EVENT_BUS.register(new TradeModifierHandler());
        MinecraftForge.EVENT_BUS.register(new VillagerBehaviorHandler());
        MinecraftForge.EVENT_BUS.register(new PersonalityBehaviorHandler());
        MinecraftForge.EVENT_BUS.register(new VillagerActivityBehavior());
        MinecraftForge.EVENT_BUS.register(new ReputationRestrictionsHandler());
        MinecraftForge.EVENT_BUS.register(FireDamageHandler.class);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(VillageDiplomacyNetwork::register);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        DiplomacyCommands.register(event.getServer().getCommands().getDispatcher());
    }
}
