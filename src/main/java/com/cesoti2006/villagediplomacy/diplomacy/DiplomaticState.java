package com.cesoti2006.villagediplomacy.diplomacy;

import java.util.Locale;

public enum DiplomaticState {

    PEACE(true),
    TENSION(true),
    CRISIS(true),
    ULTIMATUM(false),
    MOBILIZING(false),
    WAR(false),
    SIEGE(false),
    TRUCE(true),
    OCCUPATION(false);

    private static final String KEY_PREFIX = "villagediplomacy.diplomacy.state.";

    private final boolean peaceful;

    DiplomaticState(boolean peaceful) {
        this.peaceful = peaceful;
    }

    public boolean isPeaceful() {
        return peaceful;
    }

    public boolean isWar() {
        return this == MOBILIZING || this == WAR || this == SIEGE || this == OCCUPATION;
    }

    public String getTranslationKey() {
        return KEY_PREFIX + name().toLowerCase(Locale.ROOT);
    }

    public static DiplomaticState fromString(String value) {
        if (value == null) return PEACE;
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return PEACE;
        }
    }

    public static boolean isValid(String value) {
        if (value == null) return false;
        try {
            valueOf(value.trim().toUpperCase(Locale.ROOT));
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
