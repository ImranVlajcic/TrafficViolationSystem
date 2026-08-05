import { Component } from '@angular/core';
import { Toast, ToastService } from '../../core/toast.service';

@Component({
  selector: 'app-toast',
  standalone: false,
  templateUrl: './toast.component.html',
  styleUrls: ['./toast.component.css']
})
export class ToastComponent {
  toasts$;

  constructor(private toastService: ToastService) {
    this.toasts$ = this.toastService.toasts$;
  }

  dismiss(toast: Toast): void {
    this.toastService.dismiss(toast.id);
  }
}
