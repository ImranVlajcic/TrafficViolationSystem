import { BaseSearchObject, UuidEntity } from './common.model';
import { UserRole } from './enums';

/** UserDto (13.11) — mirrors UserEntity (13.12) minus passwordHash. */
export interface UserDto extends UuidEntity {
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  role: UserRole;
  badgeNumber?: string;
  isActive: boolean;
  lastLogInAt?: string;
  failedLogIns?: number;
  lockedUntil?: string;
}

/** UserCreateRequest (13.9). */
export interface UserCreateRequest {
  username: string;
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  role: UserRole;
  badgeNumber?: string;
}

/** UserUpdateRequest (13.18). */
export interface UserUpdateRequest {
  email?: string;
  password?: string;
  firstName?: string;
  lastName?: string;
  phoneNumber?: string;
  role?: UserRole;
  badgeNumber?: string;
  isActive?: boolean;
}

/** ChangePasswordRequest (13.3). */
export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

/** LoginRequest (13.4). */
export interface LoginRequest {
  username: string;
  password: string;
}

/**
 * LoginResponse (13.5). Matches the shape already consumed by
 * core/auth.service.ts — kept here too so future services share one
 * definition instead of two drifting copies.
 */
export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  userId: string;
  username: string;
  role: UserRole;
}

/** RefreshTokenEntity (13.6) — rarely needed client-side, kept for completeness. */
export interface RefreshTokenEntity extends UuidEntity {
  token: string;
  expiresAt: string;
  revoked: boolean;
  userAgent?: string;
  ipAdress?: string;
  userId: string;
}

/** UserSearchObject (13.16) filter fields, combine with BaseSearchObject. */
export interface UserSearchObject extends BaseSearchObject {
  search?: string;
  role?: UserRole;
  isActive?: boolean;
}