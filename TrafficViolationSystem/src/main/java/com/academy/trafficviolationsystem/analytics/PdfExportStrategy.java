package com.academy.trafficviolationsystem.analytics;

import com.academy.trafficviolationsystem.appeal.AppealDto;
import com.academy.trafficviolationsystem.core.exceptions.infrastructure.PdfGenerationException;
import com.academy.trafficviolationsystem.driver.DriverEntity;
import com.academy.trafficviolationsystem.driver.DriverPointHistoryDto;
import com.academy.trafficviolationsystem.driver.LicenseSuspensionDto;
import com.academy.trafficviolationsystem.fine.FineDto;
import com.academy.trafficviolationsystem.violation.ViolationDto;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Component
public class PdfExportStrategy implements ReportExportStrategy {

    private static final DeviceRgb HEADER_COLOR = new DeviceRgb(27, 79, 138);
    private static final DeviceRgb ALT_ROW_COLOR = new DeviceRgb(245, 247, 250);

    @Override
    public ReportFormat getFormat() {
        return ReportFormat.PDF;
    }

    @Override
    public void writeTabularReport(Path path, String title, List<String> headers, List<List<String>> rows) throws IOException {
        try (PdfWriter writer = new PdfWriter(path.toString());
             PdfDocument pdf  = new PdfDocument(writer);
             Document doc     = new Document(pdf)) {

            doc.setMargins(40, 40, 40, 40);
            doc.add(new Paragraph(title).setFontSize(16).setBold().setFontColor(HEADER_COLOR));
            doc.add(new Paragraph(" ").setFontSize(6));

            Table table = new Table(UnitValue.createPercentArray(headers.size())).useAllAvailableWidth();
            for (String h : headers) {
                table.addHeaderCell(new Cell()
                        .add(new Paragraph(h).setBold().setFontSize(10).setFontColor(ColorConstants.WHITE))
                        .setBackgroundColor(HEADER_COLOR));
            }
            boolean shade = false;
            for (List<String> row : rows) {
                for (String value : row) {
                    Cell cell = new Cell().add(new Paragraph(value == null ? "-" : value).setFontSize(9));
                    if (shade) cell.setBackgroundColor(ALT_ROW_COLOR);
                    table.addCell(cell);
                }
                shade = !shade;
            }
            doc.add(table);
        } catch (IOException e) {
            throw new PdfGenerationException(e.getMessage());
        }
    }

    @Override
    public void writeDriverHistoryReport(Path path, DriverEntity driver,
                                         List<ViolationDto> violations, List<FineDto> fines,
                                         List<AppealDto> appeals, List<LicenseSuspensionDto> suspensions,
                                         List<DriverPointHistoryDto> pointHistory) throws IOException {
        try (PdfWriter writer = new PdfWriter(path.toString());
             PdfDocument pdf  = new PdfDocument(writer);
             Document doc     = new Document(pdf)) {

            doc.setMargins(40, 40, 40, 40);
            doc.add(new Paragraph("Driver History Report").setFontSize(16).setBold().setFontColor(HEADER_COLOR));
            doc.add(new Paragraph(driver.getFirstName() + " " + driver.getLastName() +
                    " — License " + driver.getLicenseNumber() +
                    " — Current Points: " + driver.getPenaltyPoints()).setFontSize(10));
            doc.add(new Paragraph(" ").setFontSize(6));

            addSection(doc, "Violations", List.of("Reference", "Type", "Status", "Occurred At"),
                    violations.stream().map(v -> List.of(
                            v.getReferenceNumber(), String.valueOf(v.getViolationType()),
                            String.valueOf(v.getStatus()), String.valueOf(v.getOccurredAt()))).toList());

            addSection(doc, "Fines", List.of("Fine Number", "Status", "Total Due", "Issued At"),
                    fines.stream().map(f -> List.of(
                            f.getFineNumber(), String.valueOf(f.getStatus()),
                            String.valueOf(f.getTotalDue()), String.valueOf(f.getIssuedAt()))).toList());

            addSection(doc, "Appeals", List.of("Appeal Number", "Status", "Submitted At"),
                    appeals.stream().map(a -> List.of(
                            a.getAppealNumber(), String.valueOf(a.getStatus()),
                            String.valueOf(a.getSubmittedAt()))).toList());

            addSection(doc, "Suspensions", List.of("Reason", "Start Date", "End Date", "Active"),
                    suspensions.stream().map(s -> List.of(
                            s.getReason(), String.valueOf(s.getStartDate()),
                            String.valueOf(s.getEndDate()), String.valueOf(s.isActive()))).toList());

            addSection(doc, "Point History", List.of("Change", "Before", "After", "Reason", "At"),
                    pointHistory.stream().map(p -> List.of(
                            String.valueOf(p.getChangeAmount()), String.valueOf(p.getPointsBefore()),
                            String.valueOf(p.getPointsAfter()), p.getReason(),
                            String.valueOf(p.getOccurredAt()))).toList());
        }
    }

    private void addSection(Document doc, String title, List<String> headers, List<List<String>> rows) {
        doc.add(new Paragraph(title).setFontSize(12).setBold().setFontColor(HEADER_COLOR));
        if (rows.isEmpty()) {
            doc.add(new Paragraph("None").setFontSize(9));
            return;
        }
        Table table = new Table(UnitValue.createPercentArray(headers.size())).useAllAvailableWidth();
        for (String h : headers) {
            table.addHeaderCell(new Cell().add(new Paragraph(h).setBold().setFontSize(9)));
        }
        for (List<String> row : rows) {
            for (String v : row) {
                table.addCell(new Cell().add(new Paragraph(v == null ? "-" : v).setFontSize(8)));
            }
        }
        doc.add(table);
        doc.add(new Paragraph(" ").setFontSize(4));
    }
}