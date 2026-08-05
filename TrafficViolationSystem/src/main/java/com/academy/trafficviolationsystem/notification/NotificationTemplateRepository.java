package com.academy.trafficviolationsystem.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplateEntity, Integer> {

    /**
     * Primary lookup — finds the active template for a key, channel, and language.
     * NotificationService calls this before rendering and dispatching.
     * e.g. findByTemplateKeyAndTypeAndLanguageAndIsActiveTrue("FINE_ISSUED", EMAIL, "bs")
     */
    Optional<NotificationTemplateEntity> findByTemplateKeyAndTypeAndLanguageAndIsActiveTrue(
            String templateKey, NotificationType type, String language);

    /**
     * Fallback — finds a template ignoring language (returns any active template for the key/type).
     * Used when no match is found for the preferred language.
     */
    Optional<NotificationTemplateEntity> findFirstByTemplateKeyAndTypeAndIsActiveTrue(
            String templateKey, NotificationType type);
}
