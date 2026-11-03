package com.cesoti2006.villagediplomacy;

import com.cesoti2006.villagediplomacy.commands.DiplomacyCommands;
import com.cesoti2006.villagediplomacy.config.VillageDiplomacyConfig;
import com.cesoti2006.villagediplomacy.diplomacy.GrievanceHandler;
import com.cesoti2006.villagediplomacy.diplomacy.WarDeclarationHandler;
import com.cesoti2006.villagediplomacy.diplomacy.WarManager;
import com.cesoti2006.villagediplomacy.events.FireDamageHandler;
import com.cesoti2006.villagediplomacy.events.PlacementRestrictionsHandler;
import com.cesoti2006.villagediplomacy.events.ReputationRestrictionsHandler;
import com.cesoti2006.villagediplomacy.events.TradeModifierHandler;
import com.cesoti2006.villagediplomacy.events.VillagerBehaviorHandler;
import com.cesoti2006.villagediplomacy.events.VillagerEventHandler;
import com.cesoti2006.villagediplomacy.network.VillageDiplomacyNetwork;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod(VillageDiplomacyMod.MOD_ID)
public class VillageDiplomacyMod {

    public static final String MOD_ID = "villagediplomacy";

    public static VillagerEventHandler eventHandler;

    public VillageDiplomacyMod(IEventBus modEventBus, ModContainer modContainer) {

        org.slf4j.LoggerFactory.getLogger("villagediplomacy").info(
                "Village Diplomacy 1.5.0 inicializando en NeoForge para Minecraft {}",
                net.minecraft.SharedConstants.getCurrentVersion().getName());

        modContainer.registerConfig(ModConfig.Type.COMMON, VillageDiplomacyConfig.SPEC);

        modEventBus.addListener(VillageDiplomacyNetwork::register);

        eventHandler = new VillagerEventHandler();
        NeoForge.EVENT_BUS.register(eventHandler);
        NeoForge.EVENT_BUS.register(new TradeModifierHandler());
        NeoForge.EVENT_BUS.register(VillagerBehaviorHandler.class);
        NeoForge.EVENT_BUS.register(new PlacementRestrictionsHandler());
        NeoForge.EVENT_BUS.register(new ReputationRestrictionsHandler());
        NeoForge.EVENT_BUS.register(FireDamageHandler.class);
        NeoForge.EVENT_BUS.register(GrievanceHandler.class);
        NeoForge.EVENT_BUS.register(WarDeclarationHandler.class);
        NeoForge.EVENT_BUS.register(WarManager.class);
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        DiplomacyCommands.register(event.getDispatcher());
    }
}
