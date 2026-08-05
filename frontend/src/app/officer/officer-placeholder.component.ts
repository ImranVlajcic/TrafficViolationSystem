import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-officer-placeholder',
  standalone: false,
  template: `
    <p class="eyebrow">{{ code }}</p>
    <h1 class="officer-page-title">{{ title }}</h1>
    <p class="officer-page-lede">This section isn't built yet.</p>
    <div class="officer-placeholder-box">
      <p class="text-muted mb-0 small">The {{ title }} module will appear here.</p>
    </div>
  `,
  styleUrls: ['./officer-overview.component.css']
})
export class OfficerPlaceholderComponent {
  title = '';
  code = '';

  constructor(route: ActivatedRoute) {
    this.title = route.snapshot.data['title'] ?? 'Section';
    this.code = route.snapshot.data['code'] ?? '';
  }
}
