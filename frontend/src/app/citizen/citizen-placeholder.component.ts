import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-citizen-placeholder',
  standalone: false,
  template: `
    <p class="eyebrow">{{ code }}</p>
    <h1 class="citizen-page-title">{{ title }}</h1>
    <p class="citizen-page-lede">This section isn't built yet.</p>
    <div class="citizen-placeholder-box">
      <p class="text-muted mb-0 small">Your {{ title | lowercase }} will appear here.</p>
    </div>
  `,
  styleUrls: ['./citizen-overview.component.css']
})
export class CitizenPlaceholderComponent {
  title = '';
  code = '';

  constructor(route: ActivatedRoute) {
    this.title = route.snapshot.data['title'] ?? 'Section';
    this.code = route.snapshot.data['code'] ?? '';
  }
}
