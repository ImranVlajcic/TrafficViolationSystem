import { BaseSearchObject, UuidEntity } from './common.model';

/** DriverDto (7.2) — mirrors DriverEntity (7.3). */
export interface DriverDto extends UuidEntity {
  licenceNumber: string;
  nationalId: string;
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  email: string;
  phoneNumber: string;
  adress: string;
  licenseCategory: string;
  licenseIssuedAt: string;
  licenceExpiresAt: string;
  penaltyPoints: number;
  isSuspended: boolean;
  suspendedUntil?: string;
  userId?: string;
}

/** DriverCreateRequest (7.1). */
export interface DriverCreateRequest {
  licenceNumber: string;
  nationalId: string;
  firstName: string;
  lastName: string;
  dateOfBirth: string;
  email: string;
  phoneNumber: string;
  adress: string;
  licenseCategory: string;
  licenseIssuedAt: string;
  licenceExpiresAt: string;
}

/** DriverUpdateRequest (7.12). */
export interface DriverUpdateRequest {
  firstName?: string;
  lastName?: string;
  phoneNumber?: string;
  adress?: string;
  licenseCategory?: string;
  licenseIssuedAt?: string;
  licenceExpiresAt?: string;
}

/** SuspendDriverRequest (7.17). */
export interface SuspendDriverRequest {
  reason: string;
  endDate: string;
  violationId: string;
}

/** DriverPointHistoryDto (7.5) — mirrors DriverPointHistoryEntity (7.6). */
export interface DriverPointHistoryDto extends UuidEntity {
  changeAmount: number;
  pointsBefore: number;
  pointsAfter: number;
  reason: string;
  violationId: string;
  occurdAt: string; // doc's literal field name — verify spelling against the entity
  driverId: string;
}

/** LicenseSuspensionDto (7.13) — mirrors LicenseSuspensionEntity (7.14). */
export interface LicenseSuspensionDto extends UuidEntity {
  reason: string;
  startDate: string;
  endDate: string;
  liftedAt?: string;
  pointsAtTime: number;
  isActive: boolean;
  violationId: string;
  driverId: string;
  suspendedById: string;
}

/** DriverSearchObject (7.10) filter fields, combine with BaseSearchObject. */
export interface DriverSearchObject extends BaseSearchObject<string | number> {
  search?: string;
  isSuspended?: boolean;
  licenseExpired?: boolean;
  licenseCategory?: string;
}