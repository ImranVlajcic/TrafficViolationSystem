import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

// Adjust this import path to match your actual model location.
import { JobExecutionLogDto } from '../models/job.model';

// TODO: point this at your actual API base — swap for environment.apiUrl
const API_BASE = '/api/admin/jobs';

/**
 * ADMIN only. Manual triggers for the scheduled background jobs
 * (core module, 1.2.8 Scheduling Config) plus their execution logs.
 * Trigger endpoints return a plain string status message, not a DTO.
 */
@Injectable({ providedIn: 'root' })
export class JobService {
  constructor(private readonly http: HttpClient) {}

  /** POST /api/admin/jobs/point-reset/trigger */
  triggerPointReset(): Observable<string> {
    return this.http.post(`${API_BASE}/point-reset/trigger`, {}, { responseType: 'text' });
  }

  /** POST /api/admin/jobs/overdue-fines/trigger */
  triggerOverdueFines(): Observable<string> {
    return this.http.post(`${API_BASE}/overdue-fines/trigger`, {}, { responseType: 'text' });
  }

  /** POST /api/admin/jobs/notification-retry/trigger */
  triggerNotificationRetry(): Observable<string> {
    return this.http.post(`${API_BASE}/notification-retry/trigger`, {}, { responseType: 'text' });
  }

  /** POST /api/admin/jobs/camera-heartbeat/trigger */
  triggerCameraHeartbeat(): Observable<string> {
    return this.http.post(`${API_BASE}/camera-heartbeat/trigger`, {}, { responseType: 'text' });
  }

  /** POST /api/admin/jobs/aggregator/trigger */
  triggerAggregator(): Observable<string> {
    return this.http.post(`${API_BASE}/aggregator/trigger`, {}, { responseType: 'text' });
  }

  /** GET /api/admin/jobs/logs?jobName=... — jobName is optional, filters by job. */
  getLogs(jobName?: string): Observable<JobExecutionLogDto[]> {
    const params = jobName ? { jobName } : undefined;
    return this.http.get<JobExecutionLogDto[]>(`${API_BASE}/logs`, { params });
  }

  /** GET /api/admin/jobs/logs/stuck — jobs stuck in RUNNING past their expected window. */
  getStuckJobs(): Observable<JobExecutionLogDto[]> {
    return this.http.get<JobExecutionLogDto[]>(`${API_BASE}/logs/stuck`);
  }

  /** GET /api/admin/jobs/logs/failed */
  getFailedJobs(): Observable<JobExecutionLogDto[]> {
    return this.http.get<JobExecutionLogDto[]>(`${API_BASE}/logs/failed`);
  }
}
