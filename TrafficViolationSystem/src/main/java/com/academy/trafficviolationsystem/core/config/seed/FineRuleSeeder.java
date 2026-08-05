package com.academy.trafficviolationsystem.core.config.seed;

import com.academy.trafficviolationsystem.violation.FineRuleEntity;
import com.academy.trafficviolationsystem.violation.FineRuleRepository;
import com.academy.trafficviolationsystem.violation.ViolationType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Seeds one FineRuleEntity per ViolationType with plausible BAM amounts
 * and penalty points. Amounts are illustrative, not legally sourced.
 */
@Component
public class FineRuleSeeder {

    private final FineRuleRepository fineRuleRepository;

    public FineRuleSeeder(FineRuleRepository fineRuleRepository) {
        this.fineRuleRepository = fineRuleRepository;
    }

    public void seed() {
        if (fineRuleRepository.count() > 0) return;

        rule(ViolationType.SPEEDING, 100, 50, 500, 3, "Čl. 172. Zakona o osnovama sigurnosti saobraćaja — prekoračenje brzine.");
        rule(ViolationType.RED_LIGHT, 150, null, null, 4, "Čl. 156. — prolazak kroz crveno svjetlo na semaforu.");
        rule(ViolationType.NO_SEATBELT, 60, null, null, 1, "Čl. 178. — nekorištenje sigurnosnog pojasa.");
        rule(ViolationType.PHONE_USE, 80, null, null, 2, "Čl. 174. — korištenje mobilnog telefona za vrijeme vožnje.");
        rule(ViolationType.WRONG_WAY, 200, null, null, 5, "Čl. 160. — vožnja u suprotnom smjeru.");
        rule(ViolationType.PARKING, 40, 20, 100, 0, "Čl. 190. — nepropisno parkiranje.");
        rule(ViolationType.DUI, 500, 300, 1500, 8, "Čl. 174a. — vožnja pod utjecajem alkohola.");
        rule(ViolationType.NO_INSURANCE, 250, null, null, 2, "Čl. 201. — vožnja bez važećeg osiguranja.");
        rule(ViolationType.OVERLOAD, 300, 150, 600, 3, "Čl. 210. — prekoračenje dozvoljene nosivosti vozila.");
        rule(ViolationType.ILLEGAL_OVERTAKE, 180, null, null, 4, "Čl. 165. — nepropisno pretjecanje.");
        rule(ViolationType.WRONG_LANE, 90, null, null, 2, "Čl. 168. — vožnja nepropisnom trakom.");
        rule(ViolationType.PEDESTRIAN_CROSSING, 120, null, null, 3, "Čl. 182. — nepropuštanje pješaka na pješačkom prijelazu.");
        rule(ViolationType.EXPIRED_REGISTRATION, 100, null, null, 1, "Čl. 240. — vožnja s isteklom registracijom vozila.");
        rule(ViolationType.OTHER, 70, 30, 300, 1, "Ostali prekršaji koji nisu posebno navedeni.");
    }

    private void rule(ViolationType type, int base, Integer min, Integer max,
                      int points, String description) {
        FineRuleEntity fineRule = new FineRuleEntity();
        fineRule.setViolationType(type);
        fineRule.setBaseAmount(BigDecimal.valueOf(base));
        fineRule.setMinAmount(min == null ? null : BigDecimal.valueOf(min));
        fineRule.setMaxAmount(max == null ? null : BigDecimal.valueOf(max));
        fineRule.setPenaltyPoints(points);
        fineRule.setPaymentDueDays(30);
        fineRule.setEarlyPayDiscountPct(new BigDecimal("0.10"));
        fineRule.setEarlyPayWindowDays(7);
        fineRule.setLateSurchargePct(new BigDecimal("0.10"));
        fineRule.setDescription(description);
        fineRule.setActive(true);
        fineRuleRepository.save(fineRule);
    }
}