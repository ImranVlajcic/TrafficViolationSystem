package com.academy.trafficviolationsystem.core.config.seed;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

/**
 * Small random-generation helper shared by every seeder.
 * Uses a fixed seed so a wiped+reseeded dev database produces the same
 * data each time — makes it easier to write frontend tests against
 * specific known values (e.g. always the same number of OVERDUE fines).
 */
public final class SeedRandom {

    public static final Random RNG = new Random(42);

    private SeedRandom() {}

    public static <T> T pick(T[] values) {
        return values[RNG.nextInt(values.length)];
    }

    public static <T> T pick(List<T> values) {
        return values.get(RNG.nextInt(values.size()));
    }

    public static int intBetween(int minInclusive, int maxInclusive) {
        return minInclusive + RNG.nextInt(maxInclusive - minInclusive + 1);
    }

    public static boolean chance(double probability) {
        return RNG.nextDouble() < probability;
    }

    public static LocalDate pastDate(int minDaysAgo, int maxDaysAgo) {
        return LocalDate.now().minusDays(intBetween(minDaysAgo, maxDaysAgo));
    }

    public static LocalDateTime pastDateTime(int minDaysAgo, int maxDaysAgo) {
        return pastDate(minDaysAgo, maxDaysAgo).atTime(
                intBetween(6, 22), intBetween(0, 59), intBetween(0, 59)
        );
    }

    public static String digits(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(RNG.nextInt(10));
        }
        return sb.toString();
    }

    public static String letters(int length) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(alphabet.charAt(RNG.nextInt(alphabet.length())));
        }
        return sb.toString();
    }
}