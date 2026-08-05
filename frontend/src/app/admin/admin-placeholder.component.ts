import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-admin-placeholder',
  standalone: false,
  template: `
    <p class="eyebrow">{{ code }}</p>
    <h1 class="admin-page-title">{{ title }}</h1>
    <p class="admin-page-lede">This section isn't built yet.</p>
    <div class="admin-placeholder-box">
      <p class="text-muted mb-0 small">The {{ title }} module will appear here.</p>
    </div>
  `,
  styleUrls: ['./admin-overview.component.css']
})
export class AdminPlaceholderComponent {
  title = '';
  code = '';

  constructor(route: ActivatedRoute) {
    this.title = route.snapshot.data['title'] ?? 'Section';
    this.code = route.snapshot.data['code'] ?? '';
  }
}
