import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LoginComponent } from './auth/login.component';
import { RegisterComponent } from './auth/register.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { authGuard } from './core/auth.guard';
import { adminGuard } from './core/admin.guard';
import { citizenGuard } from './core/citizen.guard';
import { officerGuard } from './core/officer.guard';
import { AdminLayoutComponent } from './admin/admin-layout.component';
import { AdminOverviewComponent } from './admin/admin-overview.component';
import { AdminPlaceholderComponent } from './admin/admin-placeholder.component';
import { ADMIN_NAV } from './admin/admin-nav';
import { UserListComponent } from './admin/user-list.component';
import { UserFormComponent } from './admin/user-form.component';
import { CameraListComponent } from './admin/camera-list.component';
import { CameraFormComponent } from './admin/camera-form.component';
import { CameraMaintenanceComponent } from './admin/camera-maintenance.component';
import { RoadZoneListComponent } from './admin/road-zone-list.component';
import { RoadZoneFormComponent } from './admin/road-zone-form.component';
import { FineRuleListComponent } from './admin/fine-rule-list.component';
import { FineRuleFormComponent } from './admin/fine-rule-form.component';
import { SystemConfigListComponent } from './admin/system-config-list.component';
import { AuditLogListComponent } from './admin/audit-log-list.component';
import { JobListComponent } from './admin/job-list.component';
import { AnalyticsChartComponent } from './admin/analytics-chart.component';
import { CitizenLayoutComponent } from './citizen/citizen-layout.component';
import { CitizenOverviewComponent } from './citizen/citizen-overview.component';
import { CitizenPlaceholderComponent } from './citizen/citizen-placeholder.component';
import { CITIZEN_NAV } from './citizen/citizen-nav';
import { FineListComponent } from './citizen/fine-list.component';
import { FineDetailComponent } from './citizen/fine-detail.component';
import { PaymentListComponent } from './citizen/payment-list.component';
import { PaymentFormComponent } from './citizen/payment-form.component';
import { AppealListComponent } from './citizen/appeal-list.component';
import { AppealFormComponent } from './citizen/appeal-form.component';
import { ProfileComponent } from './citizen/profile.component';
import { VehicleListComponent } from './citizen/vehicle-list.component';
import { ViolationListComponent } from './citizen/violation-list.component';
import { OfficerLayoutComponent } from './officer/officer-layout.component';
import { OfficerOverviewComponent } from './officer/officer-overview.component';
import { OfficerPlaceholderComponent } from './officer/officer-placeholder.component';
import { OFFICER_NAV } from './officer/officer-nav';
import { ViolationQueueComponent } from './officer/violation-queue.component';
import { ViolationDetailComponent } from './officer/violation-detail.component';
// Officer's own fine-list.component.ts exports a class with the same name
// as citizen's — aliased on import since both are used in this file.
import { FineListComponent as OfficerFineListComponent } from './officer/fine-list.component';
import { DriverLookupComponent } from './officer/driver-lookup.component';
import { VehicleLookupComponent } from './officer/vehicle-lookup.component';
import { AppealReviewQueueComponent } from './officer/appeal-review-queue.component';
import { AppealReviewDetailComponent } from './officer/appeal-review-detail.component';
import { ReportListComponent } from './officer/report-list.component';
import { ReportRequestFormComponent } from './officer/report-request-form.component';
import { DriverListComponent } from './admin/driver-list.component';
import { DriverFormComponent } from './admin/driver-form.component';
import { VehicleListComponentAdmin } from './admin/vehicle-list.component';
import { VehicleFormComponent } from './admin/vehicle-form.component';
import { VehicleTransferComponent } from './admin/vehicle-transfer.component';
import { ViolationListComponent as AdminViolationListComponent } from './admin/violation-list.component';
import { AdminFineListComponent } from './admin/fine-list.component';
import { AdminFineDetailComponent } from './admin/fine-detail.component';
import { AdminAppealListComponent } from './admin/appeal-list.component';
import { AdminAppealDetailComponent } from './admin/appeal-detail.component';
import { AdminPaymentListComponent } from './admin/payment-list.component';
import { AdminPaymentDetailComponent } from './admin/payment-detail.component';

