package com.cesoti2006.villagediplomacy.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import javax.annotation.Nonnull;
import java.util.*;

public class VillageGrievanceData extends SavedData {

    private static final String DATA_NAME = "village_diplomacy_grievances";

    public static final long TICKS_PER_DAY = 24000L;

    public static final long FOREVER = Long.MAX_VALUE;

    private static final int MAX_PER_VILLAGE = 24;

    public enum Severity {

        MILD(15),
        MEDIUM(30),
        SEVERE(60),
        HISTORIC(0);

        private final int days;

        Severity(int days) {
            this.days = days;
        }

        public int days() {
            return days;
        }

        public boolean forever() {
            return days <= 0;
        }
    }

    public enum GrievanceType {

        VILLAGER_KILLED(Severity.SEVERE, 15),
        BABY_KILLED(Severity.SEVERE, 20),
        GUARD_KILLED(Severity.SEVERE, 20),
        GOLEM_DESTROYED(Severity.MEDIUM, 25),
        SABOTAGE(Severity.MEDIUM, 20),
        THEFT(Severity.MILD, 8),
        WAR(Severity.HISTORIC, 0);

        private final Severity severity;
        private final int impact;

        GrievanceType(Severity severity, int impact) {
            this.severity = severity;
            this.impact = impact;
        }

        public Severity severity() {
            return severity;
        }

        public int impact() {
            return impact;
        }

        public String getTranslationKey() {
            return "villagediplomacy.grievance.type." + name().toLowerCase(Locale.ROOT);
        }

