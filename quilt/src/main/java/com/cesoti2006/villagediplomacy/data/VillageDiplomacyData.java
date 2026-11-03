package com.cesoti2006.villagediplomacy.data;

import com.cesoti2006.villagediplomacy.diplomacy.CasusBelli;
import com.cesoti2006.villagediplomacy.diplomacy.DiplomaticState;
import com.cesoti2006.villagediplomacy.diplomacy.WarGoal;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import javax.annotation.Nonnull;
import java.util.*;

public class VillageDiplomacyData extends SavedData {

    private static final String DATA_NAME = "village_diplomacy_states";

    public static final int TENSION_MIN = 0;
    public static final int TENSION_MAX = 100;

    private final Map<String, PairState> pairs = new HashMap<>();

    public VillageDiplomacyData() {
    }

    public static class PairState {
        public int tension;
        public DiplomaticState state = DiplomaticState.PEACE;
        public long truceUntil;

        public String warAggressor = "";

        public int warDesire;

        public CasusBelli casusBelli = CasusBelli.NONE;

        public WarGoal warGoal = WarGoal.NONE;

        public long ultimatumUntil;

        public long declaredDay = -1L;

        public PairState() {
        }
    }

    public static VillageDiplomacyData load(@Nonnull CompoundTag tag) {
        VillageDiplomacyData data = new VillageDiplomacyData();

        ListTag list = tag.getList("Pairs", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag pairTag = list.getCompound(i);
            String key = pairTag.getString("Pair");
            if (key.isEmpty()) continue;

            PairState pair = new PairState();
            pair.tension = clampTension(pairTag.getInt("Tension"));
            pair.state = DiplomaticState.fromString(pairTag.getString("State"));
            pair.truceUntil = pairTag.getLong("TruceUntil");
            pair.warAggressor = pairTag.getString("WarAggressor");
            pair.warDesire = clampTension(pairTag.getInt("WarDesire"));
            pair.casusBelli = CasusBelli.fromString(pairTag.getString("CasusBelli"));
            pair.warGoal = WarGoal.fromString(pairTag.getString("WarGoal"));
            pair.ultimatumUntil = pairTag.getLong("UltimatumUntil");
            pair.declaredDay = pairTag.contains("DeclaredDay") ? pairTag.getLong("DeclaredDay") : -1L;
            data.pairs.put(key, pair);
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<String, PairState> entry : pairs.entrySet()) {
            CompoundTag pairTag = new CompoundTag();
            pairTag.putString("Pair", entry.getKey());
            pairTag.putInt("Tension", entry.getValue().tension);
            pairTag.putString("State", entry.getValue().state.name());
            pairTag.putLong("TruceUntil", entry.getValue().truceUntil);
            pairTag.putString("WarAggressor", entry.getValue().warAggressor);
            pairTag.putInt("WarDesire", entry.getValue().warDesire);
            pairTag.putString("CasusBelli", entry.getValue().casusBelli.name());
            pairTag.putString("WarGoal", entry.getValue().warGoal.name());
            pairTag.putLong("UltimatumUntil", entry.getValue().ultimatumUntil);
            pairTag.putLong("DeclaredDay", entry.getValue().declaredDay);
            list.add(pairTag);
        }
        tag.put("Pairs", list);
        return tag;
    }

    public static VillageDiplomacyData get(ServerLevel level) {
        DimensionDataStorage storage = level.getServer().overworld().getDataStorage();
        return storage.computeIfAbsent(VillageDiplomacyData::load, VillageDiplomacyData::new, DATA_NAME);
    }

    private static int clampTension(int value) {
        return Math.max(TENSION_MIN, Math.min(TENSION_MAX, value));
    }

    public static String pairKey(String village1, String village2) {
        return village1.compareTo(village2) <= 0
                ? village1 + ":" + village2
                : village2 + ":" + village1;
    }

    private PairState getOrCreate(String village1, String village2) {
        String key = pairKey(village1, village2);
        PairState pair = pairs.get(key);
        if (pair == null) {
            pair = new PairState();
            pairs.put(key, pair);
        }
        return pair;
    }

    public PairState getOrCreatePair(String village1, String village2) {
        return getOrCreate(village1, village2);
    }

    public PairState peekPair(String village1, String village2) {
        return pairs.get(pairKey(village1, village2));
    }

    public Map<String, PairState> getAllPairs() {
        return pairs;
    }

    public void markDirty() {
        setDirty();
    }

    public DiplomaticState getState(String village1, String village2) {
        PairState pair = pairs.get(pairKey(village1, village2));
        return pair != null ? pair.state : DiplomaticState.PEACE;
    }

    public void setState(String village1, String village2, DiplomaticState state) {
        getOrCreate(village1, village2).state = state;
        setDirty();
    }

    public int getTension(String village1, String village2) {
        PairState pair = pairs.get(pairKey(village1, village2));
        return pair != null ? pair.tension : 0;
    }

    public void setTension(String village1, String village2, int tension) {
        getOrCreate(village1, village2).tension = clampTension(tension);
        setDirty();
    }

    public void addTension(String village1, String village2, int delta) {
        PairState pair = getOrCreate(village1, village2);
        pair.tension = clampTension(pair.tension + delta);
        setDirty();
    }

    public long getTruceUntil(String village1, String village2) {
        PairState pair = pairs.get(pairKey(village1, village2));
        return pair != null ? pair.truceUntil : 0L;
    }

    public void setTruceUntil(String village1, String village2, long gameTime) {
        getOrCreate(village1, village2).truceUntil = gameTime;
        setDirty();
    }

    public String getWarAggressor(String village1, String village2) {
        PairState pair = pairs.get(pairKey(village1, village2));
        return pair != null ? pair.warAggressor : "";
    }

    public void setWarAggressor(String village1, String village2, String aggressorId) {
        getOrCreate(village1, village2).warAggressor = aggressorId != null ? aggressorId : "";
        setDirty();
    }

    public Map<String, PairState> getPairsInvolving(String villageId) {
        Map<String, PairState> result = new HashMap<>();
        for (Map.Entry<String, PairState> entry : pairs.entrySet()) {
            String[] parts = entry.getKey().split(":");
            if (parts.length == 2 && (parts[0].equals(villageId) || parts[1].equals(villageId))) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    public List<String> getNonPeaceNeighbours(String villageId) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, PairState> entry : pairs.entrySet()) {
            if (entry.getValue().state == DiplomaticState.PEACE) continue;
            String[] parts = entry.getKey().split(":");
            if (parts.length != 2) continue;
            if (parts[0].equals(villageId)) result.add(parts[1]);
            else if (parts[1].equals(villageId)) result.add(parts[0]);
        }
        return result;
    }
}
