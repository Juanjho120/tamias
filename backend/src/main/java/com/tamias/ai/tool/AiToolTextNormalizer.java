package com.tamias.ai.tool;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AiToolTextNormalizer {

    private AiToolTextNormalizer() {
    }

    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String lowered = removeAccents(value)
                .toLowerCase(Locale.ROOT)
                .replace("¿", " ")
                .replace("?", " ")
                .replace(",", " ")
                .replace(".", " ");
        return collapseWhitespace(lowered);
    }

    public static String normalizeForRouting(String value) {
        if (value == null) {
            return "";
        }
        String lowered = removeAccents(value)
                .toLowerCase(Locale.ROOT)
                .replace("¿", " ")
                .replace("?", " ")
                .replace(",", " ")
                .replace(".", " ")
                .replace(":", " ")
                .replace(";", " ");
        return collapseWhitespace(lowered);
    }

    public static boolean containsAny(String value, String... candidates) {
        String safeValue = value == null ? "" : value;
        for (String candidate : candidates) {
            if (safeValue.contains(normalize(candidate))) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsAnyForRouting(String value, String... candidates) {
        String safeValue = value == null ? "" : value;
        for (String candidate : candidates) {
            if (safeValue.contains(normalizeForRouting(candidate))) {
                return true;
            }
        }
        return false;
    }

    public static boolean startsWithAnyForRouting(String value, String... prefixes) {
        String safeValue = value == null ? "" : value;
        for (String prefix : prefixes) {
            if (safeValue.startsWith(normalizeForRouting(prefix))) {
                return true;
            }
        }
        return false;
    }

    public static String keepSearchCharacters(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if ((current >= 'a' && current <= 'z') || (current >= '0' && current <= '9') || current == '-') {
                builder.append(current);
            } else if (Character.isWhitespace(current)) {
                builder.append(' ');
            } else {
                builder.append(' ');
            }
        }
        return collapseWhitespace(builder.toString());
    }

    public static String collapseWhitespace(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        StringBuilder builder = new StringBuilder(value.length());
        boolean previousWasWhitespace = true;
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (Character.isWhitespace(current)) {
                if (!previousWasWhitespace) {
                    builder.append(' ');
                    previousWasWhitespace = true;
                }
            } else {
                builder.append(current);
                previousWasWhitespace = false;
            }
        }

        int length = builder.length();
        if (length > 0 && builder.charAt(length - 1) == ' ') {
            builder.deleteCharAt(length - 1);
        }
        return builder.toString();
    }

    public static List<String> splitWords(String value) {
        List<String> words = new ArrayList<>();
        String normalizedValue = collapseWhitespace(value);
        if (normalizedValue.isBlank()) {
            return words;
        }

        StringBuilder currentWord = new StringBuilder();
        for (int i = 0; i < normalizedValue.length(); i++) {
            char current = normalizedValue.charAt(i);
            if (Character.isWhitespace(current)) {
                if (!currentWord.isEmpty()) {
                    words.add(currentWord.toString());
                    currentWord.setLength(0);
                }
            } else {
                currentWord.append(current);
            }
        }

        if (!currentWord.isEmpty()) {
            words.add(currentWord.toString());
        }
        return words;
    }

    public static List<String> splitLines(String value) {
        List<String> lines = new ArrayList<>();
        if (value == null || value.isBlank()) {
            return lines;
        }

        StringBuilder currentLine = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == '\n' || current == '\r') {
                if (!currentLine.isEmpty()) {
                    lines.add(currentLine.toString());
                    currentLine.setLength(0);
                }
            } else {
                currentLine.append(current);
            }
        }

        if (!currentLine.isEmpty()) {
            lines.add(currentLine.toString());
        }
        return lines;
    }

    private static String removeAccents(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        StringBuilder withoutAccents = new StringBuilder(normalized.length());
        for (int i = 0; i < normalized.length(); i++) {
            char current = normalized.charAt(i);
            if (Character.getType(current) != Character.NON_SPACING_MARK) {
                withoutAccents.append(current);
            }
        }
        return withoutAccents.toString();
    }
}
