package com.academy.trafficviolationsystem.core.config.seed;

import com.academy.trafficviolationsystem.appeal.ViolationAppealEntity;
import com.academy.trafficviolationsystem.camera.CameraEntity;
import com.academy.trafficviolationsystem.camera.CameraEventEntity;
import com.academy.trafficviolationsystem.driver.DriverEntity;
import com.academy.trafficviolationsystem.fine.FineEntity;
import com.academy.trafficviolationsystem.payment.PaymentEntity;
import com.academy.trafficviolationsystem.rodezone.RoadZoneEntity;
import com.academy.trafficviolationsystem.vehicle.VehicleEntity;
import com.academy.trafficviolationsystem.violation.FineRuleEntity;
import com.academy.trafficviolationsystem.violation.ViolationEntity;
import com.academy.trafficviolationsystem.violation.ViolationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Runs every sub-seeder inside a single transaction (@Transactional keeps
 * one Hibernate session open for the whole method). This is what actually
 * prevents LazyInitializationException: no session — without it, each
 * repository call opens/closes its own session, and any lazy relation
 * (fine.getDriver(), appeal.getDriver(), driver.getUser()...) touched after
 * the session that loaded it closes will blow up. With the whole run in one
 * transaction, every proxy stays initializable until the method returns.
 *
 * DataSeeder's CommandLineRunner just calls run() — keep the seeding logic
 * here rather than in the CommandLineRunner lambda itself, since lambdas
 * can't carry a @Transactional annotation.
 */
@Component
public class SeedRunner {

    private final SystemConfigSeeder systemConfigSeeder;
    private final NotificationTemplateSeeder notificationTemplateSeeder;
    private final FineRuleSeeder fineRuleSeeder;
    private final RoadZoneSeeder roadZoneSeeder;
    private final CameraSeeder cameraSeeder;
    private final UserSeeder userSeeder;
    private final DriverSeeder driverSeeder;
    private final VehicleSeeder vehicleSeeder;
    private final VehicleOwnershipHistorySeeder vehicleOwnershipHistorySeeder;
    private final CameraEventSeeder cameraEventSeeder;
    private final ViolationSeeder violationSeeder;
    private final FineSeeder fineSeeder;
    private final PaymentSeeder paymentSeeder;
    private final AppealSeeder appealSeeder;
    private final NotificationSeeder notificationSeeder;
    private final ViolationRepository violationRepository;

    public SeedRunner(SystemConfigSeeder systemConfigSeeder,
                      NotificationTemplateSeeder notificationTemplateSeeder,
                      FineRuleSeeder fineRuleSeeder,
                      RoadZoneSeeder roadZoneSeeder,
                      CameraSeeder cameraSeeder,
                      UserSeeder userSeeder,
                      DriverSeeder driverSeeder,
                      VehicleSeeder vehicleSeeder,
                      VehicleOwnershipHistorySeeder vehicleOwnershipHistorySeeder,
                      CameraEventSeeder cameraEventSeeder,
                      ViolationSeeder violationSeeder,
                      FineSeeder fineSeeder,
                      PaymentSeeder paymentSeeder,
                      AppealSeeder appealSeeder,
                      NotificationSeeder notificationSeeder,
                      ViolationRepository violationRepository) {
        this.systemConfigSeeder = systemConfigSeeder;
        this.notificationTemplateSeeder = notificationTemplateSeeder;
        this.fineRuleSeeder = fineRuleSeeder;
        this.roadZoneSeeder = roadZoneSeeder;
        this.cameraSeeder = cameraSeeder;
        this.userSeeder = userSeeder;
        this.driverSeeder = driverSeeder;
        this.vehicleSeeder = vehicleSeeder;
        this.vehicleOwnershipHistorySeeder = vehicleOwnershipHistorySeeder;
        this.cameraEventSeeder = cameraEventSeeder;
        this.violationSeeder = violationSeeder;
        this.fineSeeder = fineSeeder;
        this.paymentSeeder = paymentSeeder;
        this.appealSeeder = appealSeeder;
        this.notificationSeeder = notificationSeeder;
        this.violationRepository = violationRepository;
    }

    @Transactional
    public void run() {
        // ── config / reference data ──────────────────────────────────
        systemConfigSeeder.seed();
        notificationTemplateSeeder.seed();
        List<FineRuleEntity> fineRules = fineRuleSeeder.seed();
        List<RoadZoneEntity> zones = roadZoneSeeder.seed();
        List<CameraEntity> cameras = cameraSeeder.seed(zones);

        // ── people & vehicles ─────────────────────────────────────────
        UserSeeder.SeededUsers users = userSeeder.seed();
        List<DriverEntity> drivers = driverSeeder.seed(users.citizens);
        List<VehicleEntity> vehicles = vehicleSeeder.seed(drivers);
        vehicleOwnershipHistorySeeder.seed(vehicles, drivers);

        if (violationRepository.count() > 0) {
            System.out.println("Seed data already present — skipping violation/fine/payment/appeal/notification seeding.");
            return;
        }

        // ── camera events → violations ───────────────────────────────
        List<CameraEventEntity> cameraEvents = cameraEventSeeder.seed(cameras, vehicles);
        List<ViolationEntity> violations = violationSeeder.seed(cameraEvents, vehicles, drivers, users.officers);

        // ── fines (+ driver points/suspensions), payments, appeals ────
        FineSeeder.FineSeedResult fineResult = fineSeeder.seed(violations, fineRules, users.officers);
        List<FineEntity> fines = fineResult.fines;
        List<PaymentEntity> payments = paymentSeeder.seed(fines);
        List<ViolationAppealEntity> appeals = appealSeeder.seed(violations, users.officers);
        notificationSeeder.seed(fines, payments, appeals, fineResult.suspensions);

        System.out.println("======== Dev database seeded ========");
        System.out.println("Users:      admin/admin123, officer/officer123, officer2-15/officer123, citizen1-90/citizen123");
        System.out.println("Drivers:    " + drivers.size());
        System.out.println("Vehicles:   " + vehicles.size());
        System.out.println("Violations: " + violations.size());
        System.out.println("Fines:      " + fines.size());
        System.out.println("======================================");
    }
}