export interface AdminNavItem {
  code: string;
  label: string;
  path: string;
}

export interface AdminNavGroup {
  label: string;
  items: AdminNavItem[];
}

/**
 * Sidebar structure for the admin workspace. Each item carries a short
 * reference code (mirrors the reference/fine/appeal numbers already used
 * across the system) instead of an icon font.
 */
export const ADMIN_NAV: AdminNavGroup[] = [
  {
    label: 'People',
    items: [
      { code: 'US', label: 'Users', path: 'users' },
      { code: 'DR', label: 'Drivers', path: 'drivers' },
      { code: 'VH', label: 'Vehicles', path: 'vehicles' }
    ]
  },
  {
    label: 'Enforcement',
    items: [
      { code: 'VL', label: 'Violations', path: 'violations' },
      { code: 'CM', label: 'Cameras', path: 'cameras' },
      { code: 'RZ', label: 'Road zones', path: 'road-zones' },
      { code: 'FR', label: 'Fine rules', path: 'fine-rules' }
    ]
  },
  {
    label: 'Finance',
    items: [
      { code: 'FN', label: 'Fines', path: 'fines' },
      { code: 'AP', label: 'Appeals', path: 'appeals' },
      { code: 'PY', label: 'Payments', path: 'payments' }
    ]
  },
  {
    label: 'Insights',
    items: [
      { code: 'AN', label: 'Analytics', path: 'analytics' },
      { code: 'RP', label: 'Reports', path: 'reports' }
    ]
  },
  {
    label: 'System',
    items: [
      { code: 'SC', label: 'System config', path: 'system-config' },
      { code: 'JB', label: 'Jobs', path: 'jobs' },
      { code: 'AL', label: 'Audit logs', path: 'audit-logs' }
    ]
  }
];
