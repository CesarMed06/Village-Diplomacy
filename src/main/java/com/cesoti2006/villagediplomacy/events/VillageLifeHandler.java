package com.cesoti2006.villagediplomacy.events;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.cesoti2006.villagediplomacy.config.VillageDiplomacyConfig;
import com.cesoti2006.villagediplomacy.data.VillageDetector;
import com.cesoti2006.villagediplomacy.data.VillageRelationshipData;
import com.cesoti2006.villagediplomacy.data.VillageReputationData;
import com.cesoti2006.villagediplomacy.util.ModLang;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class VillageLifeHandler {

    private static final int TICK_INTERVAL = 20;
    private static final long TICKS_PER_DAY = 24000L;

    private static final long MORNING = 1000L;
    private static final long NOON = 6000L;
    private static final long EVENING = 11500L;
    private static final long WINDOW = 200L;

    private static final int MARKET_CYCLE_DAYS = 7;
    private static final int MARKET_MIN_REPUTATION = 100;
    private static final double EVENT_RADIUS = 128.0;

    private static final int BELL_SEARCH_RADIUS = 64;
    private static final int CHAT_VILLAGER_RADIUS = 12;
    private static final double CHAT_MAX_PAIR_DISTANCE_SQR = 36.0;
    private static final long CHAT_PAIR_COOLDOWN_MS = 120_000L;
    private static final long CHAT_PLAYER_COOLDOWN_MS = 45_000L;

    private static final String[] CHAT_TOPICS = { "weather", "work", "gossip", "player", "war" };

    private static int tickCounter = 0;

    private static final Map<String, Long> bellDays = new HashMap<>();
    private static final Map<String, Long> pairChats = new HashMap<>();
    private static final Map<UUID, Long> playerChats = new HashMap<>();

    private VillageLifeHandler() {
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        VillageDiplomacyConfig config = VillageDiplomacyConfig.INSTANCE;
        if (!config.enableVillageLife.get()) return;

        tickCounter++;
        if (tickCounter % TICK_INTERVAL != 0) return;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            tickLevel(level, config);
        }
    }

    private static void tickLevel(ServerLevel level, VillageDiplomacyConfig config) {
        List<ServerPlayer> players = level.players();
        if (players.isEmpty()) return;

        long dayTime = level.getDayTime();
        long day = dayTime / TICKS_PER_DAY;
        long timeOfDay = dayTime % TICKS_PER_DAY;

        for (ServerPlayer player : players) {
            Optional<BlockPos> found = VillageDetector.findNearestVillage(
                    level, player.blockPosition(), config.villageEnterRadius.get());
            if (found.isEmpty()) continue;

            BlockPos center = found.get();
            if (!VillageDetector.isPlayerAtVillageLevel(level, player, center)) continue;
            if (!VillageDetector.hasLivingVillagers(level, center, 64)) continue;

            VillageRelationshipData relations = VillageRelationshipData.get(level);
            relations.registerVillage(center, level);
            String villageId = relations.getVillageId(center);

            if (config.enableBellRhythm.get() || config.enableMarketDay.get()) {
                tickBell(level, relations, villageId, center, day, timeOfDay, config);
            }
            if (config.enableVillagerChat.get()) {
                tickConversation(level, player, center);
            }
        }
    }

    private static void tickBell(ServerLevel level, VillageRelationshipData relations, String villageId,
                                 BlockPos center, long day, long timeOfDay, VillageDiplomacyConfig config) {
        int phase;
        if (isWithinWindow(timeOfDay, MORNING)) {
            phase = 0;
        } else if (isWithinWindow(timeOfDay, NOON)) {
            phase = 1;
        } else if (isWithinWindow(timeOfDay, EVENING)) {
            phase = 2;
        } else {
            return;
        }

        String key = level.dimension().location() + "|" + villageId + "|" + day + "|" + phase;
        if (bellDays.containsKey(key)) return;
        bellDays.put(key, day);
        if (bellDays.size() > 600) {
            bellDays.entrySet().removeIf(entry -> entry.getValue() < day - 1);
        }

        BlockPos bell = findBell(level, center);
        if (bell == null) return;

        boolean celebration = config.enableMarketDay.get()
                && phase == 1
                && day % MARKET_CYCLE_DAYS == 0
                && hasCelebrant(level, center);

        level.playSound(null, bell, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 2.0F, 0.9F);

        if (celebration) {
            celebrate(level, relations, villageId, center, bell);
        }
    }

    private static void celebrate(ServerLevel level, VillageRelationshipData relations, String villageId,
                                  BlockPos center, BlockPos bell) {
        level.playSound(null, bell, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 2.5F, 1.15F);
        level.playSound(null, bell, SoundEvents.VILLAGER_CELEBRATE, SoundSource.NEUTRAL, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.FIREWORK,
                bell.getX() + 0.5D, bell.getY() + 2.0D, bell.getZ() + 0.5D,
                24, 2.0D, 1.0D, 2.0D, 0.12D);

        VillageReputationData data = VillageReputationData.get(level);
        Component message = Component.translatable("villagediplomacy.life.market_day",
                ModLang.villageName(relations.getVillageName(villageId))).withStyle(ChatFormatting.GRAY);

        for (ServerPlayer player : level.players()) {
            if (player.blockPosition().distSqr(center) > EVENT_RADIUS * EVENT_RADIUS) continue;
            if (data.getReputation(player.getUUID(), center) < MARKET_MIN_REPUTATION) continue;
            player.sendSystemMessage(message);
        }

        for (Villager villager : level.getEntitiesOfClass(Villager.class,
                new AABB(center).inflate(48.0D, 24.0D, 48.0D),
                v -> !v.isBaby() && v.isAlive() && !v.isRemoved())) {
            if (level.getRandom().nextInt(3) != 0) continue;
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    villager.getX(), villager.getY() + 1.2D, villager.getZ(),
                    3, 0.4D, 0.4D, 0.4D, 0.0D);
        }
    }

    private static boolean hasCelebrant(ServerLevel level, BlockPos center) {
        VillageReputationData data = VillageReputationData.get(level);
        double radiusSqr = EVENT_RADIUS * EVENT_RADIUS;
        for (ServerPlayer player : level.players()) {
            if (player.blockPosition().distSqr(center) > radiusSqr) continue;
            if (data.getReputation(player.getUUID(), center) >= MARKET_MIN_REPUTATION) return true;
        }
        return false;
    }

    private static void tickConversation(ServerLevel level, ServerPlayer player, BlockPos center) {
        long now = System.currentTimeMillis();
        UUID playerId = player.getUUID();

        Long lastPlayer = playerChats.get(playerId);
        if (lastPlayer != null && now - lastPlayer < CHAT_PLAYER_COOLDOWN_MS) return;
        if (level.getRandom().nextInt(4) != 0) return;

        List<Villager> nearby = level.getEntitiesOfClass(Villager.class,
                player.getBoundingBox().inflate(CHAT_VILLAGER_RADIUS),
                v -> !v.isBaby() && v.isAlive() && !v.isRemoved());
        if (nearby.size() < 2) return;

        Villager first = nearby.get(level.getRandom().nextInt(nearby.size()));
        Villager second = null;
        double nearest = Double.MAX_VALUE;
        for (Villager candidate : nearby) {
            if (candidate == first) continue;
            double distance = candidate.distanceToSqr(first);
            if (distance < nearest) {
                nearest = distance;
                second = candidate;
            }
        }
        if (second == null || nearest > CHAT_MAX_PAIR_DISTANCE_SQR) return;

        String pairKey = pairKey(first.getUUID(), second.getUUID());
        Long lastPair = pairChats.get(pairKey);
        if (lastPair != null && now - lastPair < CHAT_PAIR_COOLDOWN_MS) return;

        pairChats.put(pairKey, now);
        playerChats.put(playerId, now);
        if (pairChats.size() > 400) {
            pairChats.entrySet().removeIf(entry -> now - entry.getValue() > CHAT_PAIR_COOLDOWN_MS * 2);
        }
        if (playerChats.size() > 200) {
            playerChats.entrySet().removeIf(entry -> now - entry.getValue() > CHAT_PLAYER_COOLDOWN_MS * 4);
        }

        String topic = CHAT_TOPICS[level.getRandom().nextInt(CHAT_TOPICS.length)];

        first.getLookControl().setLookAt(second, 30.0F, 30.0F);
        second.getLookControl().setLookAt(first, 30.0F, 30.0F);

        level.playSound(null, first.blockPosition(), SoundEvents.VILLAGER_AMBIENT,
                SoundSource.NEUTRAL, 0.9F, 0.9F + level.getRandom().nextFloat() * 0.2F);
        level.playSound(null, second.blockPosition(), SoundEvents.VILLAGER_AMBIENT,
                SoundSource.NEUTRAL, 0.8F, 1.0F + level.getRandom().nextFloat() * 0.2F);

        ModLang.sendDialog(player, first,
                "villagediplomacy.life.chat." + topic + "." + level.getRandom().nextInt(5),
                player.getScoreboardName());
        ModLang.sendDialog(player, second, "villagediplomacy.life.reply." + topic);
    }

    private static String pairKey(UUID first, UUID second) {
        return first.compareTo(second) <= 0 ? first + "|" + second : second + "|" + first;
    }

    private static boolean isWithinWindow(long timeOfDay, long target) {
        return timeOfDay >= target && timeOfDay < target + WINDOW;
    }

    private static BlockPos findBell(ServerLevel level, BlockPos center) {
        BlockPos meeting = level.getPoiManager()
                .findClosest(poi -> poi.is(PoiTypes.MEETING), center, BELL_SEARCH_RADIUS, PoiManager.Occupancy.ANY)
                .orElse(null);
        if (meeting == null) return null;
        if (level.getBlockState(meeting).is(Blocks.BELL)) return meeting;

        for (BlockPos pos : BlockPos.betweenClosed(meeting.offset(-8, -4, -8), meeting.offset(8, 4, 8))) {
            if (level.isLoaded(pos) && level.getBlockState(pos).is(Blocks.BELL)) {
                return pos.immutable();
            }
        }
        return null;
    }
}
