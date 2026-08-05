import { Component, OnInit } from '@angular/core';
import { ChartConfiguration, ChartData } from 'chart.js';
import { catchError, forkJoin, of } from 'rxjs';

import { AnalyticsService } from '../services/analytics.service';
import { StatisticDto } from '../models/analytics.model';

const TREND_MONTHS = 6;

/**
 * GET /api/analytics/statistics returns one aggregated StatisticDto for a
 * periodType+from+to range — there is no dedicated trend/series endpoint.
 * ASSUMPTION, not backend-confirmed: to build the "trend" the overview
 * page's placeholder called for, this component issues one MONTHLY
 * statistics call per trailing month and assembles the series client-side.
 * If a real multi-point trend endpoint exists, swap this out for it.
 *
 * Reused two places: standalone at /admin/analytics, and embedded directly
 * in admin-overview to replace its chart placeholder box.
 */
@Component({
  selector: 'app-analytics-chart',
  standalone: false,
  templateUrl: './analytics-chart.component.html',
  styleUrls: ['./analytics-chart.component.css']
})
export class AnalyticsChartComponent implements OnInit {
  loading = false;
  error: string | null = null;

  chartData: ChartData<'line'> = { labels: [], datasets: [] };

  chartOptions: ChartConfiguration<'line'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    interaction: { mode: 'index', intersect: false },
    scales: {
      y: {
        type: 'linear',
        position: 'left',
        title: { display: true, text: 'Violations' },
        beginAtZero: true
      },
      y1: {
        type: 'linear',
        position: 'right',
        title: { display: true, text: 'Collected' },
        beginAtZero: true,
        grid: { drawOnChartArea: false }
      }
    }
  };

  constructor(private readonly analyticsService: AnalyticsService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = null;

    const months = this.trailingMonths(TREND_MONTHS);

    // A month with no nightly snapshot yet 404s (per AnalyticsService's own
    // doc comment) — catch per-request so one missing month doesn't blank
    // out the whole chart, and just render it as zero.
    const requests = months.map((m) =>
      this.analyticsService.getStatistics('MONTHLY', m.from, m.to).pipe(
        catchError(() => of({ success: false, message: '', data: null as unknown as StatisticDto }))
      )
    );

    forkJoin(requests).subscribe({
      next: (responses) => {
        const stats = responses.map((r) => r.data);
        this.chartData = {
          labels: months.map((m) => m.label),
          datasets: [
            {
              label: 'Violations',
              data: stats.map((s) => s?.totalViolations ?? 0),
              yAxisID: 'y',
              borderColor: '#c0392b',
              backgroundColor: 'rgba(192, 57, 43, 0.15)',
              tension: 0.3
            },
            {
              label: 'Collected',
              data: stats.map((s) => s?.totalCollected ?? 0),
              yAxisID: 'y1',
              borderColor: '#2f6f4f',
              backgroundColor: 'rgba(47, 111, 79, 0.15)',
              tension: 0.3
            }
          ]
        };
        this.loading = false;
      },
      error: () => {
        this.error = 'Unable to load the statistics trend.';
        this.loading = false;
      }
    });
  }

  private trailingMonths(count: number): { from: string; to: string; label: string }[] {
    const months: { from: string; to: string; label: string }[] = [];
    const now = new Date();

    for (let i = count - 1; i >= 0; i--) {
      const start = new Date(now.getFullYear(), now.getMonth() - i, 1);
      const end = new Date(now.getFullYear(), now.getMonth() - i + 1, 0);
      months.push({
        from: this.toIsoDate(start),
        to: this.toIsoDate(end),
        label: start.toLocaleString(undefined, { month: 'short', year: '2-digit' })
      });
    }

    return months;
  }

  private toIsoDate(date: Date): string {
    return date.toISOString().slice(0, 10);
  }
}