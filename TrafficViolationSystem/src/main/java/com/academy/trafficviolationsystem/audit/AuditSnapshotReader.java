package com.academy.trafficviolationsystem.audit;

import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditSnapshotReader {

    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public Object snapshot(Class<?> entityClass, Object id) {
        Object entity = entityManager.find(entityClass, id);

        if (entity == null) {
            return null;
        }

        // Your existing serialization/snapshot logic
        return entity;
    }
}
