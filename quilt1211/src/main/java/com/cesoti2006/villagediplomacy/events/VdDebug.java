package com.cesoti2006.villagediplomacy.events;

public final class VdDebug {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("VillageDiplomacy");

    private VdDebug() {}

    public static void log(String fmt, Object... args) {
        LOG.info("[VD] " + fmt, args);
    }
}
