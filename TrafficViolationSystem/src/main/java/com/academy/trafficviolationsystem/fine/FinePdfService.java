package com.academy.trafficviolationsystem.fine;

import com.academy.trafficviolationsystem.core.exceptions.NotFoundException;
import com.academy.trafficviolationsystem.core.exceptions.infrastructure.PdfGenerationException;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Generates official fine PDF documents using iText 7.
 *
 * generateFinePdf() is annotated @Async("pdfExecutor") so it runs on the
 * dedicated PDF thread pool (defined in AsyncConfig) without blocking the
 * HTTP request thread that triggered fine issuance.
 *
 * Flow:
 *  1. FineService.afterInsert() calls generateFinePdf(fine).
 *  2. This method builds the PDF, writes it to the output directory, and
 *     calls FineRepository.setPdfPath() to store the path on the entity.
 *  3. The next GET /api/fines/{id} call returns pdfReady = true.
 *  4. GET /api/fines/{id}/pdf streams the file back to the client.
 *
 * Output directory is configured via:
 *   app.pdf.output-dir=./pdf-output
 *
 * Required Maven dependency:
 *   <dependency>
 *     <groupId>com.itextpdf</groupId>
 *     <artifactId>itext7-core</artifactId>
 *     <version>7.2.5</version>
 *     <type>pom</type>
 *   </dependency>
 */
@Service
public class FinePdfService {

    private static final Logger log = LoggerFactory.getLogger(FinePdfService.class);

    private static final DateTimeFormatter DATE_FMT     = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DeviceRgb HEADER_COLOR         = new DeviceRgb(27, 79, 138);  // #1B4F8A
    private static final DeviceRgb ROW_ALT_COLOR        = new DeviceRgb(245, 247, 250);

    private final FineRepository fineRepository;

    @Value("${app.pdf.output-dir:./pdf-output}")
    private String pdfOutputDir;

    public FinePdfService(FineRepository fineRepository) {
        this.fineRepository = fineRepository;
    }

    /**
     * Generates the official fine PDF and persists its path.
     * Runs on the pdfExecutor thread pool — never blocks the request thread.
     *
     * Takes a fineId rather than a FineEntity on purpose: the entity handed
     * in by the caller belongs to the caller's persistence context/session,
     * which does not survive the hop onto the async executor's thread.
     * Accessing a lazy association (fine.getDriver()) on that thread would
     * throw LazyInitializationException. Re-fetching here opens a fresh
     * transaction/session on the executor thread instead.
     */
    @Async("pdfExecutor")
    @Transactional
    public void generateFinePdf(UUID fineId) {
        FineEntity fine = fineRepository.findById(fineId)
                .orElseThrow(() -> new NotFoundException("Fine " + fineId + " not found"));

        try {
            Path outputDir = Paths.get(pdfOutputDir, "fines");
            Files.createDirectories(outputDir);

            String fileName  = "fine-" + fine.getFineNumber() + ".pdf";
            Path   filePath  = outputDir.resolve(fileName);

            buildPdf(fine, filePath.toString());

            fineRepository.setPdfPath(fine.getId(), filePath.toString());
            log.info("Fine PDF generated: {}", filePath);

        } catch (IOException e) {
            log.error("Failed to generate fine PDF for {}: {}", fine.getFineNumber(), e.getMessage());
            throw new PdfGenerationException("Failed to generate fine PDF: " + e.getMessage());
        }
    }

    // ── private PDF construction ──────────────────────────────────────────

    private void buildPdf(FineEntity fine, String destination) throws IOException {
        try (PdfWriter writer = new PdfWriter(destination);
             PdfDocument pdf  = new PdfDocument(writer);
             Document doc     = new Document(pdf)) {

            doc.setMargins(50, 50, 50, 50);

            addHeader(doc, fine);
            addSpacer(doc);
            addFineDetailsTable(doc, fine);
            addSpacer(doc);
            addDriverSection(doc, fine);
            addSpacer(doc);
            addPaymentSection(doc, fine);
            addFooter(doc, fine);
        }
    }

    private void addHeader(Document doc, FineEntity fine) {
        doc.add(new Paragraph("TRAFFIC VIOLATION SYSTEM")
                .setFontSize(20)
                .setBold()
                .setFontColor(HEADER_COLOR)
                .setTextAlignment(TextAlignment.CENTER));

        doc.add(new Paragraph("OFFICIAL TRAFFIC FINE NOTICE")
                .setFontSize(14)
                .setBold()
                .setFontColor(HEADER_COLOR)
                .setTextAlignment(TextAlignment.CENTER));

        doc.add(new Paragraph("Fine Number: " + fine.getFineNumber())
                .setFontSize(11)
                .setTextAlignment(TextAlignment.CENTER));
    }

