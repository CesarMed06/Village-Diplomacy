package com.cesoti2006.villagediplomacy.diplomacy;

import com.cesoti2006.villagediplomacy.config.VillageDiplomacyConfig;
import com.cesoti2006.villagediplomacy.data.VillageGrievanceData;
import com.cesoti2006.villagediplomacy.data.VillageGrievanceData.Grievance;
import com.cesoti2006.villagediplomacy.data.VillageGrievanceData.GrievanceType;
import com.cesoti2006.villagediplomacy.data.VillageGrievanceData.Recorded;
import com.cesoti2006.villagediplomacy.data.VillageRelationshipData;
import com.cesoti2006.villagediplomacy.util.InnkeeperHelper;
import com.cesoti2006.villagediplomacy.util.ModLang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class GrievanceHandler {

    private static final String PLAYER_PREFIX = "player:";

    private static final int EXPIRY_CHECK_TICKS = 200;

    private static final long RUMOUR_COOLDOWN_MS = 90000L;

    private static final Map<UUID, Rumour> lastRumour = new HashMap<>();

    private static final class Rumour {
        private final long announcedAt;
        private final long grievanceId;
        private final int count;

        private Rumour(long announcedAt, long grievanceId, int count) {
            this.announcedAt = announcedAt;
            this.grievanceId = grievanceId;
            this.count = count;
        }
    }

    private static int tickCounter = 0;

    private GrievanceHandler() {
    }

    public static boolean isEnabled() {
        return VillageDiplomacyConfig.INSTANCE.enableVillageRelations.get();
    }

    public static void record(ServerLevel level, BlockPos villagePos, GrievanceType type, ServerPlayer player) {
        if (!isEnabled()) return;

        VillageRelationshipData relations = VillageRelationshipData.get(level);
        relations.registerVillage(villagePos, level);

        recordById(level, relations.getVillageId(villagePos), type, player);
    }

    public static void recordById(ServerLevel level, String villageId, GrievanceType type, ServerPlayer player) {
        if (!isEnabled() || villageId == null || villageId.isEmpty()) return;

        VillageGrievanceData data = VillageGrievanceData.get(level);
        long today = VillageGrievanceData.dayOf(level);
        String culpritId = playerCulprit(player);
        Recorded recorded = data.record(villageId, type, culpritId,
                ModLang.stripFormatting(player.getName().getString()), today);

        if (!recorded.isNew) return;

        VillageRelationshipData relations = VillageRelationshipData.get(level);
        sendWoundNotice(player, relations.getVillageName(villageId), recorded.grievance, today);
    }

    private static void sendWoundNotice(ServerPlayer player, String villageName, Grievance grievance,
                                       long today) {
        player.sendSystemMessage(prefix().append(Component.translatable("villagediplomacy.grievance.notice",
                ModLang.villageName(villageName),
                Component.translatable(grievance.type.getTranslationKey()).withStyle(ChatFormatting.YELLOW),
                timeComponent(grievance, today)).withStyle(ChatFormatting.GRAY)));
    }

    private static void sendForgottenNotice(ServerPlayer player, String villageName, Grievance grievance) {
        player.sendSystemMessage(prefix().append(Component.translatable("villagediplomacy.grievance.forgotten",
                ModLang.villageName(villageName),
                Component.translatable(grievance.type.getTranslationKey()).withStyle(ChatFormatting.YELLOW))
                .withStyle(ChatFormatting.GRAY)));
    }

    public static MutableComponent prefix() {
        return Component.literal("[").withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.translatable("villagediplomacy.prefix.diplomacy").withStyle(ChatFormatting.GOLD))
                .append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY));
    }

    public static MutableComponent timeComponent(Grievance grievance, long today) {
        long daysLeft = grievance.daysLeft(today);
        if (daysLeft < 0) {
            return Component.translatable("villagediplomacy.grievance.forever")
                    .withStyle(ChatFormatting.GRAY);
        }
        return Component.translatable("villagediplomacy.grievance.days_left",
                Component.literal(String.valueOf(daysLeft)).withStyle(ChatFormatting.AQUA))
                .withStyle(ChatFormatting.GRAY);
    }

    public static void onServerTick(net.minecraft.server.MinecraftServer server) {
        if (!isEnabled()) return;

        tickCounter++;
        if (tickCounter % EXPIRY_CHECK_TICKS != 0) return;

        lastRumour.entrySet().removeIf(entry ->
                System.currentTimeMillis() - entry.getValue().announcedAt > RUMOUR_COOLDOWN_MS * 4L);

        ServerLevel overworld = server.overworld();
        long today = VillageGrievanceData.dayOf(overworld);
        VillageGrievanceData data = VillageGrievanceData.get(overworld);
        VillageRelationshipData relations = VillageRelationshipData.get(overworld);

        for (Grievance grievance : data.collectNewlyExpired(today)) {
            UUID culprit = playerOf(grievance.culpritId);
            if (culprit == null) continue;

            ServerPlayer player = server.getPlayerList().getPlayer(culprit);
            if (player == null) continue;

            sendForgottenNotice(player, relations.getVillageName(grievance.villageId), grievance);
        }
    }

    public static void innkeeperRumour(ServerPlayer player, ServerLevel level, BlockPos villagePos) {
        if (!isEnabled()) return;

        UUID playerId = player.getUUID();
        long now = System.currentTimeMillis();

        VillageRelationshipData relations = VillageRelationshipData.get(level);
        String villageId = relations.getVillageId(villagePos);

        long today = VillageGrievanceData.dayOf(level);
        VillageGrievanceData memory = VillageGrievanceData.get(level);
        Grievance grievance = memory.latestActive(villageId, today);
        if (grievance == null) return;

        for (Grievance candidate : memory.getActive(villageId, today)) {
            if (candidate.type.severity() != VillageGrievanceData.Severity.HISTORIC) {
                grievance = candidate;
                break;
            }
        }

        Rumour previous = lastRumour.get(playerId);
        if (previous != null && previous.grievanceId == grievance.id
                && previous.count == grievance.count
                && now - previous.announcedAt < RUMOUR_COOLDOWN_MS) return;

        String keyPrefix = "villagediplomacy.rumor." + grievance.type.name().toLowerCase(Locale.ROOT);

        int variants = ModLang.variantCount(keyPrefix, 0);
        if (variants <= 0) return;

        Villager innkeeper = InnkeeperHelper.findInnkeeper(level, villagePos);
        MutableComponent speaker = innkeeper != null
                ? ModLang.namedPrefix(innkeeper.getName().getString())
                : ModLang.genericVillagerPrefix();

        int index = level.getRandom().nextInt(variants);
        player.sendSystemMessage(speaker.append(Component.translatable(keyPrefix + "." + index)
                .withStyle(ChatFormatting.GRAY)));

        lastRumour.put(playerId, new Rumour(now, grievance.id, grievance.count));
    }

    public static String playerCulprit(ServerPlayer player) {
        return PLAYER_PREFIX + player.getUUID();
    }

    public static UUID playerOf(String culpritId) {
        if (culpritId == null || !culpritId.startsWith(PLAYER_PREFIX)) return null;
        try {
            return UUID.fromString(culpritId.substring(PLAYER_PREFIX.length()));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
