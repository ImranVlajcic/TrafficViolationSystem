import { AutoIdEntity, UuidEntity, BaseSearchObject } from './common.model';
import { CameraType, MaintenanceType } from './enums';

/**
 * CameraDto (5.3) — mirrors CameraEntity (5.4).
 *
 * NOTE: the doc says CameraEntity "Extends UUIDBaseEntity" (i.e. a string
 * id), but confirmed hands-on backend work established camera IDs are
 * actually Integer. Going with the confirmed-real Integer id here rather
 * than the doc's description — verify against CameraEntity.java if this
 * is stale.
 */
export interface CameraDto extends AutoIdEntity {
  serialNumber: string;
  name: string;
  cameraType: CameraType;
  latitude: number;
  longitude: number;
  directionDegrees?: number;
  locationDescription?: string;
  speedLimitKmh?: number;
  mqttTopic: string;
  isOnline: boolean;
  isActive: boolean;
  lastHeartbeatAt?: string;
  installDate: string;
  firmwareVersion?: string;
}

/** CameraCreateRequest (5.2). */
export interface CameraCreateRequest {
  serialNumber: string;
  name: string;
  cameraType: CameraType;
  latitude: number;
  longitude: number;
  directionDegrees?: number;
  locationDescription?: string;
  speedLimitKmh?: number;
  mqttTopic: string;
  installDate: string;
  firmwareVersion?: string;
}

/** CameraUpdateRequest (5.19). */
export interface CameraUpdateRequest {
  name?: string;
  cameraType?: CameraType;
  directionDegrees?: number;
  locationDescription?: string;
  speedLimitKmh?: number;
  firmwareVersion?: string;
  installDate?: string;
  isActive?: boolean;
}

/** CameraEventDto (5.5) — mirrors CameraEventEntity (5.6). */
export interface CameraEventDto extends UuidEntity {
  mqttTopic: string;
  payload: string;
  receivedAt: string;
  licensePlate?: string;
  measuredSpeed?: number;
  eventLatitude: number;
  eventLongitude: number;
  imageUrl?: string;
  processed: boolean;
  processingError?: string;
  retryCount: number;
  violationId?: string;
  cameraId: number;
}

/** CameraMaintenanceLogDto (5.10) — mirrors CameraMaintenanceLogEntity (5.11). */
export interface CameraMaintenanceLogDto extends UuidEntity {
  maintenanceType: MaintenanceType;
  scheduledDate: string;
  completedAt?: string;
  firmwareBefore?: string;
  firmwareAfter?: string;
  notes?: string;
  isCompleted: boolean;
  cameraId: number;
  performedById?: string;
}

/** LogMaintenanceRequest (5.20). */
export interface LogMaintenanceRequest {
  maintenanceType: MaintenanceType;
  scheduledDate: string;
  firmwareBefore?: string;
  firmwareAfter?: string;
  notes?: string;
  isCompleted?: boolean;
}

/**
 * MqttEventPayload (5.22) — the raw payload shape published by
 * cameras/radars over MQTT. Not usually consumed directly by the frontend,
 * kept here for completeness / debugging camera event views.
 */
export interface MqttEventPayload {
  plate?: string;
  measuredSpeedKmh?: number;
  speedLimitKmh?: number;
  imageUrl?: string;
  videoUrl?: string;
  latitude: number;
  longitude: number;
  timestampEpochMs: number;
  serial: string;
  firmwareVersion?: string;
}

/** CameraSearchObject (5.16) filter fields, combine with BaseSearchObject. */
export interface CameraSearchObject extends BaseSearchObject {
  search?: string;
  cameraType?: CameraType;
  isOnline?: boolean;
  isActive?: boolean;
}