        public static GrievanceType fromString(String value) {
            if (value == null) return null;
            try {
                return valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    public static class Grievance {

        public final long id;

        public final String villageId;
        public final GrievanceType type;

        public final String culpritId;

        public final String culpritName;
        public final int impact;

        public long createdDay;

        public long expiresDay;

        public int count = 1;

        public boolean resolved;

        public boolean expiryNoticed;

        Grievance(long id, String villageId, GrievanceType type, String culpritId,
                 String culpritName, int impact, long createdDay, long expiresDay) {
            this.id = id;
            this.villageId = villageId;
            this.type = type;
            this.culpritId = culpritId;
            this.culpritName = culpritName;
            this.impact = impact;
            this.createdDay = createdDay;
            this.expiresDay = expiresDay;
        }

        public boolean expired(long today) {
            return expiresDay != FOREVER && today >= expiresDay;
        }

        public boolean isActive(long today) {
            return !resolved && !expired(today);
        }

        public long daysLeft(long today) {
            return expiresDay == FOREVER ? -1 : Math.max(0, expiresDay - today);
        }

        public long daysAgo(long today) {
            return Math.max(0, today - createdDay);
        }
    }

    public static final class Recorded {

        public final Grievance grievance;
        public final boolean isNew;

        Recorded(Grievance grievance, boolean isNew) {
            this.grievance = grievance;
            this.isNew = isNew;
        }
    }

    private final Map<String, List<Grievance>> byVillage = new HashMap<>();
    private long nextId = 1L;

    public VillageGrievanceData() {
    }

    public static VillageGrievanceData get(ServerLevel level) {
        DimensionDataStorage storage = level.getServer().overworld().getDataStorage();
        return storage.computeIfAbsent(new SavedData.Factory<>(VillageGrievanceData::new, (tag, registries) -> VillageGrievanceData.load(tag), null), DATA_NAME);
    }

    public static long dayOf(ServerLevel level) {
        return level.getServer().overworld().getGameTime() / TICKS_PER_DAY;
    }

    public static VillageGrievanceData load(@Nonnull CompoundTag tag) {
        VillageGrievanceData data = new VillageGrievanceData();
        data.nextId = Math.max(1L, tag.getLong("NextId"));

        ListTag list = tag.getList("Grievances", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);

            GrievanceType type = GrievanceType.fromString(entry.getString("Type"));
            String villageId = entry.getString("Village");
            String culpritId = entry.getString("Culprit");
            if (type == null || villageId.isEmpty() || culpritId.isEmpty()) continue;

            Grievance grievance = new Grievance(
                    entry.getLong("Id"),
                    villageId,
                    type,
                    culpritId,
                    entry.getString("CulpritName"),
                    entry.getInt("Impact"),
                    entry.getLong("CreatedDay"),
                    entry.contains("ExpiresDay") ? entry.getLong("ExpiresDay") : FOREVER);
            grievance.count = Math.max(1, entry.getInt("Count"));
            grievance.resolved = entry.getBoolean("Resolved");
            grievance.expiryNoticed = entry.getBoolean("ExpiryNoticed");

            data.byVillage.computeIfAbsent(villageId, k -> new ArrayList<>()).add(grievance);
            data.nextId = Math.max(data.nextId, grievance.id + 1);
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<String, List<Grievance>> entry : byVillage.entrySet()) {
            for (Grievance grievance : entry.getValue()) {
                CompoundTag g = new CompoundTag();
                g.putLong("Id", grievance.id);
                g.putString("Village", grievance.villageId);
                g.putString("Type", grievance.type.name());
                g.putString("Culprit", grievance.culpritId);
                g.putString("CulpritName", grievance.culpritName);
                g.putInt("Impact", grievance.impact);
                g.putLong("CreatedDay", grievance.createdDay);
                g.putLong("ExpiresDay", grievance.expiresDay);
                g.putInt("Count", grievance.count);
                g.putBoolean("Resolved", grievance.resolved);
                g.putBoolean("ExpiryNoticed", grievance.expiryNoticed);
                list.add(g);
            }
        }
        tag.put("Grievances", list);
        tag.putLong("NextId", nextId);
        return tag;
    }

    public Recorded record(String villageId, GrievanceType type, String culpritId,
                           String culpritName, long today) {
        List<Grievance> list = byVillage.computeIfAbsent(villageId, k -> new ArrayList<>());

        for (Grievance existing : list) {
            if (existing.type != type || !existing.culpritId.equals(culpritId)) continue;
            if (!existing.isActive(today)) continue;

            existing.count++;
            existing.resolved = false;
            if (existing.expiresDay != FOREVER) {
                existing.expiresDay = today + type.severity().days();
                existing.expiryNoticed = false;
            }
            setDirty();
            return new Recorded(existing, false);
        }

        Severity severity = type.severity();
        Grievance grievance = new Grievance(nextId++, villageId, type, culpritId, culpritName,
                type.impact(), today, severity.forever() ? FOREVER : today + severity.days());
        list.add(grievance);
        trim(list, today);
        setDirty();
        return new Recorded(grievance, true);
    }

    public Recorded recordWarMemory(String villageId, String formerEnemyId, String formerEnemyName,
                                    long today) {
        return record(villageId, GrievanceType.WAR,
                "village:" + formerEnemyId + ":war:" + nextId, formerEnemyName, today);
    }

    public List<Grievance> getActive(String villageId, long today) {
        List<Grievance> list = byVillage.get(villageId);
        if (list == null) return Collections.emptyList();

        List<Grievance> active = new ArrayList<>();
        for (Grievance grievance : list) {
            if (grievance.isActive(today)) active.add(grievance);
        }

        active.sort(Comparator.comparingLong((Grievance g) -> g.createdDay).reversed()
                .thenComparing(Comparator.comparingLong((Grievance g) -> g.id).reversed()));
        return active;
    }

    public Grievance latestActive(String villageId, long today) {
        List<Grievance> active = getActive(villageId, today);
        return active.isEmpty() ? null : active.get(0);
    }

    public int totalImpact(String villageId, String culpritId, long today) {
        int total = 0;
        for (Grievance grievance : getActive(villageId, today)) {
            if (grievance.culpritId.equals(culpritId)) total += grievance.impact;
        }
        return total;
    }

    public List<Grievance> collectNewlyExpired(long today) {
        List<Grievance> expired = new ArrayList<>();
        for (List<Grievance> list : byVillage.values()) {
            for (Grievance grievance : list) {
                if (grievance.resolved || grievance.expiryNoticed) continue;
                if (!grievance.expired(today)) continue;
                grievance.expiryNoticed = true;
                expired.add(grievance);
            }
        }
        if (!expired.isEmpty()) setDirty();
        return expired;
    }

    public int resolve(String villageId, String culpritId, long today) {
        List<Grievance> list = byVillage.get(villageId);
        if (list == null) return 0;

        int resolved = 0;
        for (Grievance grievance : list) {
            if (grievance.culpritId.equals(culpritId) && grievance.isActive(today)
                    && grievance.type.severity() != Severity.HISTORIC) {
                grievance.resolved = true;
                resolved++;
            }
        }
        if (resolved > 0) setDirty();
        return resolved;
    }

    public int age(int days, long today) {
        if (days <= 0) return countActive(today);

        for (List<Grievance> list : byVillage.values()) {
            for (Grievance grievance : list) {
                grievance.createdDay -= days;
                if (grievance.expiresDay != FOREVER) grievance.expiresDay -= days;
            }
        }
        setDirty();
        return countActive(today);
    }

    public int countActive(long today) {
        int total = 0;
        for (List<Grievance> list : byVillage.values()) {
            for (Grievance grievance : list) {
                if (grievance.isActive(today)) total++;
            }
        }
        return total;
    }

    private void trim(List<Grievance> list, long today) {
        if (list.size() <= MAX_PER_VILLAGE) return;

        list.sort(Comparator.comparingLong((Grievance g) -> g.createdDay));
        for (int i = list.size() - 1; i >= 0 && list.size() > MAX_PER_VILLAGE; i--) {
            Grievance grievance = list.get(i);
            if (grievance.isActive(today)) continue;
            list.remove(i);
        }
        while (list.size() > MAX_PER_VILLAGE) {
            int removable = -1;
            for (int i = 0; i < list.size(); i++) {
                if (list.get(i).type != GrievanceType.WAR) {
                    removable = i;
                    break;
                }
            }
            if (removable < 0) break;
            list.remove(removable);
        }
    }
}
