package com.academy.trafficviolationsystem.analytics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GeneratedReportRepository extends JpaRepository<GeneratedReportEntity, UUID> {

    List<GeneratedReportEntity> findByRequestedByIdOrderByCreatedDesc(UUID userId);

    List<GeneratedReportEntity> findByStatus(ReportStatus status);
}
