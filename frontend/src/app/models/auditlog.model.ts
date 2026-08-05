import { BaseSearchObject, UuidEntity } from './common.model';

/** AuditLogDto (4.5) — mirrors AuditLogEntity (4.6). */
export interface AuditLogDto extends UuidEntity {
  action: string;
  entityType: string;
  entityId: string;
  actorId: string;
  actorUsername: string;
  ipAdress?: string;
  beforeSnapshot?: string;
  afterSnapshot?: string;
  description?: string;
  occuredAt: string; // doc's literal field name (likely "occurredAt") — verify against the entity
}

/** AuditSearchObject (4.10) filter fields, combine with BaseSearchObject. */
export interface AuditLogSearchObject extends BaseSearchObject {
  action?: string;
  entityType?: string;
  entityId?: string;
  actorId?: string;
  fromDate?: string;
  toDate?: string;
}