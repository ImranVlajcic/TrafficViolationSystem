package com.academy.trafficviolationsystem.core.config.seed;

import com.academy.trafficviolationsystem.notification.NotificationTemplateEntity;
import com.academy.trafficviolationsystem.notification.NotificationTemplateRepository;
import com.academy.trafficviolationsystem.notification.NotificationType;
import org.springframework.stereotype.Component;

/**
 * Seeds one EMAIL (bs) template per known template key, plus a couple of
 * shorter SMS variants for the most frequent notifications. Not exhaustive
 * across every type/language combination — enough for the frontend/backend
 * to have a real template to resolve during testing.
 */
@Component
public class NotificationTemplateSeeder {

    private final NotificationTemplateRepository templateRepository;

    public NotificationTemplateSeeder(NotificationTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    public void seed() {
        if (templateRepository.count() > 0) return;

        email("FINE_ISSUED", "Izdata je nova kazna",
                "Poštovani {{driverName}}, izdata vam je kazna {{fineNumber}} u iznosu od {{amount}} " +
                        "za prekršaj {{violationRef}} ({{violationType}}). Rok za plaćanje: {{dueDate}}.");
        sms("FINE_ISSUED",
                "Kazna {{fineNumber}} na iznos {{amount}} izdata. Rok placanja: {{dueDate}}.");

        email("PAYMENT_SUCCESS", "Uplata primljena",
                "Poštovani {{driverName}}, vaša uplata za kaznu {{fineNumber}} je uspješno evidentirana. " +
                        "Broj transakcije: {{transactionId}}. Hvala.");

        email("APPEAL_APPROVED", "Vaša žalba je usvojena",
                "Poštovani {{driverName}}, vaša žalba {{appealNumber}} na prekršaj {{violationRef}} je " +
                        "usvojena. Napomena službenika: {{reviewNotes}}");

        email("APPEAL_REJECTED", "Vaša žalba je odbijena",
                "Poštovani {{driverName}}, vaša žalba {{appealNumber}} na prekršaj {{violationRef}} je " +
                        "odbijena. Napomena službenika: {{reviewNotes}}");

        email("SUSPENSION_NOTICE", "Obavijest o suspenziji vozačke dozvole",
                "Poštovani {{driverName}}, vaša vozačka dozvola broj {{licenseNumber}} je suspendovana " +
                        "zbog {{penaltyPoints}} kaznenih poena, do datuma {{suspendedUntil}}.");

        email("LICENSE_EXPIRY_WARNING", "Vozačka dozvola uskoro ističe",
                "Poštovani {{driverName}}, vaša vozačka dozvola broj {{licenseNumber}} ističe " +
                        "{{expiresAt}}. Molimo obnovite je na vrijeme.");

        email("ACCOUNT_LOCKED", "Nalog privremeno zaključan",
                "Vaš korisnički nalog je privremeno zaključan zbog više neuspjelih pokušaja prijave. " +
                        "Pokušajte ponovo za nekoliko minuta.");

        email("VIOLATION_CONFIRMED", "Prekršaj potvrđen",
                "Poštovani {{driverName}}, automatski detektovani prekršaj {{violationRef}} " +
                        "({{violationType}}) je potvrđen od strane službenika.");
    }

    private void email(String key, String subject, String body) {
        save(key, NotificationType.EMAIL, subject, body);
    }

    private void sms(String key, String body) {
        save(key, NotificationType.SMS, null, body);
    }

    private void save(String key, NotificationType type, String subject, String body) {
        NotificationTemplateEntity template = new NotificationTemplateEntity();
        template.setTemplateKey(key);
        template.setType(type);
        template.setSubject(subject);
        template.setBodyTemplate(body);
        template.setLanguage("bs");
        template.setActive(true);
        templateRepository.save(template);
    }
}