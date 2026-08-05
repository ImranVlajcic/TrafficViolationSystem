import { AutoIdEntity } from './common.model';
import { JobStatus } from './enums';

/** JobExecutionLogDto (9.4) — mirrors JobExecutionLogEntity (9.5). */
export interface JobExecutionLogDto extends AutoIdEntity {
  jobName: string;
  startedAt: string;
  finishedAt?: string;
  status: JobStatus;
  recordsProcessed?: number;
  errorMessage?: string;
  triggeredBy: string;
}

/**
 * Job Admin Controller (9.3) exposes manual trigger routes per job — the
 * doc doesn't describe a request body for these, so there's no
 * JobTriggerRequest here. Add one if a real trigger endpoint turns out to
 * need parameters.
 */

/** Filters for listing job execution history, combine with BaseSearchObject. */
export interface JobSearchFilters {
  jobName?: string;
  status?: JobStatus;
}