    private void addFineDetailsTable(Document doc, FineEntity fine) {
        doc.add(new Paragraph("Fine Details").setFontSize(12).setBold().setFontColor(HEADER_COLOR));

        Table table = new Table(UnitValue.createPercentArray(new float[]{40, 60}))
                .setWidth(UnitValue.createPercentValue(100));

        addRow(table, "Fine Number",    fine.getFineNumber(), false);
        addRow(table, "Issued At",      fine.getIssuedAt() != null
                ? fine.getIssuedAt().format(DATETIME_FMT) : "-", true);
        addRow(table, "Due Date",       fine.getDueDate() != null
                ? fine.getDueDate().format(DATE_FMT) : "-", false);
        addRow(table, "Status",         fine.getStatus().name(), true);
        addRow(table, "Base Amount",    fine.getCurrency() + " " + fine.getAmount(), false);
        addRow(table, "Surcharge",      fine.getCurrency() + " " + fine.getSurchargeAmount(), true);
        addRow(table, "Discount",       fine.getCurrency() + " " + fine.getDiscountAmount(), false);
        addRow(table, "TOTAL DUE",      fine.getCurrency() + " " + fine.getTotalDue(), true);

        doc.add(table);
    }

    private void addDriverSection(Document doc, FineEntity fine) {
        doc.add(new Paragraph("Driver Information").setFontSize(12).setBold().setFontColor(HEADER_COLOR));

        Table table = new Table(UnitValue.createPercentArray(new float[]{40, 60}))
                .setWidth(UnitValue.createPercentValue(100));

        if (fine.getDriver() != null) {
            addRow(table, "Full Name",
                    fine.getDriver().getFirstName() + " " + fine.getDriver().getLastName(), false);
            addRow(table, "License Number", fine.getDriver().getLicenseNumber(), true);
            addRow(table, "National ID",    fine.getDriver().getNationalId(), false);
        }

        doc.add(table);
    }

    private void addPaymentSection(Document doc, FineEntity fine) {
        doc.add(new Paragraph("Payment Instructions").setFontSize(12).setBold().setFontColor(HEADER_COLOR));
        doc.add(new Paragraph(
                "Please pay the total amount of " + fine.getCurrency() + " " + fine.getTotalDue() +
                " by " + (fine.getDueDate() != null ? fine.getDueDate().format(DATE_FMT) : "N/A") + ".\n" +
                "Payment can be made online at the Traffic Violation Portal or at any authorised payment centre.\n" +
                "Reference your fine number " + fine.getFineNumber() + " in all correspondence.")
                .setFontSize(10));

        if (fine.getEarlyPayDiscountPct() != null
                && fine.getEarlyPayDiscountPct().compareTo(java.math.BigDecimal.ZERO) > 0) {
            doc.add(new Paragraph(
                    "Early Payment Discount: Pay within " + fine.getEarlyPayWindowDays() +
                    " days of issuance to receive a " +
                    fine.getEarlyPayDiscountPct().multiply(new java.math.BigDecimal("100")).stripTrailingZeros().toPlainString() +
                    "% discount.")
                    .setFontSize(10)
                    .setBold()
                    .setFontColor(new DeviceRgb(46, 125, 50)));
        }
    }

    private void addFooter(Document doc, FineEntity fine) {
        doc.add(new Paragraph("\nThis is an official document issued by the Traffic Violation System. " +
                "If you wish to appeal this fine, please submit your appeal within 30 days of issuance.")
                .setFontSize(8)
                .setFontColor(ColorConstants.GRAY)
                .setTextAlignment(TextAlignment.CENTER));
    }

    private void addRow(Table table, String label, String value, boolean shaded) {
        DeviceRgb bg = shaded ? ROW_ALT_COLOR : null;

        Cell labelCell = new Cell().add(new Paragraph(label).setBold().setFontSize(10));
        Cell valueCell = new Cell().add(new Paragraph(value).setFontSize(10));

        if (bg != null) {
            labelCell.setBackgroundColor(bg);
            valueCell.setBackgroundColor(bg);
        }
        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addSpacer(Document doc) {
        doc.add(new Paragraph(" ").setFontSize(6));
    }
}
