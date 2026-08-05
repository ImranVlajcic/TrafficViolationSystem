package com.academy.trafficviolationsystem.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async thread pool configuration.
 *
 * {@code @EnableAsync} activates Spring's {@code @Async} support so any
 * method annotated with {@code @Async} runs on a background thread instead
 * of blocking the HTTP request thread.
 *
 * Two named executors are defined:
 *
 *   "notificationExecutor"
 *     Used by NotificationService for email and SMS dispatch.
 *     Sized for I/O-bound work (waits on external SMTP / SMS gateway).
 *     A violation with 1 notification should not block fine issuance.
 *
 *   "pdfExecutor"
 *     Used by FinePdfService and PaymentConfirmationPdfService.
 *     PDF generation is CPU-bound but short; a small pool is fine.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    /**
     * Executor for notification dispatch (email, SMS).
     * I/O-bound — larger pool so threads can wait on external services in parallel.
     */
    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("notif-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler((task, pool) ->
                log.error("Notification task rejected — queue full: {}", task.toString())
        );
        executor.initialize();
        return executor;
    }

    /**
     * Executor for PDF generation (fine PDFs, payment receipts).
     * CPU-bound but short-lived — a small pool prevents over-saturation.
     */
    @Bean(name = "pdfExecutor")
    public Executor pdfExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("pdf-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);   // PDF jobs should never take > 1 min
        executor.setRejectedExecutionHandler((task, pool) ->
                log.error("PDF generation task rejected — queue full: {}", task.toString())
        );
        executor.initialize();
        return executor;
    }
}
