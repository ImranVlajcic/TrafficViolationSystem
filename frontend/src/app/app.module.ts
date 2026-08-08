import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { HTTP_INTERCEPTORS, HttpClientModule } from '@angular/common/http';
import { BaseChartDirective, provideCharts, withDefaultRegisterables } from 'ng2-charts';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { LoginComponent } from './auth/login.component';
import { RegisterComponent } from './auth/register.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { AuthInterceptor } from './core/auth.interceptor';
import { BrandMarkComponent } from './shared/brand-mark.component';
import { AdminLayoutComponent } from './admin/admin-layout.component';
import { AdminOverviewComponent } from './admin/admin-overview.component';
import { AdminPlaceholderComponent } from './admin/admin-placeholder.component';
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
import { JobTriggerPanelComponent } from './admin/job-trigger-panel.component';
import { AnalyticsChartComponent } from './admin/analytics-chart.component';
import { CitizenLayoutComponent } from './citizen/citizen-layout.component';
import { CitizenOverviewComponent } from './citizen/citizen-overview.component';
import { CitizenPlaceholderComponent } from './citizen/citizen-placeholder.component';
import { OfficerLayoutComponent } from './officer/officer-layout.component';
import { OfficerOverviewComponent } from './officer/officer-overview.component';
import { OfficerPlaceholderComponent } from './officer/officer-placeholder.component';
import { DataTableComponent } from './shared/data-table/data-table.component';
import { StatusBadgeComponent } from './shared/status-badge/status-badge.component';
import { PageHeaderComponent } from './shared/page-header/page-header.component';
import { ConfirmDialogComponent } from './shared/confirm-dialog/confirm-dialog.component';
import { ToastComponent } from './shared/toast/toast.component';
import { FineListComponent } from './citizen/fine-list.component';
import { FineDetailComponent } from './citizen/fine-detail.component';
import { PaymentListComponent } from './citizen/payment-list.component';
import { PaymentFormComponent } from './citizen/payment-form.component';
import { ProfileComponent } from './citizen/profile.component';
import { AppealFormComponent } from './citizen/appeal-form.component';
import { AppealListComponent } from './citizen/appeal-list.component';
import { VehicleListComponent } from './citizen/vehicle-list.component';
import { ViolationListComponent } from './citizen/violation-list.component';
import { ViolationQueueComponent } from './officer/violation-queue.component';
import { ViolationDetailComponent } from './officer/violation-detail.component';
// Officer's fine-list.component.ts exports a class with the same name as
// citizen's fine-list.component.ts — aliased since both are declared here.
import { FineListComponent as OfficerFineListComponent } from './officer/fine-list.component';
import { DriverLookupComponent } from './officer/driver-lookup.component';
import { VehicleLookupComponent } from './officer/vehicle-lookup.component';
import { AppealReviewQueueComponent } from './officer/appeal-review-queue.component';
import { AppealReviewDetailComponent } from './officer/appeal-review-detail.component';
import { ReportListComponent } from './officer/report-list.component';
import { ReportRequestFormComponent } from './officer/report-request-form.component';
import { HeatmapComponent } from './admin/heatmap.component';
import { DriverListComponent } from './admin/driver-list.component';
import { DriverFormComponent } from './admin/driver-form.component';
import { VehicleListComponentAdmin } from './admin/vehicle-list.component';
import { VehicleFormComponent } from './admin/vehicle-form.component';
import { VehicleTransferComponent } from './admin/vehicle-transfer.component';

@NgModule({
  declarations: [
    AppComponent,
    LoginComponent,
    RegisterComponent,
    DashboardComponent,
    BrandMarkComponent,
    AdminLayoutComponent,
    AdminOverviewComponent,
    AdminPlaceholderComponent,
    UserListComponent,
    UserFormComponent,
    CameraListComponent,
    CameraFormComponent,
    CameraMaintenanceComponent,
    RoadZoneListComponent,
    RoadZoneFormComponent,
    FineRuleListComponent,
    FineRuleFormComponent,
    SystemConfigListComponent,
    AuditLogListComponent,
    JobListComponent,
    JobTriggerPanelComponent,
    AnalyticsChartComponent,
    CitizenLayoutComponent,
    CitizenOverviewComponent,
    CitizenPlaceholderComponent,
    OfficerLayoutComponent,
    OfficerOverviewComponent,
    OfficerPlaceholderComponent,
    DataTableComponent,
    StatusBadgeComponent,
    PageHeaderComponent,
    ConfirmDialogComponent,
    ToastComponent,
    FineListComponent,
    FineDetailComponent,
    PaymentListComponent,
    PaymentFormComponent,
    AppealListComponent,
    AppealFormComponent,
    ProfileComponent,
    VehicleListComponent,
    ViolationListComponent,
    ViolationQueueComponent,
    ViolationDetailComponent,
    OfficerFineListComponent,
    DriverLookupComponent,
    VehicleLookupComponent,
    AppealReviewQueueComponent,
    AppealReviewDetailComponent,
    ReportListComponent,
    ReportRequestFormComponent,
    HeatmapComponent,
    DriverListComponent,
    DriverFormComponent,
    VehicleListComponentAdmin,
    VehicleFormComponent,
    VehicleTransferComponent,
  ],
  imports: [
    BrowserModule,
    FormsModule,
    HttpClientModule,
    AppRoutingModule,
    BaseChartDirective
  ],
  providers: [
    { provide: HTTP_INTERCEPTORS, useClass: AuthInterceptor, multi: true },
    provideCharts(withDefaultRegisterables())
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }