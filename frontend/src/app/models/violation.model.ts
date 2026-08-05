import { UuidEntity, BaseSearchObject } from './common.model';
import { DetectionMethod, ViolationStatus, ViolationType } from './enums';

/** ViolationDto (15.15) — mirrors ViolationEntity (15.16). */
export interface ViolationDto extends UuidEntity {
  referenceNumber: string;
  violationType: ViolationType;
  detectionMethod: DetectionMethod;
  status: ViolationStatus;
  occuredAt: string; // doc's literal field name (likely "occurredAt") — verify against the entity
  locationLangitued: number; // doc's literal field name (likely "locationLatitude") — verify against the entity
  locationLongitude: number;
  locationDescription?: string;
  measuredSpeed?: number;
  speedLimit?: number;
  evidenceImageUrl?: string;
  evidenceVideoUrl?: string;
  notes?: string;
  isAutomatic: boolean;
  vehicleId: string;
  fineId?: string;
  cameraId?: number;
  driverId?: string;
  officierId?: string;
  reviewedById?: string;
}

/** ViolationCreateRequest (15.14). */
export interface ViolationCreateRequest {
  violationType: ViolationType;
  detectionMethod: DetectionMethod;
  occuredAt: string;
  locationLangitued: number;
  locationLongitude: number;
  locationDescription?: string;
  measuredSpeed?: number;
  speedLimit?: number;
  evidenceImageUrl?: string;
  evidenceVideoUrl?: string;
  notes?: string;
  vehicleId: string;
  driverId?: string;
  cameraId?: number;
}

/** ViolationUpdateRequest (15.23). */
export interface ViolationUpdateRequest {
  driverId?: string;
  locationDescription?: string;
  measuredSpeed?: number;
  speedLimit?: number;
  evidenceImageUrl?: string;
  evidenceVideoUrl?: string;
  notes?: string;
}

/** ReviewViolationRequest (15.11) — used on confirm/dismiss. */
export interface ReviewViolationRequest {
  reviewNotes: string;
}

/** ViolationSearchObject (15.19) filter fields, combine with BaseSearchObject. */
export interface ViolationSearchObject extends BaseSearchObject {
  search?: string;
  status?: ViolationStatus;
  violaitonType?: ViolationType; // doc's literal field name (likely "violationType")
  detectionMethod?: DetectionMethod;
  vehicleid?: string; // doc's literal field name (likely "vehicleId")
  driverId?: string;
  officierId?: string;
  cameraId?: number;
  isAutomatic?: boolean;
  fromDate?: string;
  toDate?: string;
}