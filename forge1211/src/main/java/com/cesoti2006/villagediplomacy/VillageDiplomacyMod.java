package com.cesoti2006.villagediplomacy;

import com.cesoti2006.villagediplomacy.commands.DiplomacyCommands;
import com.cesoti2006.villagediplomacy.diplomacy.GrievanceHandler;
import com.cesoti2006.villagediplomacy.diplomacy.WarDeclarationHandler;
import com.cesoti2006.villagediplomacy.diplomacy.WarManager;
import com.cesoti2006.villagediplomacy.events.VillagerEventHandler;
import com.cesoti2006.villagediplomacy.events.TradeModifierHandler;
import com.cesoti2006.villagediplomacy.events.VillagerBehaviorHandler;
import com.cesoti2006.villagediplomacy.network.VillageDiplomacyNetwork;
import com.cesoti2006.villagediplomacy.events.PlacementRestrictionsHandler;
import com.cesoti2006.villagediplomacy.events.ReputationRestrictionsHandler;
import com.cesoti2006.villagediplomacy.events.FireDamageHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import com.cesoti2006.villagediplomacy.config.VillageDiplomacyConfig;

@Mod("villagediplomacy")
public class VillageDiplomacyMod {

    public static VillagerEventHandler eventHandler;

    @SuppressWarnings("removal")
    public VillageDiplomacyMod(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext context) {
        init(context.getModEventBus());
    }

    @SuppressWarnings("removal")
    private void init(IEventBus modEventBus) {

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, VillageDiplomacyConfig.SPEC);

        if (modEventBus != null) {
            modEventBus.addListener(this::commonSetup);

            if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) {
                modEventBus.addListener(
                        com.cesoti2006.villagediplomacy.events.VillageHUDHandler::registerLayer);
            }
        } else {
            VillageDiplomacyNetwork.register();
        }

        eventHandler = new VillagerEventHandler();
        MinecraftForge.EVENT_BUS.register(eventHandler);
        MinecraftForge.EVENT_BUS.register(new TradeModifierHandler());
        MinecraftForge.EVENT_BUS.register(VillagerBehaviorHandler.class);
        MinecraftForge.EVENT_BUS.register(new PlacementRestrictionsHandler());
        MinecraftForge.EVENT_BUS.register(new ReputationRestrictionsHandler());
        MinecraftForge.EVENT_BUS.register(FireDamageHandler.class);
        MinecraftForge.EVENT_BUS.register(GrievanceHandler.class);
        MinecraftForge.EVENT_BUS.register(WarDeclarationHandler.class);
        MinecraftForge.EVENT_BUS.register(WarManager.class);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(VillageDiplomacyNetwork::register);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        DiplomacyCommands.register(event.getDispatcher());
    }
}