// Real feature routes (defined explicitly below) instead of placeholders —
// same pattern as CITIZEN_BUILT_PATHS/OFFICER_BUILT_PATHS.
const ADMIN_BUILT_PATHS = [
  'users',
  'drivers',
  'vehicles',
  'cameras',
  'road-zones',
  'fine-rules',
  'system-config',
  'audit-logs',
  'jobs',
  'analytics',
  'violations',
  'fines',
  'appeals',
  'payments',
];
const adminChildRoutes = ADMIN_NAV.flatMap((group) =>
  group.items
    .filter((item) => !ADMIN_BUILT_PATHS.includes(item.path))
    .map((item) => ({
      path: item.path,
      component: AdminPlaceholderComponent,
      data: { title: item.label, code: item.code }
    }))
);

// 'fines', 'payments', 'appeals', 'profile', 'vehicles' and 'violations'
// are all real feature routes (defined explicitly below) instead of
// placeholders. 'vehicles'/'violations' resolve their citizen-scoped
// lookup the same way 'appeals' does: current user id (GET /api/users/me)
// used as driverId/ownerId, since there's no dedicated /my endpoint.
const CITIZEN_BUILT_PATHS = ['fines', 'payments', 'appeals', 'profile', 'vehicles', 'violations'];

const citizenChildRoutes = CITIZEN_NAV.flatMap((group) =>
  group.items
    .filter((item) => !CITIZEN_BUILT_PATHS.includes(item.path))
    .map((item) => ({
      path: item.path,
      component: CitizenPlaceholderComponent,
      data: { title: item.label, code: item.code }
    }))
);

// 'violations', 'fines', 'drivers/lookup' and 'vehicles/lookup' are real
// feature routes (defined explicitly below) instead of placeholders.
// ASSUMPTION, NOT CONFIRMED: these path strings mirror the citizen side's
// naming (CITIZEN_BUILT_PATHS above) and a guess at where "Quick lookup"
// on the overview page should route to — verify against the actual
// OFFICER_NAV entries (and officer-overview.component's Quick lookup
// links, if any) and adjust the paths/filter below to match.
const OFFICER_BUILT_PATHS = ['violations', 'fines', 'drivers/lookup', 'vehicles/lookup', 'appeals', 'reports'];

const officerChildRoutes = OFFICER_NAV.flatMap((group) =>
  group.items
    .filter((item) => !OFFICER_BUILT_PATHS.includes(item.path))
    .map((item) => ({
      path: item.path,
      component: OfficerPlaceholderComponent,
      data: { title: item.label, code: item.code }
    }))
);

