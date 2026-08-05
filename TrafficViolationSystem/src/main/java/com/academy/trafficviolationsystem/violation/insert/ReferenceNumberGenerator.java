package com.academy.trafficviolationsystem.violation.insert;

import com.academy.trafficviolationsystem.violation.ViolationRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Generates violation reference numbers in TRF-{YEAR}-{6-digit-seq} format.
 *
 * Extracted into its own bean so both ReferenceNumberHandler (insert chain)
 * and ViolationService.generateReferenceNumber() (existing public API,
 * possibly called from elsewhere) share one implementation instead of two
 * copies of the year/count logic drifting apart.
 *
 * The count query is non-atomic, which is fine here — reference numbers are
 * unique by the DB constraint, so a collision just triggers a retry on the
 * next request. (Same tradeoff as the original implementation.)
 */
@Component
public class ReferenceNumberGenerator {

    private final ViolationRepository violationRepository;

    public ReferenceNumberGenerator(ViolationRepository violationRepository) {
        this.violationRepository = violationRepository;
    }

    public String generate() {
        int year = LocalDate.now().getYear();
        LocalDateTime yearStart = LocalDate.of(year, 1, 1).atStartOfDay();
        LocalDateTime yearEnd   = LocalDate.of(year + 1, 1, 1).atStartOfDay();
        long count = violationRepository.countByYear(yearStart, yearEnd);
        return String.format("TRF-%d-%06d", year, count + 1);
    }
}
