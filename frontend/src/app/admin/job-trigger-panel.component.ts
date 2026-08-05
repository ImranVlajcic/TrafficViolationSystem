import { Component, EventEmitter, Output } from '@angular/core';
import { Observable } from 'rxjs';

import { JobService } from '../services/job.service';

interface JobTriggerDef {
  key: string;
  label: string;
  description: string;
}

const JOB_TRIGGERS: JobTriggerDef[] = [
  { key: 'point-reset', label: 'Point reset', description: 'Resets driver penalty points on schedule.' },
  { key: 'overdue-fines', label: 'Overdue fines', description: 'Flags fines that have passed their due date.' },
  { key: 'notification-retry', label: 'Notification retry', description: 'Retries failed notification deliveries.' },
  { key: 'camera-heartbeat', label: 'Camera heartbeat', description: 'Checks camera connectivity status.' },
  { key: 'aggregator', label: 'Statistics aggregator', description: 'Recomputes nightly analytics snapshots.' }
];

/**
 * Reusable trigger grid — one button per background job (core module,
 * 1.2.8 Scheduling Config). Emits `triggered` after each successful
 * call so a parent page (e.g. JobListComponent) can refresh its log table.
 */
@Component({
  selector: 'app-job-trigger-panel',
  standalone: false,
  templateUrl: './job-trigger-panel.component.html',
  styleUrls: ['./job-trigger-panel.component.css']
})
export class JobTriggerPanelComponent {
  jobs = JOB_TRIGGERS;
  runningKey: string | null = null;
  resultKey: string | null = null;
  resultMessage: string | null = null;
  resultError = false;

  @Output() triggered = new EventEmitter<void>();

  constructor(private readonly jobService: JobService) {}

  trigger(job: JobTriggerDef): void {
    if (this.runningKey) {
      return;
    }
    this.runningKey = job.key;
    this.resultKey = job.key;
    this.resultMessage = null;

    this.callFor(job.key).subscribe({
      next: (message) => {
        this.resultMessage = message || `${job.label} triggered.`;
        this.resultError = false;
        this.runningKey = null;
        this.triggered.emit();
      },
      error: () => {
        this.resultMessage = `Unable to trigger ${job.label.toLowerCase()}.`;
        this.resultError = true;
        this.runningKey = null;
      }
    });
  }

  private callFor(key: string): Observable<string> {
    switch (key) {
      case 'point-reset':
        return this.jobService.triggerPointReset();
      case 'overdue-fines':
        return this.jobService.triggerOverdueFines();
      case 'notification-retry':
        return this.jobService.triggerNotificationRetry();
      case 'camera-heartbeat':
        return this.jobService.triggerCameraHeartbeat();
      case 'aggregator':
        return this.jobService.triggerAggregator();
      default:
        throw new Error(`Unknown job key: ${key}`);
    }
  }
}