package com.academy.trafficviolationsystem.core.config;

import com.academy.trafficviolationsystem.core.config.seed.SeedRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Entry point for dev database seeding. The actual orchestration logic lives
 * in SeedRunner.run(), which is @Transactional — kept there rather than
 * inline here because a CommandLineRunner lambda can't itself carry a
 * @Transactional annotation, and running the whole seed in one transaction
 * is what prevents LazyInitializationException across all the sub-seeders.
 *
 * IMPORTANT: This is a @Profile("dev") bean. Run with
 * spring.profiles.active=dev (e.g. in application.properties) to activate
 * it. Remove the @Profile annotation if you'd rather it always run.
 */
@Configuration
@Profile("dev")
public class DataSeeder {

    @Bean
    public CommandLineRunner initDatabase(SeedRunner seedRunner) {
        return args -> seedRunner.run();
    }
}