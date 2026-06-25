package com.tamias.ai.tool.service;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.repository.MaintenanceLastByPersonToolRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MaintenanceLastByPersonReadOnlyToolService {

    private final MaintenanceLastByPersonToolRepository repository;

    public MaintenanceLastByPersonReadOnlyToolService(MaintenanceLastByPersonToolRepository repository) {
        this.repository = repository;
    }

    public AiToolAnswer lastMaintenanceByPerson(String userQuestion) {
        return repository.lastMaintenanceByPerson(userQuestion);
    }
}
