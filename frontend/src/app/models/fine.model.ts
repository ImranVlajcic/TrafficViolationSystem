import { BaseSearchObject, UuidEntity } from './common.model';
import { FineStatus } from './enums';

/**
 * FineDto (8.2) — mirrors FineEntity (8.3). The doc's prose shows
 * "discountmount" and "suchargeAmount" — these read like plain typos
 * (unlike OFFICER/DISUPTED, nothing elsewhere confirms them), so this
 * uses the conventional spelling. Verify against FineEntity.java before
 * relying on it; swap in the literal doc spelling if that's what's real.
 */
export interface FineDto extends UuidEntity {
  fineNumber: string;
  amount: number;
  currency: string;
  discountAmount?: number;
  surchargeAmount?: number;
  totalDue: number; // confirmed: use this, not `amount`, for what's actually owed
  penaltyPoints: number;
  paymentDue?: string;
  paymentDueDays: number;
  earlyPayDiscountPct?: number;
  earlyPayWindow?: number;
  lateSurchargePct?: number;
  issuedAt: string;
  dueDate: string;
  paidAt?: string;
  status: FineStatus;
  pdfPath?: string;
  violationId: string;
  driverId: string;
  issuedById: string;
}

/** FineSearchObject (8.7) filter fields, combine with BaseSearchObject. */
export interface FineSearchObject extends BaseSearchObject {
  search?: string;
  status?: FineStatus;
  driverId?: string;
  violationId?: string;
  issuedById?: string;
  issuedFrom?: string;
  issuedTo?: string;
  overdueDatePassed?: boolean;
}