package com.academy.trafficviolationsystem.payment;

import com.academy.trafficviolationsystem.core.exceptions.AppException;
import com.academy.trafficviolationsystem.core.exceptions.ErrorCode;
import com.academy.trafficviolationsystem.fine.FineEntity;
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
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

/**
 * Generates payment receipt PDFs using iText 7.
 *
 * Runs on the dedicated pdfExecutor thread pool (@Async("pdfExecutor"))
 * so it never blocks the HTTP thread that confirmed the payment.
 *
 * Flow (called by PaymentService.afterSuccessfulPayment):
 *  1. Build receipt PDF with: transaction ID, amount, fine number, driver
 *     details, payment method, timestamp, and a green "PAYMENT CONFIRMED" stamp.
 *  2. Write to {app.pdf.output-dir}/receipts/receipt-{transactionId}.pdf
 *  3. Call PaymentRepository.setReceiptPdfPath() to persist the path.
 *  4. Client polls GET /api/payments/{id} until receiptReady = true, then
 *     calls GET /api/payments/{id}/receipt to download.
 */
@Service
public class PaymentConfirmationPdfService {

    private static final Logger log = LoggerFactory.getLogger(PaymentConfirmationPdfService.class);

    private static final DateTimeFormatter DATE_FMT     = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    private static final DeviceRgb HEADER_COLOR   = new DeviceRgb(27, 79, 138);   // #1B4F8A
    private static final DeviceRgb SUCCESS_COLOR  = new DeviceRgb(46, 125, 50);   // #2E7D32
    private static final DeviceRgb ROW_ALT_COLOR  = new DeviceRgb(245, 247, 250);

    private final PaymentRepository paymentRepository;

    @Value("${app.pdf.output-dir:./pdf-output}")
    private String pdfOutputDir;

    public PaymentConfirmationPdfService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    /**
     * Generates the receipt PDF for a successful payment.
     * Runs asynchronously on pdfExecutor — does not block the caller.
     *
     * @param payment The successfully processed PaymentEntity.
     * @param fine    The FineEntity that was paid (pre-loaded by PaymentService).
     */
    @Async("pdfExecutor")
    @Transactional
    public void generateReceipt(PaymentEntity payment, FineEntity fine) {
        try {
            Path outputDir = Paths.get(pdfOutputDir, "receipts");
            Files.createDirectories(outputDir);

            String fileName = "receipt-" + payment.getTransactionId() + ".pdf";
            Path   filePath = outputDir.resolve(fileName);

            buildPdf(payment, fine, filePath.toString());

            paymentRepository.setReceiptPdfPath(payment.getId(), filePath.toString());
            log.info("Receipt PDF generated: {}", filePath);

        } catch (IOException e) {
            log.error("Failed to generate receipt PDF for transaction {}: {}",
                payment.getTransactionId(), e.getMessage());
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR,
                    ErrorCode.PDF_GENERATION_ERROR,
                    "Failed to generate receipt PDF: " + e.getMessage());
        }
    }

    // ── private PDF construction ──────────────────────────────────────────

    private void buildPdf(PaymentEntity payment, FineEntity fine, String destination)
            throws IOException {
        try (PdfWriter writer = new PdfWriter(destination);
             PdfDocument pdf  = new PdfDocument(writer);
             Document doc     = new Document(pdf)) {

            doc.setMargins(50, 50, 50, 50);

            addHeader(doc);
            addConfirmedStamp(doc);
            addSpacer(doc);
            addTransactionTable(doc, payment, fine);
            addSpacer(doc);
            addDriverSection(doc, fine);
            addSpacer(doc);
            addFooter(doc, payment);
        }
    }

    private void addHeader(Document doc) {
        doc.add(new Paragraph("TRAFFIC VIOLATION SYSTEM")
                .setFontSize(20).setBold()
                .setFontColor(HEADER_COLOR)
                .setTextAlignment(TextAlignment.CENTER));

        doc.add(new Paragraph("PAYMENT RECEIPT")
                .setFontSize(14).setBold()
                .setFontColor(HEADER_COLOR)
                .setTextAlignment(TextAlignment.CENTER));
    }

    private void addConfirmedStamp(Document doc) {
        doc.add(new Paragraph("✓  PAYMENT CONFIRMED")
                .setFontSize(18).setBold()
                .setFontColor(SUCCESS_COLOR)
                .setTextAlignment(TextAlignment.CENTER));
    }

    private void addTransactionTable(Document doc, PaymentEntity payment, FineEntity fine) {
        doc.add(new Paragraph("Transaction Details")
                .setFontSize(12).setBold().setFontColor(HEADER_COLOR));

        Table table = new Table(UnitValue.createPercentArray(new float[]{40, 60}))
                .setWidth(UnitValue.createPercentValue(100));

        addRow(table, "Transaction ID",  payment.getTransactionId(), false);
        addRow(table, "Fine Number",     fine.getFineNumber(), true);
        addRow(table, "Amount Paid",     payment.getCurrency() + " " + payment.getAmount(), false);
        addRow(table, "Payment Method",  payment.getMethod().name().replace("_", " "), true);
        addRow(table, "Payment Date",    payment.getPaidAt() != null
                ? payment.getPaidAt().format(DATETIME_FMT) : "-", false);
        addRow(table, "Status",          "PAID", true);

        doc.add(table);
    }

    private void addDriverSection(Document doc, FineEntity fine) {
        doc.add(new Paragraph("Driver Information")
                .setFontSize(12).setBold().setFontColor(HEADER_COLOR));

        Table table = new Table(UnitValue.createPercentArray(new float[]{40, 60}))
                .setWidth(UnitValue.createPercentValue(100));

        if (fine.getDriver() != null) {
            addRow(table, "Full Name",
                    fine.getDriver().getFirstName() + " " + fine.getDriver().getLastName(), false);
            addRow(table, "License Number", fine.getDriver().getLicenseNumber(), true);
        }

        doc.add(table);
    }

    private void addFooter(Document doc, PaymentEntity payment) {
        doc.add(new Paragraph(
                "\nThis receipt confirms that payment has been successfully processed. " +
                "Please retain this document for your records. " +
                "Transaction reference: " + payment.getTransactionId())
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
