export interface OfficerNavItem {
  code: string;
  label: string;
  path: string;
}

export interface OfficerNavGroup {
  label: string;
  items: OfficerNavItem[];
}

/**
 * Sidebar structure for the officer workspace — scoped to what the backend's
 * officer-tier security group exposes (also reachable by ADMIN, per
 * SecurityConfig's "officer endpoints also accessible by admin" rule).
 */
export const OFFICER_NAV: OfficerNavGroup[] = [
  {
    label: 'Enforcement',
    items: [
      { code: 'VL', label: 'Violations', path: 'violations' },
      { code: 'FN', label: 'Fines', path: 'fines' }
    ]
  },
  {
    label: 'Lookups',
    items: [
      { code: 'DR', label: 'Drivers', path: 'drivers' },
      { code: 'VH', label: 'Vehicles', path: 'vehicles' }
    ]
  },
  {
    label: 'Reviews',
    items: [{ code: 'AP', label: 'Appeals', path: 'appeals' }]
  },
  {
    label: 'Reports',
    items: [{ code: 'RP', label: 'Reports', path: 'reports' }]
  }
];
