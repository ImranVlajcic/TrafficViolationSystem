import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { ApiResponse } from '../core/models/api-response.model';

// Adjust these import paths to match your actual model locations.
import { HeatmapDataDto } from '../models/analytics.model';
import { StatisticDto } from '../models/analytics.model';

// TODO: point this at your actual API base — swap for environment.apiUrl
const API_BASE = '/api/analytics';

/** Read-only analytics endpoints — all data is pre-computed by nightly aggregation jobs. */
@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  constructor(private readonly http: HttpClient) {}

  /**
   * GET /api/analytics/top-danger-zones?periodEnd=...
   * Top 10 highest-severity accident hotspots. Defaults to yesterday's
   * data if periodEnd is omitted.
   */
  getTopDangerZones(periodEnd?: string): Observable<ApiResponse<HeatmapDataDto[]>> {
    const params = periodEnd ? { periodEnd } : undefined;
    return this.http.get<ApiResponse<HeatmapDataDto[]>>(`${API_BASE}/top-danger-zones`, { params });
  }

  /**
   * GET /api/analytics/statistics?periodType=&from=&to=
   * KPI snapshot for a period. Snapshots are generated at 00:30 nightly —
   * returns 404 if none exists yet for the requested period.
   */
  getStatistics(
    periodType: 'DAILY' | 'WEEKLY' | 'MONTHLY',
    from: string,
    to: string
  ): Observable<ApiResponse<StatisticDto>> {
    return this.http.get<ApiResponse<StatisticDto>>(`${API_BASE}/statistics`, {
      params: { periodType, from, to },
    });
  }

  /**
   * GET /api/analytics/heatmap?from=&to=
   * Pre-computed hotspot clusters for a date range; falls back to a live
   * query if the nightly job hasn't run yet. Feed straight into
   * Leaflet.js HeatLayer or Google Maps heatmap.
   */
  getHeatmap(from: string, to: string): Observable<ApiResponse<HeatmapDataDto[]>> {
    return this.http.get<ApiResponse<HeatmapDataDto[]>>(`${API_BASE}/heatmap`, { params: { from, to } });
  }
}
