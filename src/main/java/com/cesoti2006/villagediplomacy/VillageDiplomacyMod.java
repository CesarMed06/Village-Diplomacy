package com.cesoti2006.villagediplomacy;

import com.cesoti2006.villagediplomacy.commands.DiplomacyCommands;
import com.cesoti2006.villagediplomacy.events.VillagerEventHandler;
import com.cesoti2006.villagediplomacy.events.VillageHUDHandler;
import com.cesoti2006.villagediplomacy.events.TradeModifierHandler;
import com.cesoti2006.villagediplomacy.events.VillagerBehaviorHandler;
import com.cesoti2006.villagediplomacy.personality.PersonalityBehaviorHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("villagediplomacy")
public class VillageDiplomacyMod {

    public static VillagerEventHandler eventHandler; // Instancia estática accesible

    public VillageDiplomacyMod() {
        // Register event handlers
        eventHandler = new VillagerEventHandler();
        MinecraftForge.EVENT_BUS.register(eventHandler);
        MinecraftForge.EVENT_BUS.register(new VillageHUDHandler());
        MinecraftForge.EVENT_BUS.register(new TradeModifierHandler());
        MinecraftForge.EVENT_BUS.register(new VillagerBehaviorHandler());
        MinecraftForge.EVENT_BUS.register(new PersonalityBehaviorHandler());
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        DiplomacyCommands.register(event.getServer().getCommands().getDispatcher());
    }
}
