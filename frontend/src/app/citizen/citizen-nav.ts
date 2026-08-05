export interface CitizenNavItem {
  code: string;
  label: string;
  path: string;
}

export interface CitizenNavGroup {
  label: string;
  items: CitizenNavItem[];
}

/**
 * Sidebar structure for the citizen workspace — everything here is scoped
 * to the signed-in citizen's own records only.
 */
export const CITIZEN_NAV: CitizenNavGroup[] = [
  {
    label: 'My records',
    items: [
      { code: 'VH', label: 'Vehicles', path: 'vehicles' },
      { code: 'VL', label: 'Violations', path: 'violations' },
      { code: 'FN', label: 'Fines', path: 'fines' },
      { code: 'PY', label: 'Payments', path: 'payments' },
      { code: 'AP', label: 'Appeals', path: 'appeals' }
    ]
  },
  {
    label: 'Account',
    items: [{ code: 'PR', label: 'Profile', path: 'profile' }]
  }
];
