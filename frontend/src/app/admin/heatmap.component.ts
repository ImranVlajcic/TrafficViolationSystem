import { AfterViewInit, Component, ElementRef, OnDestroy, ViewChild } from '@angular/core';
import * as L from 'leaflet';

import { AnalyticsService } from '../services/analytics.service';
import { HeatmapDataDto } from '../models/analytics.model';

function toIsoDate(date: Date): string {
  return date.toISOString().slice(0, 10);
}

// ASSUMPTION, NOT BACKEND-CONFIRMED: severityScore's numeric range isn't
// documented anywhere in the doc excerpts seen so far, so hotspots are
// colored relative to the highest score in the *current* result set
// rather than against a fixed absolute scale. Revisit if the real range
// turns out to be fixed (e.g. always 0-100) — a fixed scale would make
// colors comparable across different date ranges, which relative scaling
// does not.
function severityColor(score: number, maxScore: number): string {
  const ratio = maxScore > 0 ? Math.min(Math.max(score / maxScore, 0), 1) : 0;
  const hue = 120 - ratio * 120; // green (120) -> red (0)
  return `hsl(${hue}, 75%, 45%)`;
}

@Component({
  selector: 'app-heatmap',
  standalone: false,
  templateUrl: './heatmap.component.html',
  styleUrls: ['./heatmap.component.css']
})
export class HeatmapComponent implements AfterViewInit, OnDestroy {
  @ViewChild('mapContainer', { static: true }) mapContainerRef!: ElementRef<HTMLDivElement>;

  loading = false;
  error = '';
  hotspots: HeatmapDataDto[] = [];

  // Default window: trailing 30 days. Matches the "recent activity" feel
  // of the rest of admin-overview without needing a dedicated default
  // from the (undocumented) heatmap endpoint itself.
  from: string;
  to: string;

  private map: L.Map | undefined;
  private hotspotLayer: L.LayerGroup | undefined;

  constructor(private readonly analyticsService: AnalyticsService) {
    const today = new Date();
    const past = new Date();
    past.setDate(past.getDate() - 30);
    this.to = toIsoDate(today);
    this.from = toIsoDate(past);
  }

  ngAfterViewInit(): void {
    this.map = L.map(this.mapContainerRef.nativeElement, {
  center: [43.9159, 17.6791], // Bosnia and Herzegovina
  zoom: 8
});

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors',
      maxZoom: 19
    }).addTo(this.map);

    this.hotspotLayer = L.layerGroup().addTo(this.map);

    this.loadHeatmap();
  }

  ngOnDestroy(): void {
    this.map?.remove();
  }

  loadHeatmap(): void {
    this.loading = true;
    this.error = '';

    this.analyticsService.getHeatmap(this.from, this.to).subscribe({
      next: (response) => {
        this.hotspots = response?.data ?? [];
        this.renderHotspots();
        this.loading = false;
      },
      error: () => {
        this.hotspots = [];
        this.renderHotspots();
        // Per the service's own doc comment, /heatmap falls back to a
        // live query if the nightly job hasn't run — so a failure here
        // is a genuine error, not just "no snapshot yet" (that case
        // the backend is expected to handle by returning live data).
        this.error = 'Unable to load heatmap data for the selected period.';
        this.loading = false;
      }
    });
  }

  private renderHotspots(): void {
    if (!this.hotspotLayer) {
      return;
    }

    this.hotspotLayer.clearLayers();

    if (!this.hotspots.length) {
      return;
    }

    const maxScore = Math.max(...this.hotspots.map((h) => h.severityScore));
    const bounds: L.LatLngExpression[] = [];

    this.hotspots.forEach((hotspot) => {
      const latlng: L.LatLngExpression = [hotspot.latitude, hotspot.longitude];
      bounds.push(latlng);

      const color = severityColor(hotspot.severityScore, maxScore);

      L.circle(latlng, {
        radius: hotspot.radiusMeters,
        color,
        fillColor: color,
        fillOpacity: 0.35,
        weight: 1.5
      })
        .bindPopup(this.buildPopup(hotspot))
        .addTo(this.hotspotLayer as L.LayerGroup);
    });

    this.map?.fitBounds(L.latLngBounds(bounds), { padding: [24, 24], maxZoom: 14 });
  }

  private buildPopup(hotspot: HeatmapDataDto): string {
    const label =
      hotspot.locationLabel ?? `${hotspot.latitude.toFixed(4)}, ${hotspot.longitude.toFixed(4)}`;
    return `
      <strong>${label}</strong><br>
      Dominant type: ${hotspot.dominantType}<br>
      Violations: ${hotspot.violationCount}<br>
      Severity score: ${hotspot.severityScore}
    `;
  }
}