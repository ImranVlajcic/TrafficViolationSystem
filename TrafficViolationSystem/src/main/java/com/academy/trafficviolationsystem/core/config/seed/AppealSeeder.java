package com.academy.trafficviolationsystem.core.config.seed;

import com.academy.trafficviolationsystem.appeal.AppealStatus;
import com.academy.trafficviolationsystem.appeal.ViolationAppealEntity;
import com.academy.trafficviolationsystem.appeal.AppealRepository;
import com.academy.trafficviolationsystem.user.UserEntity;
import com.academy.trafficviolationsystem.violation.ViolationEntity;
import com.academy.trafficviolationsystem.violation.ViolationStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Seeds appeals for:
 *  - all 8 DISUPTED violations (5 SUBMITTED, 3 UNDER_REVIEW — no decision yet)
 *  - the DISSMISED violations that carry a fine (APPROVED — fine cancelled)
 *  - 5 CONFIRMED violations (3 REJECTED, 2 WITHDRAWN — fine stands)
 *
 * DISUPTED/DISSMISED spelled exactly as the real ViolationStatus enum.
 */
@Component
public class AppealSeeder {

    private final AppealRepository appealRepository;

    public AppealSeeder(AppealRepository appealRepository) {
        this.appealRepository = appealRepository;
    }

    public List<ViolationAppealEntity> seed(List<ViolationEntity> violations, List<UserEntity> officers) {
        if (appealRepository.count() > 0) return appealRepository.findAll();

        List<ViolationAppealEntity> created = new ArrayList<>();
        int seq = 1;
        int year = LocalDate.now().getYear();

        List<ViolationEntity> disputed = new ArrayList<>();
        List<ViolationEntity> dissmisedWithFine = new ArrayList<>();
        List<ViolationEntity> confirmed = new ArrayList<>();
        for (ViolationEntity v : violations) {
            if (v.getStatus() == ViolationStatus.DISPUTED) disputed.add(v);
            else if (v.getStatus() == ViolationStatus.DISMISSED && v.getFineId() != null) dissmisedWithFine.add(v);
            else if (v.getStatus() == ViolationStatus.CONFIRMED) confirmed.add(v);
        }

        // ── DISUPTED → no decision yet ──────────────────────────────────────
        for (int i = 0; i < disputed.size(); i++) {
            AppealStatus status = i < 5 ? AppealStatus.SUBMITTED : AppealStatus.UNDER_REVIEW;
            created.add(save(disputed.get(i), status, null, null, seq, year));
            seq++;
        }

        // ── DISSMISED-with-fine → approved, fine cancelled ─────────────────
        for (ViolationEntity v : dissmisedWithFine) {
            created.add(save(v, AppealStatus.APPROVED,
                    SeedRandom.pick(officers),
                    "Nakon uvida u dokaze, prekršaj se povlači — kazna se poništava.",
                    seq, year));
            seq++;
        }

        // ── a handful of CONFIRMED → rejected / withdrawn ───────────────────
        Collections.shuffle(confirmed, SeedRandom.RNG);
        int rejected = 0, withdrawn = 0;
        for (ViolationEntity v : confirmed) {
            if (rejected >= 3 && withdrawn >= 2) break;
            if (rejected < 3) {
                created.add(save(v, AppealStatus.REJECTED, SeedRandom.pick(officers),
                        "Dokazi ne opravdavaju poništenje kazne — žalba se odbija.", seq, year));
                seq++;
                rejected++;
            } else if (withdrawn < 2) {
                created.add(save(v, AppealStatus.WITHDRAWN, null, null, seq, year));
                seq++;
                withdrawn++;
            }
        }

        return created;
    }

    private ViolationAppealEntity save(ViolationEntity violation, AppealStatus status, UserEntity reviewedBy,
                                       String reviewNotes, int seq, int year) {
        LocalDateTime submittedAt = violation.getOccurredAt()
                .plusDays(SeedRandom.intBetween(1, 25)); // within the 30-day appeal window

        ViolationAppealEntity appeal = new ViolationAppealEntity();
        appeal.setAppealNumber(String.format("APP-%d-%06d", year, seq));
        appeal.setReason(buildReason());
        appeal.setEvidenceUrl(SeedRandom.chance(0.4)
                ? "https://cdn.traffic-academy.local/appeals/evidence-" + seq + ".jpg" : null);
        appeal.setStatus(status);
        appeal.setSubmittedAt(submittedAt);
        appeal.setViolation(violation);
        appeal.setDriver(violation.getDriver() != null ? violation.getDriver() : violation.getVehicle().getOwner());
        appeal.setFineId(violation.getFineId());

        if (status == AppealStatus.APPROVED || status == AppealStatus.REJECTED) {
            appeal.setReviewedAt(submittedAt.plusDays(SeedRandom.intBetween(2, 14)));
            appeal.setReviewedBy(reviewedBy);
            appeal.setReviewNotes(reviewNotes);
        }

        return appealRepository.save(appeal);
    }

    private String buildReason() {
        String[] reasons = {
                "Saobraćajni znak je bio zaklonjen granama drveća u trenutku navodnog prekršaja.",
                "Vozilo je u tom trenutku bilo prodano, dostavljam ugovor o kupoprodaji kao dokaz.",
                "Radar nije bio kalibrisan prema posljednjem izvještaju o održavanju kamere.",
                "Nisam ja upravljao vozilom u navedeno vrijeme, priložen je iskaz svjedoka.",
                "Signalizacija ograničenja brzine na ovoj dionici nije bila vidljiva zbog radova."
        };
        return SeedRandom.pick(reasons);
    }
}