package com.academy.trafficviolationsystem.camera;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CameraEventRepository extends JpaRepository<CameraEventEntity, UUID> {

    /** Unprocessed events for a specific camera — used by retry job. */
    List<CameraEventEntity> findByCameraIdAndProcessedFalseOrderByReceivedAtAsc(Integer cameraId);

    /** All unprocessed events across all cameras — retry job entry point. */
    @Query("""
        SELECT e FROM CameraEventEntity e
        WHERE e.processed = false
          AND e.retryCount < :maxRetries
        ORDER BY e.receivedAt ASC
        """)
    List<CameraEventEntity> findUnprocessedForRetry(@Param("maxRetries") int maxRetries);

    /** Recent events for a camera — used by the admin camera detail page. */
    List<CameraEventEntity> findByCameraIdOrderByReceivedAtDesc(Integer cameraId);

    @Modifying
    @Query("""
        UPDATE CameraEventEntity e
        SET e.processed = true, e.violationId = :violationId, e.processingError = null
        WHERE e.id = :id
        """)
    void markProcessed(@Param("id") UUID id, @Param("violationId") UUID violationId);

    @Modifying
    @Query("""
        UPDATE CameraEventEntity e
        SET e.processingError = :error, e.retryCount = e.retryCount + 1
        WHERE e.id = :id
        """)
    void markFailed(@Param("id") UUID id, @Param("error") String error);

    /** Clean up old processed events older than a retention period. */
    @Modifying
    @Query("""
        DELETE FROM CameraEventEntity e
        WHERE e.processed = true
          AND e.receivedAt < :before
        """)
    int deleteProcessedBefore(@Param("before") LocalDateTime before);
}
