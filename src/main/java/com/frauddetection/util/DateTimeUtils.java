package com.frauddetection.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Ortak yardımcı methodlar için util sınıfı.
 *
 * Bu sınıf, uygulama genelinde kullanılabilecek yardımcı işlevleri
 * merkezi bir yerde toplamak için kullanılır.
 *
 * @author Dolandırıcılık Tespit Ekibi
 */
public final class DateTimeUtils {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    private DateTimeUtils() {
        // util sınıfı, örneklenmemeli
    }

    public static String toIsoString(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(ISO_FORMATTER);
    }
}
