import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';

interface FineDto {
  id: string;
  fineNumber: string;
  totalDue: number;
  status: string;
  dueDate?: string;
  issuedAt?: string;
}

interface PaymentDto {
  id: string;
  transactionId: string;
  amount: number;
  status: string;
  paidAt?: string;
}

@Component({
  selector: 'app-citizen-overview',
  standalone: false,
  templateUrl: './citizen-overview.component.html',
  styleUrls: ['./citizen-overview.component.css']
})
export class CitizenOverviewComponent implements OnInit {
  fines: FineDto[] = [];
  finesLoading = false;
  finesError = '';

  payments: PaymentDto[] = [];
  paymentsLoading = false;
  paymentsError = '';

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.loadFines();
    this.loadPayments();
  }

  get unpaidFines(): FineDto[] {
    return this.fines.filter((f) => f.status === 'UNPAID' || f.status === 'OVERDUE');
  }

  get totalDue(): number {
    return this.unpaidFines.reduce((sum, f) => sum + (f.totalDue ?? 0), 0);
  }

  get recentFines(): FineDto[] {
    return this.fines.slice(0, 5);
  }

  get recentPayments(): PaymentDto[] {
    return this.payments.slice(0, 5);
  }

  loadFines(): void {
    this.finesLoading = true;
    this.finesError = '';

    this.http.get<any>('/api/fines/my').subscribe({
      next: (response) => {
        this.fines = Array.isArray(response?.data?.resultList) ? response.data.resultList : [];
        this.finesLoading = false;
      },
      error: () => {
        this.finesError = 'Unable to load your fines.';
        this.finesLoading = false;
      }
    });
  }

  loadPayments(): void {
    this.paymentsLoading = true;
    this.paymentsError = '';

    this.http.get<any>('/api/payments/my').subscribe({
      next: (response) => {
        this.payments = Array.isArray(response?.data?.resultList) ? response.data.resultList : [];
        this.paymentsLoading = false;
      },
      error: () => {
        this.paymentsError = 'Unable to load your payments.';
        this.paymentsLoading = false;
      }
    });
  }
}
