import { BaseSearchObject, UuidEntity } from './common.model';
import { FuelType, VehicleType } from './enums';

/** VehicleDto (14.5) — mirrors VehicleEntity (14.6). */
export interface VehicleDto extends UuidEntity {
  licencePlate: string;
  vin: string;
  make: string;
  model: string;
  year: number;
  color: string;
  vehicleType: VehicleType;
  enginceCc?: number; // doc's literal field name (likely "engineCc") — verify against the entity
  fuelType: FuelType;
  registrationDate: string;
  registrationExpiry: string;
  ownerId: string;
  isStolen: boolean;
  isActive: boolean;
}

/** VehicleCreateRequest (14.4). */
export interface VehicleCreateRequest {
  licencePlate: string;
  vin: string;
  make: string;
  model: string;
  year: number;
  color: string;
  vehicleType: VehicleType;
  enginceCc?: number;
  fuelType: FuelType;
  registrationDate: string;
  registrationExpiry: string;
  ownerId: string;
}

/** VehicleUpdateRequest (14.16). */
export interface VehicleUpdateRequest {
  make?: string;
  model?: string;
  year?: number;
  color?: string;
  vehicleType?: VehicleType;
  fuelType?: FuelType;
  registrationDate?: string;
  registrationExpiry?: string;
  isActive?: boolean;
}

/** TransferOwnershipRequest (14.2). */
export interface TransferOwnershipRequest {
  newOwnerId: string;
  transferDate: string;
  notes?: string;
}

/** VehicleOwnershipHistoryDto (14.8) — mirrors VehicleOwnershipHistoryEntity (14.9). */
export interface VehicleOwnershipHistoryDto extends UuidEntity {
  transferDate: string;
  notes?: string;
  vehicleId: string;
  previousOwnerId: string;
  newOwnerId: string;
}

/** VehicleSearchObject (14.13) filter fields, combine with BaseSearchObject. */
export interface VehicleSearchObject extends BaseSearchObject {
  search?: string;
  ownerId?: string;
  vehicleType?: VehicleType;
  fuelType?: FuelType;
  isStolen?: boolean;
  isActive?: boolean;
  registraionExpires?: string; // doc's literal field name (likely "registrationExpires")
  year?: number;
}