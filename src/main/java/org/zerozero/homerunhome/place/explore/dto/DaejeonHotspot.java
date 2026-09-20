package org.zerozero.homerunhome.place.explore.dto;

import java.math.BigDecimal;

public enum DaejeonHotspot {
    EUNHAENG_DAEHEUNG("36.3277", "127.4273", 1200),
    SOJE_DONG("36.3353", "127.4410", 1000),
    DUNSAN_DONG("36.3517", "127.3785", 1500),
    EXPO("36.3751", "127.3876", 1500),
    YUSEONG("36.3537", "127.3414", 1500);

    private final BigDecimal latitude;
    private final BigDecimal longitude;
    private final int radiusMeters;

    DaejeonHotspot(String latitude, String longitude, int radiusMeters) {
        this.latitude = new BigDecimal(latitude);
        this.longitude = new BigDecimal(longitude);
        this.radiusMeters = radiusMeters;
    }

    public BigDecimal latitude() {
        return latitude;
    }

    public BigDecimal longitude() {
        return longitude;
    }

    public int radiusMeters() {
        return radiusMeters;
    }
}
