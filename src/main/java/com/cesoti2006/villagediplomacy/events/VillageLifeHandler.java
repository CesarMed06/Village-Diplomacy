package com.cesoti2006.villagediplomacy.events;

import java.util.ArrayList;
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

    private static final int BELL_SEARCH_RADIUS = 96;
    private static final int VILLAGER_SEARCH_RADIUS = 96;
    private static final int BELL_MESSAGE_RADIUS = 48;
    private static final int BELL_GATHER_RADIUS = 40;
    private static final int GATHER_MAX_VILLAGERS = 4;
    private static final long GATHER_DURATION_MS = 12_000L;

    private static final int CHAT_VILLAGER_RADIUS = 12;
    private static final double CHAT_MAX_PAIR_DISTANCE_SQR = 36.0;
    private static final long CHAT_PAIR_COOLDOWN_MS = 60_000L;
    private static final long CHAT_PLAYER_COOLDOWN_MS = 20_000L;

    private static final String[] CHAT_TOPICS = { "weather", "work", "gossip", "player", "war" };
    private static final int[][] GATHER_SPOTS = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };

    private static int tickCounter = 0;

    private static final Map<String, Long> bellLastTime = new HashMap<>();
    private static final Map<String, Long> bellGatherUntil = new HashMap<>();
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
            BlockPos playerPos = player.blockPosition();
            Optional<BlockPos> found = VillageDetector.findNearestVillage(
                    level, playerPos, Math.max(96, config.villageEnterRadius.get()));
            if (found.isEmpty()) continue;

            BlockPos center = found.get();
            if (!VillageDetector.isPlayerAtVillageLevel(level, player, center)) continue;
            if (!VillageDetector.hasLivingVillagers(level, playerPos, VILLAGER_SEARCH_RADIUS)) continue;

            BlockPos bell = findBell(level, playerPos, center);

            VillageRelationshipData relations = VillageRelationshipData.get(level);
            relations.registerVillage(center, level);
            String villageId = relations.getVillageId(center);
            String bellKey = level.dimension().location() + "|" + villageId;

            if (bell != null) {
                if (config.enableBellRhythm.get() || config.enableMarketDay.get()) {
                    tickBell(level, relations, villageId, bellKey, center, bell, day, timeOfDay, config);
                }
                tickGather(level, bellKey, bell);
            }
            if (config.enableVillagerChat.get()) {
                tickConversation(level, player, center);
            }
        }
    }

    private static void tickBell(ServerLevel level, VillageRelationshipData relations, String villageId,
                                 String bellKey, BlockPos center, BlockPos bell, long day, long timeOfDay,
                                 VillageDiplomacyConfig config) {
        long target;
        String phaseName;
        if (isWithinWindow(timeOfDay, MORNING)) {
            target = MORNING;
            phaseName = "morning";
        } else if (isWithinWindow(timeOfDay, NOON)) {
            target = NOON;
            phaseName = "noon";
        } else if (isWithinWindow(timeOfDay, EVENING)) {
            target = EVENING;
            phaseName = "evening";
        } else {
            return;
        }

        Long previous = bellLastTime.put(bellKey, timeOfDay);
        if (bellLastTime.size() > 400) bellLastTime.clear();
        if (previous != null && isWithinWindow(previous, target)) return;

        boolean celebration = config.enableMarketDay.get()
                && phaseName.equals("noon")
                && day % MARKET_CYCLE_DAYS == 0
                && hasCelebrant(level, center, bell);

        // Vanilla block event: shakes the bell and plays the long toll for everyone nearby.
        level.blockEvent(bell, Blocks.BELL, 1, 0);
        bellGatherUntil.put(bellKey, System.currentTimeMillis() + GATHER_DURATION_MS);

        sendBellMessage(level, relations, villageId, bell, "villagediplomacy.life.bell." + phaseName);
        if (celebration) celebrate(level, relations, villageId, center, bell);
    }

    private static void sendBellMessage(ServerLevel level, VillageRelationshipData relations, String villageId,
                                        BlockPos bell, String keyPrefix) {
        int count = ModLang.variantCount(keyPrefix, 3);
        if (count <= 0) return;

        Component message = Component.translatable(
                keyPrefix + "." + level.getRandom().nextInt(count),
                ModLang.villageName(relations.getVillageName(villageId))).withStyle(ChatFormatting.GRAY);

        for (ServerPlayer player : level.players()) {
            if (player.blockPosition().distSqr(bell) <= BELL_MESSAGE_RADIUS * BELL_MESSAGE_RADIUS) {
                player.sendSystemMessage(message);
            }
        }
    }

    private static void tickGather(ServerLevel level, String bellKey, BlockPos bell) {
        Long until = bellGatherUntil.get(bellKey);
        if (until == null) return;
        if (System.currentTimeMillis() > until) {
            bellGatherUntil.remove(bellKey);
            return;
        }

        List<Villager> nearby = level.getEntitiesOfClass(Villager.class,
                new AABB(bell).inflate(BELL_GATHER_RADIUS, 16.0D, BELL_GATHER_RADIUS),
                v -> !v.isBaby() && v.isAlive() && !v.isRemoved());
        if (nearby.isEmpty()) return;

        nearby.sort((a, b) -> Double.compare(a.blockPosition().distSqr(bell), b.blockPosition().distSqr(bell)));

        int gathered = 0;
        for (Villager villager : nearby) {
            if (gathered >= GATHER_MAX_VILLAGERS) break;
            int[] offset = GATHER_SPOTS[gathered % GATHER_SPOTS.length];
            BlockPos spot = spotNearBell(level, bell, offset[0], offset[1]);
            if (spot == null) {
                gathered++;
                continue;
            }
            if (villager.blockPosition().distSqr(spot) <= 2.25D) {
                villager.getLookControl().setLookAt(bell.getX() + 0.5D, bell.getY() + 0.5D, bell.getZ() + 0.5D);
            } else {
                villager.getNavigation().moveTo(spot.getX() + 0.5D, spot.getY(), spot.getZ() + 0.5D, 0.9D);
            }
            gathered++;
        }
    }

    private static BlockPos spotNearBell(ServerLevel level, BlockPos bell, int dx, int dz) {
        BlockPos base = bell.offset(dx, 0, dz);
        for (int dy = 1; dy >= -7; dy--) {
            BlockPos pos = base.offset(0, dy, 0);
            if (!level.isLoaded(pos)) return null;
            if (level.getBlockState(pos).isAir()
                    && level.getBlockState(pos.above()).isAir()
                    && level.getBlockState(pos.below()).isSolidRender(level, pos.below())) {
                return pos;
            }
        }
        return null;
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
            if (player.blockPosition().distSqr(bell) > EVENT_RADIUS * EVENT_RADIUS) continue;
            if (data.getReputation(player.getUUID(), center) < MARKET_MIN_REPUTATION) continue;
            player.sendSystemMessage(message);
        }

        for (Villager villager : level.getEntitiesOfClass(Villager.class,
                new AABB(bell).inflate(48.0D, 24.0D, 48.0D),
                v -> !v.isBaby() && v.isAlive() && !v.isRemoved())) {
            if (level.getRandom().nextInt(3) != 0) continue;
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    villager.getX(), villager.getY() + 1.2D, villager.getZ(),
                    3, 0.4D, 0.4D, 0.4D, 0.0D);
        }
    }

    private static boolean hasCelebrant(ServerLevel level, BlockPos center, BlockPos bell) {
        VillageReputationData data = VillageReputationData.get(level);
        double radiusSqr = EVENT_RADIUS * EVENT_RADIUS;
        for (ServerPlayer player : level.players()) {
            if (player.blockPosition().distSqr(bell) > radiusSqr) continue;
            if (data.getReputation(player.getUUID(), center) >= MARKET_MIN_REPUTATION) return true;
        }
        return false;
    }

    private static void tickConversation(ServerLevel level, ServerPlayer player, BlockPos center) {
        long now = System.currentTimeMillis();
        UUID playerId = player.getUUID();

        Long lastPlayer = playerChats.get(playerId);
        if (lastPlayer != null && now - lastPlayer < CHAT_PLAYER_COOLDOWN_MS) return;
        if (level.getRandom().nextInt(3) != 0) return;

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

        String topic = pickTopic(level, player, center);
        int count = ModLang.variantCount("villagediplomacy.life.chat." + topic, 4);
        if (count <= 0) return;

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

        first.getLookControl().setLookAt(second, 30.0F, 30.0F);
        second.getLookControl().setLookAt(first, 30.0F, 30.0F);
        first.getNavigation().stop();
        second.getNavigation().stop();

        level.playSound(null, first.blockPosition(), SoundEvents.VILLAGER_AMBIENT,
                SoundSource.NEUTRAL, 0.9F, 0.9F + level.getRandom().nextFloat() * 0.2F);
        level.playSound(null, second.blockPosition(), SoundEvents.VILLAGER_AMBIENT,
                SoundSource.NEUTRAL, 0.8F, 1.0F + level.getRandom().nextFloat() * 0.2F);

        ModLang.sendChatLine(player, first, second,
                "villagediplomacy.life.chat." + topic + "." + level.getRandom().nextInt(count));
        ModLang.sendChatLine(player, second, first, "villagediplomacy.life.reply." + topic);
    }

    private static String pickTopic(ServerLevel level, ServerPlayer player, BlockPos center) {
        List<String> topics = new ArrayList<>(List.of(CHAT_TOPICS));

        if (level.isRaining() || level.isThundering()) topics.add("storm");

        long timeOfDay = level.getDayTime() % TICKS_PER_DAY;
        if (timeOfDay >= 13000L && timeOfDay < 23000L) topics.add("night");

        int reputation = VillageReputationData.get(level).getReputation(player.getUUID(), center);
        if (reputation >= 300) {
            topics.add("player_good");
        } else if (reputation < 0) {
            topics.add("player_bad");
        }

        return topics.get(level.getRandom().nextInt(topics.size()));
    }

    private static String pairKey(UUID first, UUID second) {
        return first.compareTo(second) <= 0 ? first + "|" + second : second + "|" + first;
    }

    private static boolean isWithinWindow(long timeOfDay, long target) {
        return timeOfDay >= target && timeOfDay < target + WINDOW;
    }

    private static BlockPos findBell(ServerLevel level, BlockPos playerPos, BlockPos center) {
        BlockPos meeting = VillageDetector.findMeeting(level, playerPos, BELL_SEARCH_RADIUS).orElse(null);
        if (meeting == null) {
            meeting = VillageDetector.findMeeting(level, center, BELL_SEARCH_RADIUS).orElse(null);
        }
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
