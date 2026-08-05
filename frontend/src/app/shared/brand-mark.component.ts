import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-brand-mark',
  standalone: false,
  template: `
    <span class="plate-badge">
      <span class="plate-badge__text">
        <span class="plate-badge__id">TRAFICC</span>
        <span class="plate-badge__caption">Enforcement System</span>
      </span>
    </span>
  `
})
export class BrandMarkComponent {
  /** Reserved for future variants (e.g. compact mark for a nav bar). */
  @Input() compact = false;
}
