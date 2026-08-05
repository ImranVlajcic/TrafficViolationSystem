package com.academy.trafficviolationsystem.camera;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CameraRepository extends JpaRepository<CameraEntity, Integer> {

    Optional<CameraEntity> findBySerialNumber(String serialNumber);

    Optional<CameraEntity> findByMqttTopic(String mqttTopic);

    boolean existsBySerialNumber(String serialNumber);

    boolean existsByMqttTopic(String mqttTopic);

    List<CameraEntity> findByIsActiveTrueOrderByNameAsc();

    List<CameraEntity> findByIsOnlineFalseAndIsActiveTrue();

    int countByZoneId(Integer zoneId);

    // ── heartbeat updates ─────────────────────────────────────────────────

    @Modifying
    @Query("""
        UPDATE CameraEntity c
        SET c.isOnline = true, c.lastHeartbeatAt = :now
        WHERE c.id = :id
        """)
    void recordHeartbeat(@Param("id") Integer id, @Param("now") LocalDateTime now);

    @Modifying
    @Query("""
        UPDATE CameraEntity c
        SET c.isOnline = true, c.lastHeartbeatAt = :now, c.firmwareVersion = :firmware
        WHERE c.id = :id
        """)
    void recordHeartbeatWithFirmware(@Param("id") Integer id,
                                     @Param("now") LocalDateTime now,
                                     @Param("firmware") String firmware);

    /**
     * CameraHeartbeatJob calls this to mark cameras offline
     * when no heartbeat has been received within the threshold window.
     */
    @Modifying
    @Query("""
        UPDATE CameraEntity c
        SET c.isOnline = false
        WHERE c.isOnline = true
          AND c.isActive = true
          AND (c.lastHeartbeatAt IS NULL OR c.lastHeartbeatAt < :threshold)
        """)
    int markStaleAsOffline(@Param("threshold") LocalDateTime threshold);

    @Modifying
    @Query("UPDATE CameraEntity c SET c.zoneId = :zoneId WHERE c.id = :cameraId")
    void updateZoneId(@Param("cameraId") Integer cameraId,
                    @Param("zoneId")   Integer zoneId);

    @Modifying
    @Query("UPDATE CameraEntity c SET c.zoneId = NULL WHERE c.zoneId = :zoneId")
    void clearZoneId(@Param("zoneId") Integer zoneId);

    @Query("SELECT COUNT(c) FROM CameraEntity c WHERE c.isOnline = true")
    int countOnlineAtDate(LocalDate date);
}
