package com.cesoti2006.villagediplomacy.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.cesoti2006.villagediplomacy.config.VillageDiplomacyConfig;
import com.cesoti2006.villagediplomacy.data.VillageDetector;
import com.cesoti2006.villagediplomacy.data.VillageRelationshipData;
import com.cesoti2006.villagediplomacy.data.VillageReputationData;
import com.cesoti2006.villagediplomacy.util.InnkeeperHelper;
import com.cesoti2006.villagediplomacy.util.ModLang;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
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
    private static final int GATHER_MAX_VILLAGERS = 1;
    private static final long BELL_CALL_MS = 26_000L;
    private static final int BELL_MAX_ATTEMPTS = 3;
    private static final double BELL_REACH_HORIZONTAL = 3.0D;
    private static final double BELL_REACH_WET = 4.5D;
    private static final int BELL_REACH_BELOW = 2;
    private static final int BELL_REACH_ABOVE = 2;

    private static final long DANGER_COOLDOWN_MS = 30_000L;
    private static final int DANGER_RADIUS = 18;
    private static final int DANGER_MONSTER_RADIUS = 14;

    private static final int GOLEM_SEARCH_RADIUS = 14;
    private static final long GOLEM_PLAYER_COOLDOWN_MS = 85_000L;
    private static final long GOLEM_REPLY_MIN_MS = 2_200L;
    private static final int GOLEM_TALK_CHANCE = 6;

    private static final int CHAT_VILLAGER_RADIUS = 12;
    private static final double CHAT_MAX_PAIR_DISTANCE_SQR = 36.0;
    private static final long CHAT_PAIR_COOLDOWN_MS = 240_000L;
    private static final long CHAT_PLAYER_COOLDOWN_MS = 50_000L;
    private static final int CHAT_CHANCE = 5;

    private static final double INN_LEASH_SQR = 18.0 * 18.0;
    private static final double INN_TELEPORT_SQR = 48.0 * 48.0;

    private static final String[] CHAT_TOPICS = { "weather", "work", "gossip", "player", "war" };
    private static final int[][] GATHER_SPOTS = {
            { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 }, { 1, 1 }, { -1, -1 }
    };

    private static int tickCounter = 0;

    private static final Map<String, String> bellRungTags = new HashMap<>();
    private static final Map<String, BellCall> bellCalls = new HashMap<>();
    private static final Map<String, Long> pairChats = new HashMap<>();
    private static final Map<UUID, Long> playerChats = new HashMap<>();
    private static final List<PendingReply> pendingReplies = new ArrayList<>();
    private static final List<PendingGolemReply> pendingGolemReplies = new ArrayList<>();
    private static final Map<UUID, Long> golemChats = new HashMap<>();
    private static final Map<UUID, InnTracker> innTrackers = new HashMap<>();
    private static final Map<String, Long> dangerChats = new HashMap<>();

    private VillageLifeHandler() {
    }

    private record BellCall(String day, String phase, boolean celebration, long expiresAt, int attempt) {
    }

    private record PendingReply(long dueAt, UUID speaker, UUID listener, String key) {
    }

    private record PendingGolemReply(long dueAt, UUID speaker, UUID listener, String key) {
    }

    private record InnTracker(BlockPos lastPos, long since) {
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

        Set<BlockPos> visitedCenters = new HashSet<>();

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

            visitedCenters.add(center);

            if (bell != null && (config.enableBellRhythm.get() || config.enableMarketDay.get())) {
                tickBell(level, relations, villageId, bellKey, center, bell, day, timeOfDay);
            }
            if (config.enableVillagerChat.get()) {
                tickDanger(level, player, center);
                tickConversation(level, player, center);
                if (config.enableGolemPersonalities.get()) {
                    tickGolems(level, player);
                }
            }
        }

        tickPendingReplies(level);
        tickPendingGolemReplies(level);
        tickInnkeepers(level, visitedCenters);
    }

    private static void tickBell(ServerLevel level, VillageRelationshipData relations, String villageId,
                                 String bellKey, BlockPos center, BlockPos bell, long day, long timeOfDay) {
        long now = System.currentTimeMillis();

        BellCall active = bellCalls.get(bellKey);
        if (active != null && now <= active.expiresAt()) {
            tickGather(level, relations, villageId, bellKey, center, bell, active);
            return;
        }
        if (active != null) {
            bellCalls.remove(bellKey);
            String activeTag = active.day() + "|" + active.phase();
            boolean alreadyRung = activeTag.equals(bellRungTags.get(bellKey));
            if (!alreadyRung && active.attempt() < BELL_MAX_ATTEMPTS) {
                BellCall retry = new BellCall(active.day(), active.phase(), active.celebration(),
                        now + BELL_CALL_MS, active.attempt() + 1);
                bellCalls.put(bellKey, retry);
                tickGather(level, relations, villageId, bellKey, center, bell, retry);
                return;
            }
        }

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
        if (!isWithinWindow(timeOfDay, target)) return;

        String tag = day + "|" + phaseName;
        if (tag.equals(bellRungTags.get(bellKey))) return;

        boolean celebration = VillageDiplomacyConfig.INSTANCE.enableMarketDay.get()
                && phaseName.equals("noon")
                && day % MARKET_CYCLE_DAYS == 0;

        BellCall call = new BellCall(String.valueOf(day), phaseName, celebration, now + BELL_CALL_MS, 0);
        bellCalls.put(bellKey, call);
        if (bellCalls.size() > 200) bellCalls.clear();

        tickGather(level, relations, villageId, bellKey, center, bell, call);
    }

    private static void tickGather(ServerLevel level, VillageRelationshipData relations, String villageId,
                                   String bellKey, BlockPos center, BlockPos bell, BellCall call) {
        List<Villager> nearby = level.getEntitiesOfClass(Villager.class,
                new AABB(bell).inflate(BELL_GATHER_RADIUS, 16.0D, BELL_GATHER_RADIUS),
                v -> !v.isBaby() && v.isAlive() && !v.isRemoved());
        if (nearby.isEmpty()) {
            if (System.currentTimeMillis() > call.expiresAt()) bellCalls.remove(bellKey);
            return;
        }

        Villager ringer = null;
        for (Villager villager : nearby) {
            if (canReachBell(villager, bell)) {
                ringer = villager;
                break;
            }
        }

        if (ringer != null) {
            ringBell(level, relations, villageId, bellKey, center, bell, call, ringer);
            return;
        }

        nearby.sort((a, b) -> Double.compare(a.blockPosition().distSqr(bell), b.blockPosition().distSqr(bell)));

        int assigned = 0;
        for (Villager villager : nearby) {
            if (assigned >= GATHER_MAX_VILLAGERS) break;
            BlockPos spot = spotNearBell(level, bell, assigned);
            if (spot == null) continue;

            villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
                    new WalkTarget(spot, 0.62F, 1));
            if (villager.blockPosition().distSqr(bell) <= 400) {
                villager.getLookControl().setLookAt(bell.getX() + 0.5D, bell.getY() + 0.5D, bell.getZ() + 0.5D);
            }
            assigned++;
        }

        if (System.currentTimeMillis() > call.expiresAt()) bellCalls.remove(bellKey);
    }

    private static void ringBell(ServerLevel level, VillageRelationshipData relations, String villageId,
                                 String bellKey, BlockPos center, BlockPos bell, BellCall call, Villager ringer) {
        ringer.getNavigation().stop();
        ringer.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        ringer.getLookControl().setLookAt(bell.getX() + 0.5D, bell.getY() + 0.5D, bell.getZ() + 0.5D);
        ringer.swing(InteractionHand.MAIN_HAND, true);

        // The server has to send the block event (that is what starts the swing
        // on every client) and the sound itself. Calling onHit alone left the
        // bell silent in game, so both halves are done explicitly here.
        BlockState bellState = level.getBlockState(bell);
        if (bellState.is(Blocks.BELL)) {
            Direction facing = bellState.hasProperty(BellBlock.FACING)
                    ? bellState.getValue(BellBlock.FACING) : Direction.NORTH;
            level.blockEvent(bell, bellState.getBlock(), 1, facing.get3DDataValue());
        }
        level.playSound(null, bell.getX() + 0.5D, bell.getY() + 0.5D, bell.getZ() + 0.5D,
                SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 2.0F, 1.0F);

        bellCalls.remove(bellKey);
        bellRungTags.put(bellKey, call.day() + "|" + call.phase());
        if (bellRungTags.size() > 400) bellRungTags.clear();

        level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                ringer.getX(), ringer.getY() + 1.2D, ringer.getZ(),
                6, 0.35D, 0.4D, 0.35D, 0.0D);

        sendBellMessage(level, relations, villageId, bell,
                "villagediplomacy.life.bell." + call.phase(), ringer);

        if (call.celebration()) celebrate(level, relations, villageId, center, bell);
    }

    /**
     * A villager has to actually stand at the bell: close on both axes and with a
     * clear line of sight. The old generous reach let someone ring a bell hanging
     * ten blocks above their head from inside a house, which looked like nothing
     * happened. Water widens the reach a little so a swimmer can still strike it.
     */
    private static boolean canReachBell(Villager villager, BlockPos bell) {
        BlockPos pos = villager.blockPosition();
        double dx = (pos.getX() + 0.5D) - (bell.getX() + 0.5D);
        double dz = (pos.getZ() + 0.5D) - (bell.getZ() + 0.5D);
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double limit = villager.isInWater() ? BELL_REACH_WET : BELL_REACH_HORIZONTAL;
        int dy = pos.getY() - bell.getY();
        if (horizontal > limit || dy < -BELL_REACH_BELOW || dy > BELL_REACH_ABOVE) return false;
        return seesBell(villager, bell);
    }

    private static boolean seesBell(Villager villager, BlockPos bell) {
        BlockHitResult hit = villager.level().clip(new ClipContext(villager.getEyePosition(),
                Vec3.atCenterOf(bell), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, villager));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(bell);
    }

    private static void sendBellMessage(ServerLevel level, VillageRelationshipData relations, String villageId,
                                        BlockPos bell, String keyPrefix, Villager ringer) {
        int count = ModLang.variantCount(keyPrefix, 3);
        if (count <= 0) return;

        Component message = Component.literal("[").withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.translatable("villagediplomacy.tag.bell").withStyle(ChatFormatting.GOLD))
                .append(Component.literal("] ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.translatable(
                        keyPrefix + "." + level.getRandom().nextInt(count),
                        ModLang.villageName(relations.getVillageName(villageId)),
                        ModLang.displayName(ringer)).withStyle(ChatFormatting.GRAY));

        for (ServerPlayer player : level.players()) {
            if (player.blockPosition().distSqr(bell) <= BELL_MESSAGE_RADIUS * BELL_MESSAGE_RADIUS) {
                player.sendSystemMessage(message);
            }
        }
    }

    private static BlockPos spotNearBell(ServerLevel level, BlockPos bell, int index) {
        for (int attempt = 0; attempt < GATHER_SPOTS.length; attempt++) {
            int[] offset = GATHER_SPOTS[(index + attempt) % GATHER_SPOTS.length];
            for (int dy = 1; dy >= -2; dy--) {
                BlockPos pos = bell.offset(offset[0], dy, offset[1]);
                if (!level.isLoaded(pos)) continue;
                if (!isStandable(level, pos)) continue;
                if (!seesBell(level, pos, bell)) continue;
                return pos;
            }
        }
        return null;
    }

    private static boolean isStandable(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) return false;
        if (!level.getFluidState(pos).isEmpty() || !level.getFluidState(pos.above()).isEmpty()) return false;
        BlockState below = level.getBlockState(pos.below());
        return below.isSolidRender(level, pos.below()) || below.getBlock() instanceof BedBlock;
    }

    private static boolean seesBell(ServerLevel level, BlockPos from, BlockPos bell) {
        Vec3 eye = Vec3.atBottomCenterOf(from).add(0.0D, 1.6D, 0.0D);
        BlockHitResult hit = level.clip(new ClipContext(eye, Vec3.atCenterOf(bell),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(bell);
    }

    private static void tickPendingReplies(ServerLevel level) {
        if (pendingReplies.isEmpty()) return;

        long now = System.currentTimeMillis();
        Iterator<PendingReply> iterator = pendingReplies.iterator();
        while (iterator.hasNext()) {
            PendingReply pending = iterator.next();
            if (pending.dueAt() > now) continue;
            iterator.remove();

            Entity speakerEntity = level.getEntity(pending.speaker());
            Entity listenerEntity = level.getEntity(pending.listener());
            if (!(speakerEntity instanceof Villager speaker)) continue;
            if (!(listenerEntity instanceof Villager listener)) continue;
            if (!speaker.isAlive() || speaker.isRemoved()) continue;
            if (!listener.isAlive() || listener.isRemoved()) continue;
            if (speaker.distanceToSqr(listener) > CHAT_MAX_PAIR_DISTANCE_SQR) continue;

            speaker.getLookControl().setLookAt(listener, 40.0F, 30.0F);
            for (ServerPlayer player : level.players()) {
                // Only deliver the answer to players who can actually see both speakers,
                // otherwise the chat names a villager standing behind a wall.
                if (player.blockPosition().distSqr(speaker.blockPosition()) <= 196
                        && player.blockPosition().distSqr(listener.blockPosition()) <= 196) {
                    ModLang.sendChatLine(player, speaker, listener, pending.key());
                }
            }
        }
    }

    private static void tickConversation(ServerLevel level, ServerPlayer player, BlockPos center) {
        long now = System.currentTimeMillis();
        UUID playerId = player.getUUID();

        Long lastPlayer = playerChats.get(playerId);
        if (lastPlayer != null && now - lastPlayer < CHAT_PLAYER_COOLDOWN_MS) return;
        if (level.getRandom().nextInt(CHAT_CHANCE) != 0) return;

        List<Villager> nearby = level.getEntitiesOfClass(Villager.class,
                player.getBoundingBox().inflate(CHAT_VILLAGER_RADIUS),
                v -> !v.isBaby() && v.isAlive() && !v.isRemoved() && v.distanceToSqr(player) <= 100.0D);
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
        if (Math.abs(first.getY() - second.getY()) > 4.0D) return;

        String topic = pickTopic(level, player, center);
        String family = ModLang.professionFamily(first.getVillagerData().getProfession());
        String mood = ModLang.moodKey(first);

        String lineKey;
        int roll = level.getRandom().nextInt(100);
        if (mood != null && roll < 24) {
            lineKey = ModLang.randomLineKey(level.getRandom(), "villagediplomacy.life.chat.mood." + mood, 4);
        } else if (roll < 62) {
            lineKey = ModLang.randomLineKey(level.getRandom(), "villagediplomacy.life.chat.fam." + family, 6);
        } else {
            lineKey = ModLang.randomLineKey(level.getRandom(), "villagediplomacy.life.chat." + topic, 4);
        }
        if (lineKey == null) return;

        String replyFamily = ModLang.professionFamily(second.getVillagerData().getProfession());
        String replyKey;
        if (level.getRandom().nextInt(100) < 55) {
            replyKey = ModLang.randomLineKey(level.getRandom(),
                    "villagediplomacy.life.reply.fam." + replyFamily, 4);
        } else {
            replyKey = "villagediplomacy.life.reply." + topic;
        }
        if (replyKey == null) return;

        String pairKey = pairKey(first.getUUID(), second.getUUID());
        Long lastPair = pairChats.get(pairKey);
        if (lastPair != null && now - lastPair < CHAT_PAIR_COOLDOWN_MS) return;

        pairChats.put(pairKey, now);
        playerChats.put(playerId, now);
        if (pairChats.size() > 400) {
            pairChats.entrySet().removeIf(entry -> now - entry.getValue() > CHAT_PAIR_COOLDOWN_MS);
        }
        if (playerChats.size() > 200) {
            playerChats.entrySet().removeIf(entry -> now - entry.getValue() > CHAT_PLAYER_COOLDOWN_MS * 2);
        }

        first.getLookControl().setLookAt(second, 40.0F, 30.0F);
        second.getLookControl().setLookAt(first, 40.0F, 30.0F);

        Component playerName = ModLang.playerDisplayName(player);

        ModLang.sendChatLine(player, first, second, lineKey, playerName);

        long delay = 2400L + level.getRandom().nextInt(2200);
        pendingReplies.add(new PendingReply(now + delay, second.getUUID(), first.getUUID(), replyKey));
        if (pendingReplies.size() > 64) pendingReplies.remove(0);
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

    /**
     * Villagers notice monsters near the player and say something about it, even
     * when only one of them is around. Before this a zombie could stand in the
     * middle of the square and nobody reacted at all.
     */
    private static void tickDanger(ServerLevel level, ServerPlayer player, BlockPos center) {
        long now = System.currentTimeMillis();

        // Enemy covers hostile modded mobs too, not only vanilla Monster subclasses.
        List<Entity> monsters = level.getEntitiesOfClass(Entity.class,
                player.getBoundingBox().inflate(DANGER_RADIUS, 8.0D, DANGER_RADIUS),
                m -> m instanceof Enemy && m.isAlive() && !m.isRemoved()
                        && m.distanceToSqr(player) <= (double) (DANGER_RADIUS * DANGER_RADIUS));
        if (monsters.isEmpty()) return;

        Entity threat = monsters.get(level.getRandom().nextInt(monsters.size()));
        String category = dangerCategory(threat);

        // One cooldown per kind of threat: a zombie no longer silences the
        // warning for the creeper standing next to it.
        String cooldownKey = player.getUUID() + "|" + category;
        Long last = dangerChats.get(cooldownKey);
        if (last != null && now - last < DANGER_COOLDOWN_MS) return;

        List<Villager> witnesses = level.getEntitiesOfClass(Villager.class,
                threat.getBoundingBox().inflate(DANGER_MONSTER_RADIUS),
                v -> !v.isBaby() && v.isAlive() && !v.isRemoved()
                        && v.distanceToSqr(player) <= 196.0D
                        && v.distanceToSqr(threat) <= (double) (DANGER_MONSTER_RADIUS * DANGER_MONSTER_RADIUS));
        if (witnesses.isEmpty()) return;

        witnesses.sort((a, b) -> Double.compare(a.distanceToSqr(threat), b.distanceToSqr(threat)));
        Villager speaker = witnesses.get(0);
        Villager listener = null;
        for (Villager candidate : witnesses) {
            if (candidate == speaker) continue;
            if (candidate.distanceToSqr(speaker) <= CHAT_MAX_PAIR_DISTANCE_SQR) {
                listener = candidate;
                break;
            }
        }

        String lineKey = ModLang.randomLineKey(level.getRandom(),
                "villagediplomacy.life.chat.danger." + category, 3);
        if (lineKey == null) return;
        Component monsterName = Component.translatable(threat.getType().getDescriptionId());

        dangerChats.put(cooldownKey, now);
        if (dangerChats.size() > 256) dangerChats.clear();

        speaker.getLookControl().setLookAt(threat, 50.0F, 40.0F);
        playerChats.put(player.getUUID(), now);

        if (listener != null) {
            listener.getLookControl().setLookAt(threat, 50.0F, 40.0F);
            ModLang.sendChatLine(player, speaker, listener, lineKey, monsterName);
            pairChats.put(pairKey(speaker.getUUID(), listener.getUUID()), now);
            String replyKey = ModLang.randomLineKey(level.getRandom(),
                    "villagediplomacy.life.reply.danger", 3);
            if (replyKey != null) {
                pendingReplies.add(new PendingReply(now + 1_800L + level.getRandom().nextInt(1_400),
                        listener.getUUID(), speaker.getUUID(), replyKey));
            }
        } else {
            ModLang.sendChatLine(player, speaker, null, lineKey, monsterName);
        }
    }

    /**
     * Every kind of monster gets its own warning, so a creeper hissing at the
     * door no longer triggers the same line as a zombie on the far wall. The mob
     * name is passed into the translation, which keeps the line specific without
     * needing one key per entity.
     */
    private static String dangerCategory(Entity threat) {
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(threat.getType());
        String id = key.getPath();
        if (id.contains("zombie") || id.contains("skeleton") || id.contains("phantom")
                || id.contains("drowned") || id.contains("husk") || id.contains("stray")
                || id.contains("wither")) {
            return "undead";
        }
        if (id.contains("creeper")) return "creeper";
        if (id.contains("pillager") || id.contains("vindicator") || id.contains("evoker")
                || id.contains("illusioner") || id.contains("ravager") || id.contains("witch")) {
            return "raider";
        }
        if (id.contains("spider")) return "spider";
        if (id.contains("slime")) return "slime";
        if (id.contains("enderman")) return "enderman";
        if (!"minecraft".equals(key.getNamespace())) return "strange";
        return "other";
    }

    /** Ring state is per server run: a new world or a reload starts from a clean slate. */
    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel().isClientSide()) return;
        bellCalls.clear();
        bellRungTags.clear();
        pairChats.clear();
        playerChats.clear();
        pendingReplies.clear();
        pendingGolemReplies.clear();
        golemChats.clear();
        innTrackers.clear();
        dangerChats.clear();
    }

    private static void tickGolems(ServerLevel level, ServerPlayer player) {
        long now = System.currentTimeMillis();
        Long last = golemChats.get(player.getUUID());
        if (last != null && now - last < GOLEM_PLAYER_COOLDOWN_MS) return;
        if (level.getRandom().nextInt(GOLEM_TALK_CHANCE) != 0) return;

        List<IronGolem> golems = level.getEntitiesOfClass(IronGolem.class,
                player.getBoundingBox().inflate(GOLEM_SEARCH_RADIUS, 8.0D, GOLEM_SEARCH_RADIUS),
                g -> g.isAlive() && !g.isRemoved() && g.distanceToSqr(player) <= 196.0D);
        if (golems.isEmpty()) return;

        IronGolem golem = golems.get(level.getRandom().nextInt(golems.size()));

        Villager villager = null;
        double nearest = Double.MAX_VALUE;
        for (Villager candidate : level.getEntitiesOfClass(Villager.class,
                golem.getBoundingBox().inflate(8.0D),
                v -> !v.isBaby() && v.isAlive() && !v.isRemoved() && v.distanceToSqr(player) <= 196.0D)) {
            double distance = candidate.distanceToSqr(golem);
            if (distance < nearest) {
                nearest = distance;
                villager = candidate;
            }
        }

        IronGolem partner = null;
        for (IronGolem candidate : golems) {
            if (candidate == golem) continue;
            if (candidate.distanceToSqr(golem) <= 64.0D) {
                partner = candidate;
                break;
            }
        }

        if (villager == null && partner == null) return;

        if (villager != null) {
            String key = ModLang.randomLineKey(level.getRandom(), "villagediplomacy.golem.to_villager", 5);
            if (key == null) return;
            golem.getLookControl().setLookAt(villager, 40.0F, 30.0F);
            villager.getLookControl().setLookAt(golem, 40.0F, 30.0F);
            ModLang.sendEntityChatLine(player, golem, villager, key);
            golemChats.put(player.getUUID(), now);
            if (level.getRandom().nextInt(10) < 7) {
                String reply = ModLang.randomLineKey(level.getRandom(),
                        "villagediplomacy.golem.reply.villager", 4);
                if (reply != null) {
                    addGolemReply(now + GOLEM_REPLY_MIN_MS + level.getRandom().nextInt(1800),
                            villager.getUUID(), golem.getUUID(), reply);
                }
            }
        } else {
            String key = ModLang.randomLineKey(level.getRandom(), "villagediplomacy.golem.to_golem", 5);
            if (key == null) return;
            golem.getLookControl().setLookAt(partner, 40.0F, 30.0F);
            partner.getLookControl().setLookAt(golem, 40.0F, 30.0F);
            ModLang.sendEntityChatLine(player, golem, partner, key);
            golemChats.put(player.getUUID(), now);
            if (level.getRandom().nextInt(10) < 6) {
                String reply = ModLang.randomLineKey(level.getRandom(),
                        "villagediplomacy.golem.reply.golem", 3);
                if (reply != null) {
                    addGolemReply(now + GOLEM_REPLY_MIN_MS + level.getRandom().nextInt(1500),
                            partner.getUUID(), golem.getUUID(), reply);
                }
            }
        }

        level.playSound(null, golem.getX(), golem.getY(), golem.getZ(),
                SoundEvents.IRON_GOLEM_STEP, SoundSource.NEUTRAL, 0.55F, 0.85F);
        if (golemChats.size() > 200) golemChats.clear();
    }

    private static void addGolemReply(long dueAt, UUID speaker, UUID listener, String key) {
        pendingGolemReplies.add(new PendingGolemReply(dueAt, speaker, listener, key));
        if (pendingGolemReplies.size() > 48) pendingGolemReplies.remove(0);
    }

    private static void tickPendingGolemReplies(ServerLevel level) {
        if (pendingGolemReplies.isEmpty()) return;

        long now = System.currentTimeMillis();
        Iterator<PendingGolemReply> iterator = pendingGolemReplies.iterator();
        while (iterator.hasNext()) {
            PendingGolemReply pending = iterator.next();
            if (pending.dueAt() > now) continue;
            iterator.remove();

            Entity speakerEntity = level.getEntity(pending.speaker());
            Entity listenerEntity = level.getEntity(pending.listener());
            if (speakerEntity == null || listenerEntity == null) continue;
            if (!speakerEntity.isAlive() || speakerEntity.isRemoved()) continue;
            if (!listenerEntity.isAlive() || listenerEntity.isRemoved()) continue;
            if (speakerEntity.distanceToSqr(listenerEntity) > 64.0D) continue;

            for (ServerPlayer player : level.players()) {
                if (player.blockPosition().distSqr(speakerEntity.blockPosition()) <= 196
                        && player.blockPosition().distSqr(listenerEntity.blockPosition()) <= 196) {
                    ModLang.sendEntityChatLine(player, speakerEntity, listenerEntity, pending.key());
                }
            }
        }
    }

    private static void tickInnkeepers(ServerLevel level, Set<BlockPos> centers) {
        if (centers.isEmpty()) return;
        if (level.getGameTime() % 100L >= TICK_INTERVAL) return;

        for (BlockPos center : centers) {
            BlockPos anchor = InnkeeperHelper.getInnAnchor(level, center);
            if (anchor == null) continue;

            Villager innkeeper = InnkeeperHelper.findInnkeeper(level, center);
            if (innkeeper == null) continue;

            BlockPos current = innkeeper.blockPosition();
            double distance = current.distSqr(anchor);
            UUID id = innkeeper.getUUID();

            if (distance <= INN_LEASH_SQR) {
                innTrackers.remove(id);
                continue;
            }

            long now = System.currentTimeMillis();
            InnTracker tracker = innTrackers.get(id);
            if (tracker == null || !tracker.lastPos().equals(current)) {
                tracker = new InnTracker(current, now);
                innTrackers.put(id, tracker);
            }
            if (innTrackers.size() > 256) innTrackers.clear();

            // A perch above water or a ledge the villager cannot climb leaves the
            // navigation spinning in place; after a few seconds of no progress the
            // innkeeper is placed back at the inn instead of pacing forever.
            boolean stuck = now - tracker.since() > 8000L && distance > 144.0D;
            boolean farAway = distance > INN_TELEPORT_SQR;
            BlockPos goal = nextTo(level, anchor);
            if ((farAway || stuck) && goal != null && level.isLoaded(anchor)) {
                innkeeper.teleportTo(goal.getX() + 0.5D, goal.getY(), goal.getZ() + 0.5D);
                innkeeper.getNavigation().stop();
                innTrackers.put(id, new InnTracker(goal, now));
                continue;
            }

            innkeeper.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
                    new WalkTarget(goal != null ? goal : anchor, 0.55F, 2));
        }
    }

    /**
     * A real standing spot next to the inn bed: on the bed or at its side, never a
     * two-block pocket carved inside a wall. The innkeeper used to be put back
     * inside a wall or over water and stayed there without a path out.
     */
    private static BlockPos nextTo(ServerLevel level, BlockPos anchor) {
        List<BlockPos> candidates = new ArrayList<>();
        candidates.add(anchor.above());

        BlockState state = level.getBlockState(anchor);
        if (state.getBlock() instanceof BedBlock) {
            Direction facing = state.getValue(BedBlock.FACING);
            candidates.add(anchor.relative(facing.getClockWise()));
            candidates.add(anchor.relative(facing.getCounterClockWise()));
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            candidates.add(anchor.relative(direction));
        }
        candidates.add(anchor.offset(1, 0, 1));
        candidates.add(anchor.offset(1, 0, -1));
        candidates.add(anchor.offset(-1, 0, 1));
        candidates.add(anchor.offset(-1, 0, -1));

        for (BlockPos pos : candidates) {
            if (!level.isLoaded(pos)) continue;
            if (!isStandable(level, pos)) continue;
            if (!isOpenAround(level, pos)) continue;
            return pos;
        }
        return null;
    }

    /** Rejects one-block holes inside a wall that would leave the villager stuck. */
    private static boolean isOpenAround(ServerLevel level, BlockPos pos) {
        int open = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos side = pos.relative(direction);
            if (!level.getBlockState(side).isSuffocating(level, side)) open++;
        }
        return open >= 3;
    }

    private static void celebrate(ServerLevel level, VillageRelationshipData relations, String villageId,
                                  BlockPos center, BlockPos bell) {
        level.playSound(null, bell, SoundEvents.VILLAGER_CELEBRATE, SoundSource.NEUTRAL, 1.2F, 1.0F);
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
