package com.cesoti2006.villagediplomacy.events;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cesoti2006.villagediplomacy.config.VillageDiplomacyConfig;
import com.cesoti2006.villagediplomacy.network.VillageDiplomacyNetwork;
import com.cesoti2006.villagediplomacy.util.InnkeeperHelper;
import com.cesoti2006.villagediplomacy.util.ModLang;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Sends the identity the mod generated for a villager to nearby players so their
 * own nameplates can be drawn. The fingerprint check keeps this cheap: nothing is
 * sent twice for the same villager unless its name, mood or role actually changes.
 */
public final class VillagerIdentitySync {

    private static final int SEND_RADIUS = 40;
    private static final int MAX_PER_PLAYER = 48;
    private static final int INTERVAL_TICKS = 100;

    private static final Map<String, String> SENT = new HashMap<>();

    private static int counter = 0;

    private VillagerIdentitySync() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        VillageDiplomacyConfig config = VillageDiplomacyConfig.INSTANCE;
        if (!config.enableNameplates.get()) return;

        counter++;
        if (counter % INTERVAL_TICKS != 0) return;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                syncPlayer(level, player);
            }
        }

        if (SENT.size() > 8192) SENT.clear();
    }

    private static void syncPlayer(ServerLevel level, ServerPlayer player) {
        List<Villager> nearby = level.getEntitiesOfClass(Villager.class,
                player.getBoundingBox().inflate(SEND_RADIUS),
                villager -> villager.isAlive() && !villager.isRemoved());

        int sent = 0;
        for (Villager villager : nearby) {
            if (sent >= MAX_PER_PLAYER) break;

            String name = ModLang.plainName(villager);
            String mood = ModLang.moodKey(villager);
            boolean innkeeper = InnkeeperHelper.isInnkeeper(level, villager);

            String fingerprint = (name == null ? "" : name)
                    + "|" + (mood == null ? "" : mood)
                    + "|" + innkeeper;
            String key = player.getUUID() + "|" + villager.getUUID();

            if (fingerprint.equals(SENT.get(key))) continue;
            SENT.put(key, fingerprint);

            VillageDiplomacyNetwork.sendVillagerIdentity(player, villager.getId(), villager.getUUID(),
                    name == null ? "" : name, mood == null ? "" : mood, innkeeper);
            sent++;
        }
    }
}
