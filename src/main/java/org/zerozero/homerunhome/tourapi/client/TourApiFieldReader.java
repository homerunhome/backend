package org.zerozero.homerunhome.tourapi.client;

import java.math.BigDecimal;
import java.util.Map;

final class TourApiFieldReader {

    private TourApiFieldReader() {}

    static String text(Map<String, Object> fields, String name) {
        Object value = fields.get(name);
        return value == null ? "" : value.toString().trim();
    }

    static BigDecimal decimal(Map<String, Object> fields, String name) {
        String value = text(fields, name);
        if (value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
