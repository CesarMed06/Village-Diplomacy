package com.cesoti2006.villagediplomacy.diplomacy;

import com.cesoti2006.villagediplomacy.data.VillageDiplomacyData;
import com.cesoti2006.villagediplomacy.data.VillageDiplomacyData.PairState;
import com.cesoti2006.villagediplomacy.data.VillageGrievanceData;
import com.cesoti2006.villagediplomacy.data.VillageRelationshipData;
import com.cesoti2006.villagediplomacy.data.VillageWarData;
import com.cesoti2006.villagediplomacy.data.VillageWarData.WarRecord;
import com.cesoti2006.villagediplomacy.data.VillagerPersonalityData;
import com.cesoti2006.villagediplomacy.personality.EmotionalState;
import com.cesoti2006.villagediplomacy.personality.VillagerPersonality;
import com.cesoti2006.villagediplomacy.util.ModLang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class WarManager {

    private static final int TICK_INTERVAL = 20;

    private static final int SCORE_INTERVAL = 200;

    private static final int SCORE_STEP = 2;

    private static final long MOBILIZATION_TICKS = 24000L;

    private static final long ALARM_TICKS = 400L;

    private static final long ALARM_INTERVAL = 100L;

    private static final long RINGER_STRIKE_TICKS = 100L;

    private static final float FALLBACK_VOLUME = 4.0F;

    private static final int BELL_SEARCH_RADIUS = 64;

    private static final int BELL_PLAYER_RADIUS = 24;

    private static final double RINGER_RADIUS = 32.0;

    private static final double RINGER_SPEAK_RADIUS = 6.0;

    private static final double STAND_TOLERANCE = 0.75;

    private static final double RINGER_GIVE_UP_RADIUS = 96.0;

    private static final float RINGER_SPEED = 1.2F;

    private static final float RINGER_WALK_SPEED = 0.6F;

    private static final double RINGER_HURRY_DISTANCE = 16.0;

    private static final int STAND_RADIUS = 5;

    private static final int[] STAND_OFFSETS = {-1, 0, -2, -3};

    private static final double CHANT_RADIUS = 32.0;

    private static final long RINGER_SPEECH_COOLDOWN = 600L;

    private static final long BELL_DUTY_TICKS = 100L;

    private static final int SHELTER_INTERVAL = 200;

    private static final double SHELTER_RADIUS = 48.0;

    private static final double STRENGTH_RADIUS = 128.0;

    private static final double NOTIFY_RADIUS = 128.0;

    private static final Map<BlockPos, BlockPos> BELL_CACHE = new HashMap<>();

    private static final Map<BlockPos, Long> LAST_RINGER_SPEECH = new HashMap<>();

    private static final Map<BlockPos, BellDuty> BELL_DUTY = new HashMap<>();

    private static int tickCounter = 0;

    private WarManager() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (!GrievanceHandler.isEnabled()) return;

        ServerLevel level = event.getServer().overworld();

        driveBellDuties(level);

        tickCounter++;
        if (tickCounter % TICK_INTERVAL != 0) return;

        long now = level.getGameTime();
        boolean scoreNow = tickCounter % SCORE_INTERVAL == 0;
        boolean shelterNow = tickCounter % SHELTER_INTERVAL == 0;

        VillageDiplomacyData diplomacy = VillageDiplomacyData.get(level);
        VillageWarData wars = VillageWarData.get(level);
        VillageRelationshipData relations = VillageRelationshipData.get(level);

        for (Map.Entry<String, PairState> entry : new ArrayList<>(diplomacy.getAllPairs().entrySet())) {
            String[] parts = entry.getKey().split(":");
            if (parts.length != 2) continue;
            if (!entry.getValue().state.isWar()) continue;

            WarRecord existing = wars.getWar(parts[0], parts[1]);

            if (existing != null && entry.getValue().declaredDay > existing.startedDay) {
                wars.endWar(parts[0], parts[1]);
                existing = null;
            }
            if (existing == null) {
                mobilise(level, wars, relations, parts[0], parts[1], entry.getValue(), now);
            }
        }

        for (Map.Entry<String, WarRecord> entry : new ArrayList<>(wars.getAllWars().entrySet())) {
            String[] parts = entry.getKey().split(":");
            if (parts.length != 2) continue;
            WarRecord war = entry.getValue();

            DiplomaticState current = diplomacy.getState(parts[0], parts[1]);
            if (!current.isWar()) {
                wars.endWar(parts[0], parts[1]);
                SkirmishManager.forget(entry.getKey());
                if (current.isPeaceful() && current != DiplomaticState.TRUCE) {
                    broadcast(level, relations, war.attacker, war.defender,
                            message("villagediplomacy.war.ended",
                                    ModLang.villageName(relations.getVillageName(war.attacker)),
                                    ModLang.villageName(relations.getVillageName(war.defender))));
                }
                continue;
            }

            boolean mobilised = now < war.mobilizationUntil;
            if (mobilised && now < war.alarmUntil && now >= war.nextAlarm) {
                alarm(level, relations, war.attacker, war.defender);
                war.nextAlarm = now + ALARM_INTERVAL;
                wars.markDirty();
            }
            if (mobilised && shelterNow) {
                shelter(level, relations, war.attacker);
                shelter(level, relations, war.defender);
            }
            if (scoreNow) updateScore(level, wars, relations, war);

            if (mobilised && SkirmishManager.tick(level, relations, entry.getKey(), war, now)) {
                wars.markDirty();
            }
        }
    }

    private static void mobilise(ServerLevel level, VillageWarData wars, VillageRelationshipData relations,
                                 String villageA, String villageB, PairState pair, long now) {
        String attacker = pair.warAggressor != null && !pair.warAggressor.isEmpty()
                ? pair.warAggressor : villageA;
        String defender = attacker.equals(villageA) ? villageB : villageA;

        WarRecord war = wars.startWar(villageA, villageB, attacker, defender,
                VillageGrievanceData.dayOf(level));

        SkirmishManager.restart(VillageDiplomacyData.pairKey(villageA, villageB), now);
        war.mobilizationUntil = now + MOBILIZATION_TICKS;
        war.alarmUntil = now + ALARM_TICKS;
        war.nextAlarm = now;
        war.attackerStrength = measuredStrengthOrZero(level, relations, attacker);
        war.attackerBaseline = war.attackerStrength;
        war.defenderStrength = measuredStrengthOrZero(level, relations, defender);
        war.defenderBaseline = war.defenderStrength;
        wars.markDirty();

        alarm(level, relations, attacker, defender);
        broadcast(level, relations, attacker, defender,
                message("villagediplomacy.war.mobilization",
                        ModLang.villageName(relations.getVillageName(attacker)),
                        ModLang.villageName(relations.getVillageName(defender))));
    }

    private static void alarm(ServerLevel level, VillageRelationshipData relations,
                              String village1, String village2) {
        String pairKey = VillageDiplomacyData.pairKey(village1, village2);
        ringBell(level, relations, village1, pairKey);
        ringBell(level, relations, village2, pairKey);
    }

    private static void ringBell(ServerLevel level, VillageRelationshipData relations,
                                 String villageId, String pairKey) {
        BlockPos center = relations.getVillagePosition(villageId);
        if (center == null) return;

        BlockPos bell = findBell(level, center);
        if (bell == null) {
            level.playSound(null, center, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, FALLBACK_VOLUME, 1.0F);
            return;
        }

        if (raiseAlarm(level, bell, level.getGameTime(), pairKey)) return;
        strike(level, bell);
    }

    private static void strike(ServerLevel level, BlockPos bell) {
        BlockState state = level.getBlockState(bell);
        if (state.getBlock() instanceof BellBlock bellBlock && bellBlock.attemptToRing(level, bell, null)) return;
        level.playSound(null, bell, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, FALLBACK_VOLUME, 1.0F);
    }

    private static BlockPos findBell(ServerLevel level, BlockPos center) {
        BlockPos cached = BELL_CACHE.get(center);
        if (cached != null && level.getBlockState(cached).is(Blocks.BELL)) return cached;

        BlockPos poi = level.getPoiManager()
                .findClosest(holder -> holder.is(PoiTypes.MEETING), center, BELL_SEARCH_RADIUS,
                        PoiManager.Occupancy.ANY)
                .orElse(null);
        if (poi != null) {
            BELL_CACHE.put(center, poi);
            return poi;
        }

        for (ServerPlayer player : level.players()) {

            if (player.blockPosition().distSqr(center) > STRENGTH_RADIUS * STRENGTH_RADIUS) continue;
            BlockPos around = scanBell(level, player.blockPosition(), BELL_PLAYER_RADIUS);
            if (around != null) {
                BELL_CACHE.put(center, around);
                return around;
            }
        }
        return null;
    }

    private static BlockPos scanBell(ServerLevel level, BlockPos origin, int radius) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos closest = null;
        double best = Double.MAX_VALUE;
        for (int x = -radius; x <= radius; x++) {
            for (int y = -6; y <= 6; y++) {
                for (int z = -radius; z <= radius; z++) {
                    pos.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    if (!level.getBlockState(pos).is(Blocks.BELL)) continue;
                    double distance = origin.distSqr(pos);
                    if (distance < best) {
                        best = distance;
                        closest = pos.immutable();
                    }
                }
            }
        }
        return closest;
    }

    private static boolean raiseAlarm(ServerLevel level, BlockPos bell, long now, String pairKey) {
        AABB area = new AABB(bell).inflate(RINGER_RADIUS);
        VillagerPersonalityData personalities = VillagerPersonalityData.get(level);

        Villager ringer = assignedRinger(level, bell);
        if (ringer != null) {

            BELL_DUTY.get(bell).until = now + BELL_DUTY_TICKS;
        } else {
            ringer = nearestAdult(level, bell);
            if (ringer != null) {
                BELL_DUTY.put(bell, new BellDuty(ringer.getUUID(), now + BELL_DUTY_TICKS, pairKey));
            }
        }

        if (ringer != null) {

            setEmotion(personalities, ringer, EmotionalState.SCARED);
            walkToBell(level, bell, BELL_DUTY.get(bell), ringer, now);
        }

        for (Villager villager : level.getEntitiesOfClass(Villager.class, area)) {
            if (ringer != null && villager.getUUID().equals(ringer.getUUID())) continue;

            setEmotion(personalities, villager, EmotionalState.SCARED);
        }

        return ringer != null;
    }

    private static final class BellDuty {

        private final UUID ringer;
        private final String pairKey;

        private BlockPos stand;

        private Float lookYaw;
        private Float lookPitch;

        private long until;

        private boolean arrived;

        private long lastStrike = Long.MIN_VALUE / 2;

        private BellDuty(UUID ringer, long until, String pairKey) {
            this.ringer = ringer;
            this.until = until;
            this.pairKey = pairKey;
        }
    }

    private static void driveBellDuties(ServerLevel level) {
        if (BELL_DUTY.isEmpty()) return;
        long now = level.getGameTime();
        VillageDiplomacyData diplomacy = VillageDiplomacyData.get(level);
        List<String> endedPairs = new ArrayList<>();

        for (Map.Entry<BlockPos, BellDuty> entry : new ArrayList<>(BELL_DUTY.entrySet())) {
            BlockPos bell = entry.getKey();
            BellDuty duty = entry.getValue();
            Villager ringer = level.getEntity(duty.ringer) instanceof Villager villager ? villager : null;
            String[] pairVillages = duty.pairKey.split(":");
            boolean warEnded = diplomacy.peekPair(pairVillages[0], pairVillages[1]) == null
                    || !diplomacy.peekPair(pairVillages[0], pairVillages[1]).state.isWar();
            boolean lost = warEnded || ringer == null || !ringer.isAlive() || now > duty.until
                    || ringer.distanceToSqr(Vec3.atCenterOf(bell))
                            > RINGER_GIVE_UP_RADIUS * RINGER_GIVE_UP_RADIUS;
            if (lost) {
                if (ringer != null) release(ringer);
                BELL_DUTY.remove(bell);
                if (warEnded) endedPairs.add(duty.pairKey);
                continue;
            }
            walkToBell(level, bell, duty, ringer, now);
        }
        for (String pairKey : endedPairs) SkirmishManager.forget(pairKey);
    }

    private static Villager assignedRinger(ServerLevel level, BlockPos bell) {
        BellDuty duty = BELL_DUTY.get(bell);
        if (duty == null) return null;
        if (!(level.getEntity(duty.ringer) instanceof Villager villager) || !villager.isAlive()) {
            BELL_DUTY.remove(bell);
            return null;
        }
        if (villager.distanceToSqr(Vec3.atCenterOf(bell))
                > RINGER_GIVE_UP_RADIUS * RINGER_GIVE_UP_RADIUS) {
            release(villager);
            BELL_DUTY.remove(bell);
            return null;
        }
        return villager;
    }

    private static Villager nearestAdult(ServerLevel level, BlockPos bell) {
        Vec3 center = Vec3.atCenterOf(bell);
        Villager best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Villager villager : level.getEntitiesOfClass(Villager.class,
                new AABB(bell).inflate(RINGER_RADIUS), v -> !v.isBaby() && v.isAlive())) {
            double distance = villager.distanceToSqr(center);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = villager;
            }
        }
        return best;
    }

    private static void walkToBell(ServerLevel level, BlockPos bell, BellDuty duty, Villager ringer,
                                   long now) {
        Vec3 bellCenter = Vec3.atCenterOf(bell);
        ringer.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(bell));
        if (duty.stand == null) duty.stand = standSpot(level, bell);

        if (!atPost(ringer, bell, duty.stand)) {
            BlockPos target = duty.stand != null ? duty.stand : bell;

            ringer.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
                    new WalkTarget(target, ringerSpeed(ringer, target), 0));

            if (ringer.distanceToSqr(bellCenter) <= RINGER_SPEAK_RADIUS * RINGER_SPEAK_RADIUS) {
                speakAtBell(level, bell, duty, ringer, now);
            }
            return;
        }

        ringer.getNavigation().stop();
        ringer.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
                new WalkTarget(ringer.blockPosition(), RINGER_SPEED, 0));
        face(ringer, bellCenter, duty);

        if (now - duty.lastStrike >= RINGER_STRIKE_TICKS) {
            strike(level, bell);
            duty.lastStrike = now;
        }
        speakAtBell(level, bell, duty, ringer, now);
    }

    private static float ringerSpeed(Villager ringer, BlockPos target) {
        double dx = ringer.getX() - (target.getX() + 0.5D);
        double dz = ringer.getZ() - (target.getZ() + 0.5D);
        return dx * dx + dz * dz > RINGER_HURRY_DISTANCE * RINGER_HURRY_DISTANCE
                ? RINGER_SPEED : RINGER_WALK_SPEED;
    }

    private static boolean atPost(Villager ringer, BlockPos bell, BlockPos stand) {

        if (Math.abs(ringer.getX() - (bell.getX() + 0.5D)) < 0.6D
                && Math.abs(ringer.getZ() - (bell.getZ() + 0.5D)) < 0.6D) return false;
        BlockPos target = stand != null ? stand : bell;
        double dx = ringer.getX() - (target.getX() + 0.5D);
        double dz = ringer.getZ() - (target.getZ() + 0.5D);
        if (dx * dx + dz * dz > STAND_TOLERANCE * STAND_TOLERANCE) return false;
        double dy = ringer.getY() - target.getY();
        return dy >= -1.0D && dy <= 1.5D;
    }

    private static void face(Villager ringer, Vec3 target, BellDuty duty) {
        if (duty.lookYaw == null) {
            double dx = target.x - ringer.getX();
            double dz = target.z - ringer.getZ();
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            float pitch = (float) (-(Mth.atan2(target.y - ringer.getEyeY(),
                    Math.max(horizontal, 1.0E-4D)) * (180.0D / Math.PI)));
            duty.lookYaw = horizontal < 1.0E-4D
                    ? ringer.getYRot()
                    : (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
            duty.lookPitch = Mth.clamp(pitch, -80.0F, 80.0F);
        }

        float yaw = duty.lookYaw;
        float pitch = duty.lookPitch;

        ringer.setYRot(yaw);
        ringer.setYHeadRot(yaw);
        ringer.yBodyRot = yaw;
        ringer.setXRot(pitch);

        ringer.getLookControl().setLookAt(target.x, target.y, target.z, 45.0F, 90.0F);
    }

    private static void speakAtBell(ServerLevel level, BlockPos bell, BellDuty duty, Villager ringer,
                                    long now) {
        if (duty.arrived) return;
        duty.arrived = true;

        Long previous = LAST_RINGER_SPEECH.get(bell);
        if (previous != null && now - previous < RINGER_SPEECH_COOLDOWN) return;

        VillagerPersonality personality = VillagerPersonalityData.get(level).getPersonality(ringer.getUUID());
        String name = personality != null && personality.getCustomName() != null
                ? personality.getCustomName() : ringer.getName().getString();

        String ringerKey = "villagediplomacy.war.bell_ringer";
        int ringerVariants = ModLang.variantCount(ringerKey, 3);
        MutableComponent line = ModLang.namedPrefix(name)

                .append(Component.translatable(ringerKey + "." + level.getRandom().nextInt(ringerVariants))
                        .withStyle(ChatFormatting.RED));

        String chantKey = "villagediplomacy.war.bell_chant";
        int chantVariants = ModLang.variantCount(chantKey, 2);
        MutableComponent chant = Component
                .translatable(chantKey + "." + level.getRandom().nextInt(chantVariants))
                .withStyle(ChatFormatting.YELLOW);

        if (sendNearBell(level, bell, line) + sendNearBell(level, bell, chant) > 0) {
            LAST_RINGER_SPEECH.put(bell, now);
        }
    }

    private static void release(Villager villager) {
        villager.getNavigation().stop();
        villager.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        villager.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
    }

    private static BlockPos standSpot(ServerLevel level, BlockPos bell) {
        List<Direction> columns = new ArrayList<>();
        BlockState state = level.getBlockState(bell);
        if (state.hasProperty(BellBlock.FACING)) {

            Direction front = state.getValue(BellBlock.FACING).getOpposite();
            if (front.getAxis().isHorizontal()) {
                columns.add(front);
                columns.add(front.getClockWise());
                columns.add(front.getCounterClockWise());
            }
        }

        for (Direction horizontal : Direction.Plane.HORIZONTAL) {
            if (!columns.contains(horizontal)) columns.add(horizontal);
        }

        for (Direction column : columns) {
            BlockPos foot = bell.relative(column);
            for (int dy : STAND_OFFSETS) {
                BlockPos pos = foot.offset(0, dy, 0);
                if (isStandable(level, pos)) return pos;
            }
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int radius = 1; radius <= STAND_RADIUS; radius++) {
            for (int dy : STAND_OFFSETS) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                        pos.set(bell.getX() + dx, bell.getY() + dy, bell.getZ() + dz);
                        if (isStandable(level, pos)) return pos.immutable();
                    }
                }
            }
        }
        return null;
    }

    private static boolean isStandable(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) return false;
        if (!level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()) return false;
        return !level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty();
    }

    private static void setEmotion(VillagerPersonalityData personalities, Villager villager,
                                   EmotionalState emotion) {
        VillagerPersonality personality = personalities.getPersonality(villager.getUUID());
        if (personality != null) personality.setCurrentEmotion(emotion);
    }

    private static int measuredStrengthOrZero(ServerLevel level, VillageRelationshipData relations,
                                              String villageId) {
        Integer here = measuredStrength(level, relations, villageId);
        return here != null ? here : 0;
    }

    private static int sendNearBell(ServerLevel level, BlockPos bell, MutableComponent text) {
        int heard = 0;
        for (ServerPlayer player : level.players()) {
            if (player.blockPosition().distSqr(bell) <= CHANT_RADIUS * CHANT_RADIUS) {
                player.sendSystemMessage(text);
                heard++;
            }
        }
        return heard;
    }

    private static void shelter(ServerLevel level, VillageRelationshipData relations, String villageId) {
        BlockPos center = relations.getVillagePosition(villageId);
        if (center == null) return;

        double maxDistance = SHELTER_RADIUS * SHELTER_RADIUS;
        for (Villager villager : level.getEntitiesOfClass(Villager.class,
                columnArea(level, center, SHELTER_RADIUS * 2.0))) {
            if (onBellDuty(villager)) continue;
            double dx = villager.getX() - (center.getX() + 0.5);
            double dz = villager.getZ() - (center.getZ() + 0.5);
            if (dx * dx + dz * dz <= maxDistance) continue;
            villager.getNavigation().moveTo(center.getX() + 0.5, villager.getY(), center.getZ() + 0.5, 1.0D);
        }
    }

    private static boolean onBellDuty(Villager villager) {
        for (BellDuty duty : BELL_DUTY.values()) {
            if (duty.ringer.equals(villager.getUUID())) return true;
        }
        return false;
    }

    public static WarRecord activeWar(ServerLevel level, String villageId) {
        if (villageId == null) return null;
        VillageDiplomacyData diplomacy = VillageDiplomacyData.get(level);
        VillageWarData wars = VillageWarData.get(level);
        for (Map.Entry<String, PairState> entry : diplomacy.getPairsInvolving(villageId).entrySet()) {
            if (!entry.getValue().state.isWar()) continue;
            String[] parts = entry.getKey().split(":");
            if (parts.length != 2) continue;
            WarRecord war = wars.getWar(parts[0], parts[1]);
            if (war != null) return war;
        }
        return null;
    }

    public static String ringerNameAt(ServerLevel level, BlockPos bell) {
        BellDuty duty = BELL_DUTY.get(bell);
        if (duty == null) return null;
        if (!(level.getEntity(duty.ringer) instanceof Villager villager)) return null;
        VillagerPersonality personality = VillagerPersonalityData.get(level)
                .getPersonality(villager.getUUID());
        return personality != null && personality.getCustomName() != null
                ? personality.getCustomName() : villager.getName().getString();
    }

    private static void updateScore(ServerLevel level, VillageWarData wars,
                                    VillageRelationshipData relations, WarRecord war) {
        Integer attackerNow = measuredStrength(level, relations, war.attacker);
        Integer defenderNow = measuredStrength(level, relations, war.defender);
        if (attackerNow == null && defenderNow == null) return;

        if (attackerNow == null || defenderNow == null) {
            boolean attackerLost = attackerNow != null && war.attackerBaseline > 0
                    && attackerNow < war.attackerBaseline;
            boolean defenderLost = defenderNow != null && war.defenderBaseline > 0
                    && defenderNow < war.defenderBaseline;
            if (!attackerLost && !defenderLost) return;
        }

        if (attackerNow != null) {
            war.attackerStrength = attackerNow;
            if (war.attackerBaseline <= 0) war.attackerBaseline = attackerNow;
        }
        if (defenderNow != null) {
            war.defenderStrength = defenderNow;
            if (war.defenderBaseline <= 0) war.defenderBaseline = defenderNow;
        }

        int target;
        if (attackerNow != null && defenderNow != null) {
            int total = attackerNow + defenderNow;
            target = total > 0
                    ? (int) Math.round(100.0 * attackerNow / total) : VillageWarData.SCORE_DRAW;
        } else {
            double attackerRatio = survivorRatio(attackerNow, war.attackerBaseline);
            double defenderRatio = survivorRatio(defenderNow, war.defenderBaseline);
            double together = attackerRatio + defenderRatio;
            target = together > 0
                    ? (int) Math.round(100.0 * attackerRatio / together) : VillageWarData.SCORE_DRAW;
        }

        int score = war.attackerScore;
        if (score < target) score = Math.min(target, score + SCORE_STEP);
        else if (score > target) score = Math.max(target, score - SCORE_STEP);

        war.attackerScore = score;
        war.defenderScore = 100 - score;
        wars.markDirty();
    }

    private static double survivorRatio(Integer now, int baseline) {
        if (now == null || baseline <= 0) return 1.0;
        return Math.min(1.0, now / (double) baseline);
    }

    private static Integer measuredStrength(ServerLevel level, VillageRelationshipData relations,
                                            String villageId) {
        BlockPos center = relations.getVillagePosition(villageId);
        if (center == null || !level.isLoaded(center)) return null;
        return strength(level, center);
    }

    private static int strength(ServerLevel level, BlockPos center) {
        return level.getEntitiesOfClass(Villager.class, columnArea(level, center, STRENGTH_RADIUS),
                villager -> !villager.isBaby()).size();
    }

    private static AABB columnArea(ServerLevel level, BlockPos center, double radius) {
        return new AABB(
                center.getX() - radius, level.getMinBuildHeight(), center.getZ() - radius,
                center.getX() + radius + 1.0, level.getMaxBuildHeight(), center.getZ() + radius + 1.0);
    }

    private static void broadcast(ServerLevel level, VillageRelationshipData relations,
                                  String villageA, String villageB, MutableComponent text) {
        BlockPos a = relations.getVillagePosition(villageA);
        BlockPos b = relations.getVillagePosition(villageB);
        for (ServerPlayer player : level.players()) {
            if (near(player, a) || near(player, b)) {
                player.sendSystemMessage(text);
            }
        }
    }

    private static boolean near(ServerPlayer player, BlockPos center) {
        if (center == null) return false;
        return player.blockPosition().distSqr(center) <= NOTIFY_RADIUS * NOTIFY_RADIUS;
    }

    private static MutableComponent message(String key, Object... args) {
        return GrievanceHandler.prefix()
                .append(Component.translatable(key, args).withStyle(ChatFormatting.GRAY));
    }
}
