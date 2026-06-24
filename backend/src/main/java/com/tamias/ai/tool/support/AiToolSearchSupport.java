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

public abstract class AiToolSearchSupport extends AiReadOnlyQuerySupport {

    protected AiToolSearchSupport(EntityManager entityManager, CurrentUserService currentUserService) {
        super(entityManager, currentUserService);
    }

    protected static final Set<String> SEARCH_STOP_WORDS = Set.of(
            "a", "al", "algo", "actual", "actuales", "actualmente", "ahi", "aqui",
            "cargado", "cargados", "con", "cual", "cuales", "cuando", "cuanto", "cuantos",
            "da", "dame", "de", "del", "dice", "e", "el", "en", "estado", "estan", "esta",
            "este", "estos", "fue", "hay", "indexado", "indexados", "la", "las", "le",
            "lista", "listar", "lo", "los", "me", "mi", "mis", "muestra", "nombre", "o",
            "para", "por", "procesado", "procesados", "que", "quiero", "reciente", "registrada",
            "registradas", "registrado", "registrados", "se", "son", "subido", "subidos", "tengo",
            "tienes", "tipo", "tu", "un", "una", "usado", "usados", "usaron", "usan", "usa", "uso", "ver", "vez", "y"
    );

    protected String extractSearchText(String userQuestion, String... extraStopWords) {
        if (userQuestion == null) {
            return "";
        }
        Set<String> extra = Arrays.stream(extraStopWords)
                .map(this::normalize)
                .collect(Collectors.toSet());
        String cleaned = keepSearchCharacters(normalize(userQuestion));
        return trimSearch(splitWords(cleaned).stream()
                .filter(word -> !SEARCH_STOP_WORDS.contains(word))
                .filter(word -> !extra.contains(word))
                .collect(Collectors.joining(" ")));
    }

    protected String trimSearch(String value) {
        String cleaned = collapseWhitespace(value);
        return cleaned.length() > 60 ? cleaned.substring(0, 60).trim() : cleaned;
    }

    protected String nullableSearch(String search) {
        return search == null || search.isBlank() ? null : search;
    }

    protected boolean containsAny(String value, String... candidates) {
        return AiToolTextNormalizer.containsAny(value, candidates);
    }

    protected String normalize(String value) {
        return AiToolTextNormalizer.normalize(value);
    }

    protected String keepSearchCharacters(String value) {
        return AiToolTextNormalizer.keepSearchCharacters(value);
    }

    protected String collapseWhitespace(String value) {
        return AiToolTextNormalizer.collapseWhitespace(value);
    }

    protected List<String> splitWords(String value) {
        return AiToolTextNormalizer.splitWords(value);
    }

    protected List<String> splitLines(String value) {
        return AiToolTextNormalizer.splitLines(value);
    }
}
