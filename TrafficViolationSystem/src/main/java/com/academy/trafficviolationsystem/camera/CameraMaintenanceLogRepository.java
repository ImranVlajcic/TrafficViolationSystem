package com.academy.trafficviolationsystem.camera;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CameraMaintenanceLogRepository extends JpaRepository<CameraMaintenanceLogEntity, UUID> {

    List<CameraMaintenanceLogEntity> findByCameraIdOrderByCompletedAtDesc(Integer cameraId);

    List<CameraMaintenanceLogEntity> findByCameraIdAndIsCompletedFalseOrderByScheduledDateAsc(Integer cameraId);
}
