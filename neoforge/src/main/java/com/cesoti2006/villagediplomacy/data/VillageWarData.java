package com.cesoti2006.villagediplomacy.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class VillageWarData extends SavedData {

    private static final String DATA_NAME = "village_wars";

    public static final int SCORE_DRAW = 50;

    private final Map<String, WarRecord> wars = new HashMap<>();

    public VillageWarData() {
    }

    public static class WarRecord {

        public String attacker = "";

        public String defender = "";

        public int attackerScore = SCORE_DRAW;
        public int defenderScore = SCORE_DRAW;

        public int attackerStrength;
        public int defenderStrength;

        public int attackerBaseline;
        public int defenderBaseline;

        public long startedDay = -1L;

        public long mobilizationUntil;

        public long alarmUntil;

        public long nextAlarm;
    }

    public static VillageWarData load(@Nonnull CompoundTag tag) {
        VillageWarData data = new VillageWarData();

        ListTag list = tag.getList("Wars", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag warTag = list.getCompound(i);
            String key = warTag.getString("Pair");
            if (key.isEmpty()) continue;

            WarRecord war = new WarRecord();
            war.attacker = warTag.getString("Attacker");
            war.defender = warTag.getString("Defender");
            war.attackerScore = clampScore(warTag.getInt("AttackerScore"));
            war.defenderScore = clampScore(warTag.getInt("DefenderScore"));
            war.attackerStrength = warTag.getInt("AttackerStrength");
            war.defenderStrength = warTag.getInt("DefenderStrength");
            war.attackerBaseline = warTag.getInt("AttackerBaseline");
            war.defenderBaseline = warTag.getInt("DefenderBaseline");
            war.startedDay = warTag.contains("StartedDay") ? warTag.getLong("StartedDay") : -1L;
            war.mobilizationUntil = warTag.getLong("MobilizationUntil");
            war.alarmUntil = warTag.getLong("AlarmUntil");
            war.nextAlarm = warTag.getLong("NextAlarm");
            data.wars.put(key, war);
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<String, WarRecord> entry : wars.entrySet()) {
            WarRecord war = entry.getValue();
            CompoundTag warTag = new CompoundTag();
            warTag.putString("Pair", entry.getKey());
            warTag.putString("Attacker", war.attacker);
            warTag.putString("Defender", war.defender);
            warTag.putInt("AttackerScore", war.attackerScore);
            warTag.putInt("DefenderScore", war.defenderScore);
            warTag.putInt("AttackerStrength", war.attackerStrength);
            warTag.putInt("DefenderStrength", war.defenderStrength);
            warTag.putInt("AttackerBaseline", war.attackerBaseline);
            warTag.putInt("DefenderBaseline", war.defenderBaseline);
            warTag.putLong("StartedDay", war.startedDay);
            warTag.putLong("MobilizationUntil", war.mobilizationUntil);
            warTag.putLong("AlarmUntil", war.alarmUntil);
            warTag.putLong("NextAlarm", war.nextAlarm);
            list.add(warTag);
        }
        tag.put("Wars", list);
        return tag;
    }

    public static VillageWarData get(ServerLevel level) {
        DimensionDataStorage storage = level.getServer().overworld().getDataStorage();
        return storage.computeIfAbsent(VillageWarData::load, VillageWarData::new, DATA_NAME);
    }

    private static int clampScore(int value) {
        return Math.max(0, Math.min(100, value));
    }

    public WarRecord getWar(String village1, String village2) {
        return wars.get(VillageDiplomacyData.pairKey(village1, village2));
    }

    public WarRecord startWar(String village1, String village2, String attacker, String defender,
                              long startedDay) {
        WarRecord war = new WarRecord();
        war.attacker = attacker;
        war.defender = defender;
        war.startedDay = startedDay;
        wars.put(VillageDiplomacyData.pairKey(village1, village2), war);
        setDirty();
        return war;
    }

    public boolean endWar(String village1, String village2) {
        if (wars.remove(VillageDiplomacyData.pairKey(village1, village2)) == null) return false;
        setDirty();
        return true;
    }

    public Map<String, WarRecord> getAllWars() {
        return wars;
    }

    public void markDirty() {
        setDirty();
    }
}
