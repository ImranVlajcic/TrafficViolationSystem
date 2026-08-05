package com.academy.trafficviolationsystem.core.config.seed;

import com.academy.trafficviolationsystem.configuration.ConfigDataType;
import com.academy.trafficviolationsystem.configuration.SystemConfigEntity;
import com.academy.trafficviolationsystem.configuration.SystemConfigRepository;
import org.springframework.stereotype.Component;

/**
 * Seeds the small set of system-wide configuration rows the app reads at
 * runtime (penalty point threshold, default currency, MQTT heartbeat window...).
 */
@Component
public class SystemConfigSeeder {

    private final SystemConfigRepository systemConfigRepository;

    public SystemConfigSeeder(SystemConfigRepository systemConfigRepository) {
        this.systemConfigRepository = systemConfigRepository;
    }

    public void seed() {
        if (systemConfigRepository.count() > 0) return;

        save("DRIVER_SUSPENSION_POINT_THRESHOLD", "12", ConfigDataType.INTEGER,
                "DRIVER", "Penalty points at which a driver's license is suspended", false);

        save("DEFAULT_CURRENCY", "BAM", ConfigDataType.STRING,
                "FINE", "Default currency for fine amounts", false);

        save("FINE_PAYMENT_DUE_DAYS_DEFAULT", "30", ConfigDataType.INTEGER,
                "FINE", "Default number of days before a fine becomes overdue", true);

        save("APPEAL_WINDOW_DAYS", "30", ConfigDataType.INTEGER,
                "FINE", "Days after a violation within which an appeal can be filed", true);

        save("CAMERA_HEARTBEAT_TIMEOUT_MINUTES", "10", ConfigDataType.INTEGER,
                "MQTT", "Minutes without a heartbeat before a camera is marked offline", true);

        save("NOTIFICATION_MAX_RETRY_COUNT", "4", ConfigDataType.INTEGER,
                "NOTIFICATION", "Maximum dispatch attempts before a notification is marked FAILED", true);

        save("PDF_COMPANY_NAME", "Traffic Violation System", ConfigDataType.STRING,
                "PDF", "Organisation name printed on generated PDF documents", true);

        save("EARLY_PAYMENT_DISCOUNT_DEFAULT_PCT", "0.10", ConfigDataType.DECIMAL,
                "FINE", "Default early-payment discount percentage", true);
    }

    private void save(String key, String value, ConfigDataType type,
                      String category, String description, boolean editable) {
        SystemConfigEntity config = new SystemConfigEntity();
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setDataType(type);
        config.setCategory(category);
        config.setDescription(description);
        config.setEditable(editable);
        systemConfigRepository.save(config);
    }
}