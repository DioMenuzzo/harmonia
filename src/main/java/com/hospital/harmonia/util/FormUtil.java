package com.hospital.harmonia.util;

import javafx.scene.control.DatePicker;
import javafx.util.StringConverter;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Small form/filter helpers shared by every module screen that has a
 * DatePicker and/or a free-text filter field (Cafeteria, Technical Support).
 *
 * These four were previously duplicated byte-for-byte in
 * CafeteriaController and TechnicalSupportController (see the audit's
 * "3.5 Duplicação de código") -- kept here instead so a future fix or
 * change only needs to happen once.
 */
public final class FormUtil {

    /**
     * Fixed date format (day/month/year), independent of the machine's
     * locale -- see configureDateFormat below.
     */
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private FormUtil() {
    }

    /**
     * By default a DatePicker displays/parses the date in the MACHINE'S
     * LOCALE format (e.g. "7/27/2026" on a machine set to English/US) --
     * this explicitly forces the Brazilian day/month/year format instead,
     * regardless of the system's locale.
     */
    public static void configureDateFormat(DatePicker datePicker) {
        datePicker.setPromptText("dd/mm/aaaa");
        datePicker.setConverter(new StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate date) {
                return date == null ? "" : DATE_FORMAT.format(date);
            }

            @Override
            public LocalDate fromString(String text) {
                if (text == null || text.isBlank()) {
                    return null;
                }
                return LocalDate.parse(text.trim(), DATE_FORMAT);
            }
        });
    }

    /** Never null -- an empty string instead, so callers can chain .trim()/.contains() safely. */
    public static String orEmpty(String text) {
        return text == null ? "" : text;
    }

    /** Strips accents and lowercases, so a text filter/search is more tolerant. */
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String withoutAccents = Normalizer.normalize(text.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase();
    }
}
