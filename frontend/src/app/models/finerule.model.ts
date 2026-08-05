import { AutoIdEntity, BaseSearchObject } from './common.model';
import { ViolationType } from './enums';

/** FineRuleDto (15.4) — mirrors FineRuleEntity (15.5). */
export interface FineRuleDto extends AutoIdEntity {
  violationType: ViolationType;
  baseAmount: number;
  minAmount: number;
  maxAmount: number;
  penaltyPoints: number;
  paymentDueDates: number;
  earlyPayDiscountPct?: number;
  earlyPayWindowDay?: number;
  lateSurchargePct?: number;
  description?: string;
  isActive: boolean;
}

/** FineRuleCreateRequest (15.3). */
export interface FineRuleCreateRequest {
  violationType: ViolationType;
  baseAmount: number;
  minAmount: number;
  maxAmount: number;
  penaltyPoints: number;
  paymentDueDates: number;
  earlyPayDiscountPct?: number;
  earlyPayWindowDay?: number;
  lateSurchargePct?: number;
  description?: string;
}

/**
 * FineRuleUpdateRequest (15.10). The doc's field names here
 * (paymentDueDays, earlyPayWindowsDays) drift slightly from the create
 * request and entity (paymentDueDates, earlyPayWindowDay) — almost
 * certainly inconsistent doc prose rather than a real API difference.
 * Kept aligned with the entity/create request field names below; verify
 * against FineRuleUpdateRequest.java if the request 400s.
 */
export interface FineRuleUpdateRequest {
  baseAmount?: number;
  minAmount?: number;
  maxAmount?: number;
  penaltyPoints?: number;
  paymentDueDates?: number;
  earlyPayDiscountPct?: number;
  earlyPayWindowDay?: number;
  lateSurchargePct?: number;
  description?: string;
  isActive?: boolean;
}

/** FineRuleSearchObject (15.8) filter fields, combine with BaseSearchObject. */
export interface FineRuleSearchObject extends BaseSearchObject {
  violationType?: ViolationType;
  isActive?: boolean;
}