import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common'; // <-- Fixes NG8004 (pipes: number, lowercase, date)
import { FormsModule } from '@angular/forms';     // <-- Fixes NG8002 (ngModel, ngValue)

// Adjust these import paths to match your actual project layout.
import { FineService } from '../services/fine.service';
import { FineDto } from '../models/fine.model';

/**
 * Single fine view for the signed-in citizen.
 *
 * Loaded via FineService.findById() (GET /api/fines/{id}) — that route
 * isn't scoped to /my, so nothing here stops a citizen from *requesting*
 * someone else's fine id; it relies entirely on backend-side ownership
 * checks to reject that. Don't treat this page as access-controlled on
 * the frontend.
 */
@Component({
  selector: 'app-fine-detail',
  standalone: false,
  templateUrl: './fine-detail.component.html',
  styleUrls: ['./fine-detail.component.css']
})
export class FineDetailComponent implements OnInit {
  fine: FineDto | null = null;
  loading = false;
  error = '';

  pdfError = '';
  downloadingPdf = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private fineService: FineService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.error = 'No fine specified.';
      return;
    }
    this.load(id);
  }

  load(id: string): void {
    this.loading = true;
    this.error = '';

    this.fineService.findById(id).subscribe({
      next: (fine) => {
        this.fine = fine;
        this.loading = false;
      },
      error: () => {
        this.error = 'Unable to load this fine.';
        this.loading = false;
      }
    });
  }

  get canPay(): boolean {
    return this.fine?.status === 'UNPAID' || this.fine?.status === 'OVERDUE';
  }

  payNow(): void {
    if (!this.fine) return;
    this.router.navigate(['/citizen/payments/pay', this.fine.id]);
  }

  back(): void {
    this.router.navigate(['/citizen/fines']);
  }

  /**
   * GET /api/fines/{id}/pdf returns 404 until the document has finished
   * generating server-side — there's no pdfReady flag on FineDto to
   * check up front, so this just attempts the download and surfaces a
   * friendly message on failure rather than guessing readiness.
   */
  downloadPdf(): void {
    if (!this.fine) return;
    this.pdfError = '';
    this.downloadingPdf = true;

    this.fineService.downloadPdf(this.fine.id).subscribe({
      next: (blob) => {
        this.downloadingPdf = false;
        this.saveBlob(blob, `${this.fine!.fineNumber}.pdf`);
      },
      error: () => {
        this.downloadingPdf = false;
        this.pdfError = "The fine PDF isn't ready yet — try again shortly.";
      }
    });
  }

  private saveBlob(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.click();
    URL.revokeObjectURL(url);
  }
}
