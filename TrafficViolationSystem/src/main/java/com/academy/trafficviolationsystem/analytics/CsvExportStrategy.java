package com.academy.trafficviolationsystem.analytics;

import com.academy.trafficviolationsystem.appeal.AppealDto;
import com.academy.trafficviolationsystem.driver.DriverEntity;
import com.academy.trafficviolationsystem.driver.DriverPointHistoryDto;
import com.academy.trafficviolationsystem.driver.LicenseSuspensionDto;
import com.academy.trafficviolationsystem.fine.FineDto;
import com.academy.trafficviolationsystem.violation.ViolationDto;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
public class CsvExportStrategy implements ReportExportStrategy {

    @Override
    public ReportFormat getFormat() {
        return ReportFormat.CSV;
    }

    @Override
    public void writeTabularReport(Path path, String title, List<String> headers, List<List<String>> rows) throws IOException {
        StringBuilder sb = new StringBuilder(String.join(",", headers)).append("\n");
        for (List<String> row : rows) {
            for (int i = 0; i < row.size(); i++) {
                sb.append(csvEscape(row.get(i)));
                sb.append(i < row.size() - 1 ? "," : "\n");
            }
        }
        Files.writeString(path, sb.toString());
    }

    @Override
    public void writeDriverHistoryReport(Path path, DriverEntity driver,
                                         List<ViolationDto> violations, List<FineDto> fines,
                                         List<AppealDto> appeals, List<LicenseSuspensionDto> suspensions,
                                         List<DriverPointHistoryDto> pointHistory) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("Driver History Report\n");
        sb.append("Name,").append(driver.getFirstName()).append(" ").append(driver.getLastName()).append("\n");
        sb.append("License Number,").append(driver.getLicenseNumber()).append("\n");
        sb.append("Current Penalty Points,").append(driver.getPenaltyPoints()).append("\n\n");

        sb.append("VIOLATIONS\nReference,Type,Status,Occurred At\n");
        for (ViolationDto v : violations) {
            sb.append(csvEscape(v.getReferenceNumber())).append(",")
                    .append(csvEscape(String.valueOf(v.getViolationType()))).append(",")
                    .append(csvEscape(String.valueOf(v.getStatus()))).append(",")
                    .append(csvEscape(String.valueOf(v.getOccurredAt()))).append("\n");
        }

        sb.append("\nFINES\nFine Number,Status,Total Due,Issued At\n");
        for (FineDto f : fines) {
            sb.append(csvEscape(f.getFineNumber())).append(",")
                    .append(csvEscape(String.valueOf(f.getStatus()))).append(",")
                    .append(csvEscape(String.valueOf(f.getTotalDue()))).append(",")
                    .append(csvEscape(String.valueOf(f.getIssuedAt()))).append("\n");
        }

        sb.append("\nAPPEALS\nAppeal Number,Status,Submitted At\n");
        for (AppealDto a : appeals) {
            sb.append(csvEscape(a.getAppealNumber())).append(",")
                    .append(csvEscape(String.valueOf(a.getStatus()))).append(",")
                    .append(csvEscape(String.valueOf(a.getSubmittedAt()))).append("\n");
        }

        sb.append("\nSUSPENSIONS\nReason,Start Date,End Date,Active\n");
        for (LicenseSuspensionDto s : suspensions) {
            sb.append(csvEscape(s.getReason())).append(",")
                    .append(csvEscape(String.valueOf(s.getStartDate()))).append(",")
                    .append(csvEscape(String.valueOf(s.getEndDate()))).append(",")
                    .append(csvEscape(String.valueOf(s.isActive()))).append("\n");
        }

        sb.append("\nPOINT HISTORY\nChange,Before,After,Reason,Occurred At\n");
        for (DriverPointHistoryDto p : pointHistory) {
            sb.append(csvEscape(String.valueOf(p.getChangeAmount()))).append(",")
                    .append(csvEscape(String.valueOf(p.getPointsBefore()))).append(",")
                    .append(csvEscape(String.valueOf(p.getPointsAfter()))).append(",")
                    .append(csvEscape(p.getReason())).append(",")
                    .append(csvEscape(String.valueOf(p.getOccurredAt()))).append("\n");
        }

        Files.writeString(path, sb.toString());
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        return (value.contains(",") || value.contains("\"") || value.contains("\n"))
                ? "\"" + value.replace("\"", "\"\"") + "\""
                : value;
    }
}