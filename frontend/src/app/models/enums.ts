/**
 * Every backend enum, spelled exactly as documented in the backend's own
 * technical doc. Some of these look like typos — they might be. A few are
 * CONFIRMED real (OFFICER, DISUPTED, DISSMISED, RESIEDNTAL — these caused
 * an actual bug earlier when the frontend used the "corrected" spelling).
 * The rest are marked below with how confident that confidence is. Verify
 * against the actual Java enum before relying on an unmarked one.
 */

// --- User module (13.15) ---------------------------------------------------
export type UserRole = 'ADMIN' | 'OFFICER' | 'CITIZEN' | 'SYSTEM'; // OFFICER confirmed, not a typo

// --- Driver module ----------------------------------------------------------
// (license category isn't enumerated in the doc — treat as free text for now)

// --- Vehicle module (14.1, 14.15) -------------------------------------------
export type FuelType =
  | 'GASOLINE'
  | 'DIESEL'
  | 'ELECTRIC'
  | 'HYBRID'
  | 'LPG'
  | 'CNG'
  | 'HYDROGEN'
  | 'OTHER';

export type VehicleType = 'CAR' | 'MOTORCYCLE' | 'VAN' | 'TRUCK' | 'BUS' | 'TRACTOR' | 'OTHER';

// --- Violation module (15.1, 15.21, 15.22) ----------------------------------
export type DetectionMethod = 'CAMERA_AUTO' | 'RADAR_AUTO' | 'MANUAL_OFFICER';

// DISUPTED and DISSMISED confirmed, not typos — match exactly.
export type ViolationStatus = 'PENDING' | 'CONFIRMED' | 'DISUPTED' | 'DISSMISED' | 'CLOSED';

export type ViolationType =
  | 'SPEEDING'
  | 'RED_LIGHT'
  | 'NO_SEATBELT'
  | 'PHONE_USE'
  | 'WRONG_WAY'
  | 'PARKING'
  | 'DUI'
  | 'NO_INSURANCE'
  | 'OVERLOAD'
  | 'ILLEGAL_OVERTAKE'
  | 'WRONG_LANE'
  | 'PEDESTRIAN_CROSSING'
  | 'EXPIRED_REGISTRATION'
  | 'OTHER';

// --- Fine module (8.9) -------------------------------------------------------
export type FineStatus = 'UNPAID' | 'OVERDUE' | 'DISPUTED' | 'PAID' | 'CANCELLED';

// --- Appeal module (3.8) ------------------------------------------------------
export type AppealStatus = 'SUBMITTED' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED' | 'WITHDRAWN';

// --- Payment module (11.7, 11.12) --------------------------------------------
export type PaymentMethod = 'CREDIT_CARD' | 'DEBIT_CARD' | 'BANK_TRANSFER' | 'CASH' | 'ONLINE_PORTAL';
export type PaymentStatus = 'PENDING' | 'SUCCESS' | 'FAILED' | 'REFUNDED' | 'REVERSED';

// --- Camera module (5.18, 5.21) ----------------------------------------------
export type CameraType = 'ANPR' | 'SPEED_RADAR' | 'RED_LIGHT' | 'MOBILE_RADAR' | 'OVERHEAD';
export type MaintenanceType =
  | 'PHYSICAL_INSPECTION'
  | 'CALIBRATION'
  | 'FIRMWARE_UPDATE'
  | 'FAULT_REPAIR'
  | 'HARDWARE_REPLACEMENT';

// --- Road zone module (12.10) -------------------------------------------------
// RESIEDNTAL confirmed, not a typo — match exactly.
export type ZoneType =
  | 'SCHOOL'
  | 'RESIEDNTAL'
  | 'HIGHWAY'
  | 'CITY_CENTER'
  | 'HOSPITAL'
  | 'CONSTRUCTION'
  | 'INDUSTRIAL';

// --- Analytics / Report module (2.9, 2.12, 2.18, 2.19) -------------------------
export type PeriodType = 'DAILY' | 'WEEKLY' | 'MONTHLY';
export type ReportFormat = 'PDF' | 'CSV';
export type ReportStatus = 'PENDING' | 'GENERATED' | 'DONE' | 'FAILED';
export type ReportType =
  | 'MONTHLY_FINES'
  | 'OFFICER_ACTIVITY'
  | 'ZONE_RANKING'
  | 'DRIVER_HISTORY'
  | 'CAMERA_UPTIME';

// --- System config module (6.1) -----------------------------------------------
export type ConfigDataType = 'STRING' | 'INTEGER' | 'DECIMAL' | 'BOOLEAN' | 'JSON';

// --- Job scheduler module (9.8) ------------------------------------------------
// Doc literally shows "SUCCES" (one S). Unlike OFFICER/DISUPTED this isn't
// separately confirmed anywhere else, and it's a single-letter slip in a
// document full of prose typos — likely just a typo, not a real enum value.
// Verify against JobStatus.java before shipping against this one.
export type JobStatus = 'RUNNING' | 'SUCCESS' | 'FAILED' | 'SKIPPED';

// --- Notification module (10.8, 10.11) ------------------------------------------
export type NotificationStatus = 'PENDING' | 'FAILED' | 'SENT' | 'RETRYING';
export type NotificationType = 'EMAIL' | 'SMS' | 'IN_APP';