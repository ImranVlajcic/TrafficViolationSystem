import { BaseSearchObject, UuidEntity } from './common.model';
import { PaymentMethod, PaymentStatus } from './enums';

/** PaymentDto (11.3) — mirrors PaymentEntity (11.4). */
export interface PaymentDto extends UuidEntity {
  transactionId: string;
  amount: number;
  currency: string;
  method: PaymentMethod;
  status: PaymentStatus;
  paidAt?: string;
  gatewayResponse?: string;
  receiptPdfFile?: string;
  notes?: string;
  fineId: string;
  paidById: string;
}

/** PaymentRequest (11.9) — the body sent to POST /api/payments. */
export interface PaymentRequest {
  fineId: string;
  method: PaymentMethod;
  notes?: string;
}

/**
 * PaymentResult (11.13) — response body for a payment attempt. Field list
 * isn't spelled out in the doc beyond "returns data relevant to payment";
 * this is a reasonable approximation, verify against PaymentResult.java.
 */
export interface PaymentResult {
  success: boolean;
  transactionId: string;
  status: PaymentStatus;
  message?: string;
  paidAt?: string;
}

/** PaymentSearchObject (11.10) filter fields, combine with BaseSearchObject. */
export interface PaymentSearchObject extends BaseSearchObject {
  fineId?: string;
  status?: PaymentStatus;
  method?: PaymentMethod;
  paidById?: string;
  fromDate?: string;
  toDate?: string;
}