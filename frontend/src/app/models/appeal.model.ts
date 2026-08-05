import { BaseSearchObject, UuidEntity } from './common.model';
import { AppealStatus } from './enums';

/** AppealDto (3.3) — mirrors ViolationAppealEntity (3.11). */
export interface AppealDto extends UuidEntity {
  appealNumber: string;
  reason: string;
  evidenceUrl?: string;
  status: AppealStatus;
  submittedAt: string;
  reviewedAt?: string;
  reviewNotes?: string;
  fineId?: string;
  violationId: string;
  driverId: string;
  reviewedById?: string;
}

/** AppealCreateRequest (3.2). */
export interface AppealCreateRequest {
  violationId: string;
  reason: string;
  evidenceUrl?: string;
}

/** AppealUpdateRequest (3.9). */
export interface AppealUpdateRequest {
  reason?: string;
  evidenceUrl?: string;
}

/** ReviewUpdateRequest (3.10) — used on start-review/approve/reject. */
export interface ReviewUpdateRequest {
  reviewNotes: string;
}

/** AppealSearchObject (3.6) filter fields, combine with BaseSearchObject. */
export interface AppealSearchObject extends BaseSearchObject {
  status?: AppealStatus;
  driverId?: string;
  violationId?: string;
  reviewedById?: string;
  fromDate?: string;
  toDate?: string;
}

export interface ReviewAppealRequest {
  reviewNotes: string;
}