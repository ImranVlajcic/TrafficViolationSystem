package com.academy.trafficviolationsystem.analytics;

import com.academy.trafficviolationsystem.appeal.AppealDto;
import com.academy.trafficviolationsystem.driver.DriverEntity;
import com.academy.trafficviolationsystem.driver.DriverPointHistoryDto;
import com.academy.trafficviolationsystem.driver.LicenseSuspensionDto;
import com.academy.trafficviolationsystem.fine.FineDto;
import com.academy.trafficviolationsystem.violation.ViolationDto;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Format-specific export logic. Implementations are picked up by Spring and
 * indexed by {@link #getFormat()} in {@link ReportGenerationService}.
 */
public interface ReportExportStrategy {

    ReportFormat getFormat();

    void writeTabularReport(Path path, String title, List<String> headers, List<List<String>> rows) throws IOException;

    void writeDriverHistoryReport(Path path, DriverEntity driver,
                                  List<ViolationDto> violations, List<FineDto> fines,
                                  List<AppealDto> appeals, List<LicenseSuspensionDto> suspensions,
                                  List<DriverPointHistoryDto> pointHistory) throws IOException;
}