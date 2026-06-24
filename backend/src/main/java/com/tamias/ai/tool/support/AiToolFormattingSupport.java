package com.tamias.ai.tool.support;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.security.service.CurrentUserService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public abstract class AiToolFormattingSupport extends AiToolSearchSupport {

    protected AiToolFormattingSupport(EntityManager entityManager, CurrentUserService currentUserService) {
        super(entityManager, currentUserService);
    }

    protected String value(Object value) {
        return value == null ? "" : value.toString();
    }

    protected String blankToDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    protected String formatBooleanYesNo(Object value) {
        String text = normalize(value(value));
        if (text.isBlank()) {
            return "—";
        }
        return containsAny(text, "true", "t", "yes", "si", "sí", "1") ? "Sí" : "No";
    }

    protected String formatDateTime(Object value) {
        String text = value(value).trim();
        if (text.isBlank()) {
            return "—";
        }

        int tIndex = text.indexOf('T');
        if (tIndex > 0) {
            String date = text.substring(0, tIndex);
            String rest = text.substring(tIndex + 1);
            StringBuilder time = new StringBuilder();
            for (int i = 0; i < rest.length() && time.length() < 8; i++) {
                char current = rest.charAt(i);
                if ((current >= '0' && current <= '9') || current == ':') {
                    time.append(current);
                } else {
                    break;
                }
            }
            return time.isEmpty() ? date : date + " " + time;
        }

        if (text.length() >= 19 && text.charAt(10) == ' ') {
            return text.substring(0, 19);
        }
        return text;
    }

    protected String formatDateTimeMinutes(Object value) {
        String formatted = formatDateTime(value);
        if (formatted.length() >= 16 && formatted.charAt(10) == ' ') {
            return formatted.substring(0, 16);
        }
        return formatted;
    }

    protected List<String> splitCommaValues(String value) {
        List<String> values = new ArrayList<>();
        if (value == null || value.isBlank()) {
            return values;
        }
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == ',') {
                String item = current.toString().trim();
                if (!item.isBlank()) {
                    values.add(item);
                }
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        String item = current.toString().trim();
        if (!item.isBlank()) {
            values.add(item);
        }
        return values;
    }

    protected String firstLine(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        List<String> lines = splitLines(value);
        return lines.isEmpty() ? value.trim() : lines.get(0).trim();
    }

    protected String formatTimelineContent(String value) {
        if (value == null || value.isBlank()) {
            return "—";
        }
        List<String> lines = splitLines(value);
        if (lines.isEmpty()) {
            return value.trim();
        }
        StringBuilder builder = new StringBuilder(lines.get(0).trim());
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (!line.isBlank()) {
                builder.append(System.lineSeparator()).append("	").append(line);
            }
        }
        return builder.toString();
    }

    protected String formatMoney(Object value) {
        if (value == null || value.toString().isBlank()) {
            return "—";
        }
        return "Q " + value;
    }


    protected String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        double kb = bytes / 1024.0;
        if (kb < 1024) {
            return String.format(Locale.ROOT, "%.1f KB", kb);
        }
        double mb = kb / 1024.0;
        if (mb < 1024) {
            return String.format(Locale.ROOT, "%.1f MB", mb);
        }
        double gb = mb / 1024.0;
        return String.format(Locale.ROOT, "%.1f GB", gb);
    }

    protected String joinName(Object firstName, Object lastName) {
        return (value(firstName) + " " + value(lastName)).trim();
    }


    protected long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = value(value);
        if (text.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }


    protected String firstNonBlank(String first, String second) {
        return first == null || first.isBlank() ? blankToDash(second) : first;
    }
}