const routes: Routes = [
  { path: '', redirectTo: '/login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'dashboard', component: DashboardComponent, canActivate: [authGuard] },
  {
    path: 'admin',
    component: AdminLayoutComponent,
    canActivate: [adminGuard],
    children: [
      { path: '', component: AdminOverviewComponent },
      { path: 'users', component: UserListComponent, data: { title: 'Users', code: 'US' } },
      { path: 'users/new', component: UserFormComponent, data: { title: 'New user', code: 'US' } },
      { path: 'users/:id/edit', component: UserFormComponent, data: { title: 'Edit user', code: 'US' } },
      { path: 'drivers', component: DriverListComponent, data: { title: 'Drivers', code: 'DR' } },
      { path: 'drivers/new', component: DriverFormComponent, data: { title: 'New driver', code: 'DR' } },
      { path: 'drivers/:id/edit', component: DriverFormComponent, data: { title: 'Edit driver', code: 'DR' } },
      { path: 'cameras', component: CameraListComponent, data: { title: 'Cameras', code: 'CM' } },
      { path: 'cameras/new', component: CameraFormComponent, data: { title: 'New camera', code: 'CM' } },
      { path: 'cameras/:id/edit', component: CameraFormComponent, data: { title: 'Edit camera', code: 'CM' } },
      {
        path: 'cameras/:id/maintenance',
        component: CameraMaintenanceComponent,
        data: { title: 'Camera maintenance', code: 'CM' }
      },
      { path: 'road-zones', component: RoadZoneListComponent, data: { title: 'Road zones', code: 'RZ' } },
      {
        path: 'road-zones/new',
        component: RoadZoneFormComponent,
        data: { title: 'New road zone', code: 'RZ' }
      },
      {
        path: 'road-zones/:id/edit',
        component: RoadZoneFormComponent,
        data: { title: 'Edit road zone', code: 'RZ' }
      },
      { path: 'fine-rules', component: FineRuleListComponent, data: { title: 'Fine rules', code: 'FR' } },
      {
        path: 'fine-rules/new',
        component: FineRuleFormComponent,
        data: { title: 'New fine rule', code: 'FR' }
      },
      {
        path: 'fine-rules/:id/edit',
        component: FineRuleFormComponent,
        data: { title: 'Edit fine rule', code: 'FR' }
      },
      {
        path: 'system-config',
        component: SystemConfigListComponent,
        data: { title: 'System config', code: 'SC' }
      },
      { path: 'audit-logs', component: AuditLogListComponent, data: { title: 'Audit logs', code: 'AL' } },
      { path: 'jobs', component: JobListComponent, data: { title: 'Jobs', code: 'JB' } },
      { path: 'analytics', component: AnalyticsChartComponent, data: { title: 'Analytics', code: 'AN' } },
      { path: 'vehicles', component: VehicleListComponentAdmin, data: { title: 'Vehicles', code: 'VH' } },
      { path: 'vehicles/new', component: VehicleFormComponent, data: { title: 'New vehicle', code: 'VH' } },
      { path: 'vehicles/:id/edit', component: VehicleFormComponent, data: { title: 'Edit vehicle', code: 'VH' } },
      {
        path: 'vehicles/:id/transfer-ownership',
        component: VehicleTransferComponent,
        data: { title: 'Transfer ownership', code: 'VH' }
      },
      {
        path: 'violations',
        component: AdminViolationListComponent,
        data: { title: 'Violations', code: 'VL' }
      },
      { path: 'fines', component: AdminFineListComponent },
      { path: 'fines/:id', component: AdminFineDetailComponent },
      { path: 'appeals', component: AdminAppealListComponent, data: { title: 'Appeals', code: 'AP' } },
      { path: 'appeals/:id', component: AdminAppealDetailComponent, data: { title: 'Appeal review', code: 'AP' } },
      { path: 'payments', component: AdminPaymentListComponent, data: { title: 'Payments', code: 'PY' } },
      { path: 'payments/:id', component: AdminPaymentDetailComponent, data: { title: 'Payment detail', code: 'PY' } },
      ...adminChildRoutes
    ]
  },
  {
    path: 'citizen',
    component: CitizenLayoutComponent,
    canActivate: [citizenGuard],
    children: [
      { path: '', component: CitizenOverviewComponent },
      { path: 'fines', component: FineListComponent, data: { title: 'Fines', code: 'FN' } },
      { path: 'fines/:id', component: FineDetailComponent, data: { title: 'Fine detail', code: 'FN' } },
      { path: 'payments', component: PaymentListComponent, data: { title: 'Payments', code: 'PY' } },
      {
        path: 'payments/pay/:fineId',
        component: PaymentFormComponent,
        data: { title: 'Pay fine', code: 'PY' }
      },
      { path: 'appeals', component: AppealListComponent, data: { title: 'Appeals', code: 'AP' } },
      { path: 'appeals/new', component: AppealFormComponent, data: { title: 'New appeal', code: 'AP' } },
      {
        path: 'appeals/new/:violationId',
        component: AppealFormComponent,
        data: { title: 'New appeal', code: 'AP' }
      },
      { path: 'profile', component: ProfileComponent, data: { title: 'Profile', code: 'PR' } },
      { path: 'vehicles', component: VehicleListComponent, data: { title: 'Vehicles', code: 'VH' } },
      { path: 'violations', component: ViolationListComponent, data: { title: 'Violations', code: 'VL' } },
      ...citizenChildRoutes
    ]
  },
  {
    path: 'officer',
    component: OfficerLayoutComponent,
    canActivate: [officerGuard],
    children: [
      { path: '', component: OfficerOverviewComponent },
      { path: 'violations', component: ViolationQueueComponent, data: { title: 'Violations', code: 'VL' } },
      {
        path: 'violations/:id',
        component: ViolationDetailComponent,
        data: { title: 'Violation detail', code: 'VL' }
      },
      { path: 'fines', component: OfficerFineListComponent, data: { title: 'Fines', code: 'FN' } },
      {
        path: 'drivers/lookup',
        component: DriverLookupComponent,
        data: { title: 'Driver lookup', code: 'DR' }
      },
      {
        path: 'vehicles/lookup',
        component: VehicleLookupComponent,
        data: { title: 'Vehicle lookup', code: 'VH' }
      },
      { path: 'appeals', component: AppealReviewQueueComponent, data: { title: 'Appeals', code: 'AP' } },
      { path: 'appeals/:id', component: AppealReviewDetailComponent, data: { title: 'Appeal review', code: 'AP' } },
      { path: 'reports', component: ReportListComponent, data: { title: 'Reports', code: 'RP' } },
      { path: 'reports/new', component: ReportRequestFormComponent, data: { title: 'Request report', code: 'RP' } },
      ...officerChildRoutes
    ]
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }