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

public abstract class AiReadOnlyQuerySupport extends AiBaseReadOnlyToolSupport {

    protected AiReadOnlyQuerySupport(EntityManager entityManager, CurrentUserService currentUserService) {
        super(entityManager, currentUserService);
    }

    protected Object scalar(String sql, QueryConfigurer configurer) {
        Query query = entityManager.createNativeQuery(sql);
        configurer.configure(query);
        return normalizeValue(query.getSingleResult());
    }

    @SuppressWarnings("unchecked")
    protected List<Map<String, Object>> query(String sql, QueryConfigurer configurer, String... columns) {
        Query query = entityManager.createNativeQuery(sql);
        configurer.configure(query);
        List<Object> resultList = query.getResultList();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object result : resultList) {
            Object[] values = result instanceof Object[] array ? array : new Object[]{result};
            Map<String, Object> row = new LinkedHashMap<>();
            for (int index = 0; index < columns.length; index++) {
                Object value = index < values.length ? values[index] : null;
                row.put(columns[index], normalizeValue(value));
            }
            rows.add(row);
        }
        return rows;
    }

    protected Object normalizeValue(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant().toString();
        }
        if (value instanceof Date date) {
            return date.toLocalDate().toString();
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toString();
        }
        return value;
    }


    @FunctionalInterface
    protected interface QueryConfigurer {
        void configure(Query query);
    }
}
