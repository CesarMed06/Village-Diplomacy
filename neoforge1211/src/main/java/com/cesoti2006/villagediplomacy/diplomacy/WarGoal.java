package com.cesoti2006.villagediplomacy.diplomacy;

import java.util.Locale;

public enum WarGoal {

    NONE,
    TRIBUTE,
    BORDER,
    SUBMISSION,
    VENGEANCE;

    private static final String KEY_PREFIX = "villagediplomacy.wargoal.";

    public boolean isPresent() {
        return this != NONE;
    }

    public String getTranslationKey() {
        return KEY_PREFIX + name().toLowerCase(Locale.ROOT);
    }

    public static WarGoal fromString(String value) {
        if (value == null) return NONE;
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
