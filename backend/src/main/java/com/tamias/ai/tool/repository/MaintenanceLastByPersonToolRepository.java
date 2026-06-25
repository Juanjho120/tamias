package com.tamias.ai.tool.repository;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.support.AiReadOnlyToolSupport;
import com.tamias.security.service.CurrentUserService;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class MaintenanceLastByPersonToolRepository extends AiReadOnlyToolSupport {

    public MaintenanceLastByPersonToolRepository(EntityManager entityManager, CurrentUserService currentUserService) {
        super(entityManager, currentUserService);
    }

    public AiToolAnswer lastMaintenanceByPerson(String userQuestion) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        String personSearch = nullableSearch(extractSearchText(
                userQuestion,
                "cual",
                "cuál",
                "cuando",
                "cuándo",
                "fue",
                "ultimo",
                "último",
                "ultima",
                "última",
                "mantenimiento",
                "mantenimientos",
                "que",
                "qué",
                "hizo",
                "realizo",
                "realizó",
                "ejecuto",
                "ejecutó",
                "atendio",
                "atendió",
                "persona",
                "responsable",
                "principal",
                "involucrado",
                "involucrada",
                "involucrados",
                "involucradas",
                "por",
                "de",
                "del",
                "la",
                "el",
                "un",
                "una"
        ));

        if (personSearch == null) {
            return AiToolAnswer.of(
                    "Necesito el nombre de la persona para buscar el último mantenimiento relacionado.",
                    "maintenance.lastByPerson",
                    "Last maintenance by person",
                    "A person name was required but could not be extracted from the question.",
                    List.of()
            );
        }

        List<Map<String, Object>> rows = query("""
                SELECT
                    mr.id,
                    p.name AS property_name,
                    mr.title,
                    mr.description,
                    mc.name AS category_name,
                    mt.name AS type_name,
                    mr.status,
                    mr.performed_at,
                    mr.scheduled_at,
                    COALESCE(mr.performed_at, mr.scheduled_at, mr.created_at) AS maintenance_date,
                    mr.cost,
                    responsible.full_name AS responsible_person_name,
                    COALESCE(
                        STRING_AGG(DISTINCT involved.full_name, ', ')
                            FILTER (WHERE involved.id IS NOT NULL),
                        ''
                    ) AS involved_people_names,
                    CASE
                        WHEN LOWER(COALESCE(responsible.full_name, '')) LIKE LOWER(CONCAT('%', CAST(:personSearch AS TEXT), '%'))
                             AND EXISTS (
                                SELECT 1
                                FROM maintenance_record_people mrp_match
                                JOIN maintenance_people involved_match
                                  ON involved_match.id = mrp_match.maintenance_person_id
                                WHERE mrp_match.maintenance_record_id = mr.id
                                  AND mrp_match.organization_id = mr.organization_id
                                  AND involved_match.deleted_at IS NULL
                                  AND LOWER(involved_match.full_name) LIKE LOWER(CONCAT('%', CAST(:personSearch AS TEXT), '%'))
                             )
                        THEN 'Responsable principal y persona involucrada'
                        WHEN LOWER(COALESCE(responsible.full_name, '')) LIKE LOWER(CONCAT('%', CAST(:personSearch AS TEXT), '%'))
                        THEN 'Responsable principal'
                        ELSE 'Persona involucrada'
                    END AS match_source
                FROM maintenance_records mr
                JOIN properties p
                  ON p.id = mr.property_id
                LEFT JOIN maintenance_categories mc
                  ON mc.id = mr.maintenance_category_id
                LEFT JOIN maintenance_types mt
                  ON mt.id = mr.maintenance_type_id
                LEFT JOIN maintenance_people responsible
                  ON responsible.id = mr.maintenance_person_id
                LEFT JOIN maintenance_record_people mrp
                  ON mrp.maintenance_record_id = mr.id
                 AND mrp.organization_id = mr.organization_id
                LEFT JOIN maintenance_people involved
                  ON involved.id = mrp.maintenance_person_id
                 AND involved.deleted_at IS NULL
                WHERE mr.organization_id = :organizationId
                  AND mr.deleted_at IS NULL
                  AND (
                    LOWER(COALESCE(responsible.full_name, '')) LIKE LOWER(CONCAT('%', CAST(:personSearch AS TEXT), '%'))
                    OR EXISTS (
                        SELECT 1
                        FROM maintenance_record_people mrp_filter
                        JOIN maintenance_people involved_filter
                          ON involved_filter.id = mrp_filter.maintenance_person_id
                        WHERE mrp_filter.maintenance_record_id = mr.id
                          AND mrp_filter.organization_id = mr.organization_id
                          AND involved_filter.deleted_at IS NULL
                          AND LOWER(involved_filter.full_name) LIKE LOWER(CONCAT('%', CAST(:personSearch AS TEXT), '%'))
                    )
                  )
                GROUP BY
                    mr.id,
                    p.name,
                    mr.title,
                    mr.description,
                    mc.name,
                    mt.name,
                    mr.status,
                    mr.performed_at,
                    mr.scheduled_at,
                    mr.created_at,
                    mr.cost,
                    responsible.full_name
                ORDER BY COALESCE(mr.performed_at, mr.scheduled_at, mr.created_at) DESC
                LIMIT 1
                """,
                q -> {
                    q.setParameter("organizationId", organizationId);
                    q.setParameter("personSearch", personSearch);
                },
                "id",
                "propertyName",
                "title",
                "description",
                "categoryName",
                "typeName",
                "status",
                "performedAt",
                "scheduledAt",
                "maintenanceDate",
                "cost",
                "responsiblePersonName",
                "involvedPeopleNames",
                "matchSource"
        );

        if (rows.isEmpty()) {
            return AiToolAnswer.of(
                    "No encontré mantenimientos relacionados con “" + personSearch + "” como responsable principal ni como persona involucrada.",
                    "maintenance.lastByPerson",
                    "Last maintenance by person",
                    "No maintenance record matched the requested person.",
                    List.of()
            );
        }

        Map<String, Object> row = rows.getFirst();
        String answer = """
                El último mantenimiento que encontré relacionado con “%s” fue:

                - Título: %s
                - Propiedad: %s
                - Fecha usada: %s
                - Estado: %s
                - Coincidencia: %s
                - Responsable principal: %s
                - Personas involucradas: %s
                - Categoría: %s
                - Tipo: %s
                - Costo: %s
                """.formatted(
                personSearch,
                blankToDash(value(row.get("title"))),
                blankToDash(value(row.get("propertyName"))),
                blankToDash(value(row.get("maintenanceDate"))),
                blankToDash(value(row.get("status"))),
                blankToDash(value(row.get("matchSource"))),
                blankToDash(value(row.get("responsiblePersonName"))),
                blankToDash(value(row.get("involvedPeopleNames"))),
                blankToDash(value(row.get("categoryName"))),
                blankToDash(value(row.get("typeName"))),
                formatMoney(row.get("cost"))
        ).trim();

        return AiToolAnswer.of(
                answer,
                "maintenance.lastByPerson",
                "Last maintenance by person",
                "Most recent maintenance record by responsible or involved person was found.",
                rows
        );
    }
}
