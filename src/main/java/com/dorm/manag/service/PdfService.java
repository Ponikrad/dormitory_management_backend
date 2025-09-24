package com.dorm.manag.service;

import com.dorm.manag.entity.Issue;
import com.dorm.manag.entity.Payment;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class PdfService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] generatePaymentReceipt(Payment payment) {
        log.info("Generating PDF receipt for payment: {}", payment.getId());

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);

            // Create document with A4 and leave top margin for the header we will draw
            // manually
            Document document = new Document(pdf, PageSize.A4);
            float topMargin = 80f; // leave space for manual header
            document.setMargins(topMargin, 36f, 36f, 36f);

            // prepare fonts
            PdfFont headerFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont normalFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            // Draw header on first page (centered)
            drawCenteredTextOnPage(pdf, 1, headerFont, 20f, "DORMITORY PAYMENT RECEIPT",
                    pdf.getDefaultPageSize().getTop() - 30f);

            // Small gap
            document.add(new Paragraph("\n"));

            // Payment details table (simple 2-column table)
            Table table = new Table(2);
            table.setWidth(PageSize.A4.getWidth() - 72f); // page width minus left+right margins (36+36)

            addTableRow(table, "Receipt ID:", "RCP-" + payment.getId());
            addTableRow(table, "Transaction ID:", payment.getTransactionId());
            addTableRow(table, "Payment Date:",
                    payment.getCompletedAt() != null ? payment.getCompletedAt().format(DATE_FORMATTER)
                            : payment.getCreatedAt().format(DATE_FORMATTER));
            addTableRow(table, "Student Name:",
                    payment.getUser().getFirstName() + " " + payment.getUser().getLastName());
            addTableRow(table, "Student Email:", payment.getUser().getEmail());
            addTableRow(table, "Room Number:", payment.getRoomNumber() != null ? payment.getRoomNumber() : "N/A");
            addTableRow(table, "Description:", payment.getDescription());
            addTableRow(table, "Payment Type:",
                    payment.getPaymentType() != null ? payment.getPaymentType() : "General");
            addTableRow(table, "Payment Method:", payment.getPaymentMethod().getDisplayName());
            addTableRow(table, "Amount:", payment.getDisplayAmount());
            addTableRow(table, "Status:", payment.getStatus().getDisplayName());

            if (payment.getExternalPaymentId() != null) {
                addTableRow(table, "External Payment ID:", payment.getExternalPaymentId());
            }

            if (payment.getPeriodStart() != null && payment.getPeriodEnd() != null) {
                addTableRow(table, "Period:",
                        payment.getPeriodStart().format(DATE_FORMATTER) + " - " +
                                payment.getPeriodEnd().format(DATE_FORMATTER));
            }

            document.add(table);

            // After adding content, draw footer on the last page (centered)
            String footerLine1 = "This is an automatically generated receipt.";
            String footerLine2 = "Generated on: " + java.time.LocalDateTime.now().format(DATE_FORMATTER);

            // choose a small y offset from bottom
            PdfPage lastPage = pdf.getLastPage();
            float footerY1 = lastPage.getPageSize().getBottom() + 30f;
            float footerY2 = lastPage.getPageSize().getBottom() + 18f;

            drawCenteredTextOnPage(pdf, pdf.getNumberOfPages(), normalFont, 10f, footerLine1, footerY1);
            drawCenteredTextOnPage(pdf, pdf.getNumberOfPages(), normalFont, 8f, footerLine2, footerY2);

            document.close();

            log.info("PDF receipt generated successfully for payment: {}", payment.getId());
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generating PDF receipt for payment {}: {}", payment.getId(), e.getMessage());
            throw new RuntimeException("Failed to generate PDF receipt", e);
        }
    }

    public byte[] generateIssueReport(Issue issue) {
        log.info("Generating PDF report for issue: {}", issue.getId());

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);

            Document document = new Document(pdf, PageSize.A4);
            float topMargin = 80f;
            document.setMargins(topMargin, 36f, 36f, 36f);

            PdfFont headerFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont normalFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            drawCenteredTextOnPage(pdf, 1, headerFont, 20f, "DORMITORY ISSUE REPORT",
                    pdf.getDefaultPageSize().getTop() - 30f);

            document.add(new Paragraph("\n"));

            Table table = new Table(2);
            table.setWidth(PageSize.A4.getWidth() - 72f);

            addTableRow(table, "Issue ID:", "ISS-" + issue.getId());
            addTableRow(table, "Title:", issue.getTitle());
            addTableRow(table, "Category:", issue.getCategoryDisplay());
            addTableRow(table, "Priority:", issue.getPriorityDisplay());
            addTableRow(table, "Status:", issue.getStatusDisplay());
            addTableRow(table, "Reported By:", issue.getUser().getFirstName() + " " + issue.getUser().getLastName());
            addTableRow(table, "Room Number:", issue.getRoomNumber() != null ? issue.getRoomNumber() : "N/A");
            addTableRow(table, "Location Details:",
                    issue.getLocationDetails() != null ? issue.getLocationDetails() : "N/A");
            addTableRow(table, "Reported Date:", issue.getReportedAt().format(DATE_FORMATTER));

            if (issue.getAcknowledgedAt() != null) {
                addTableRow(table, "Acknowledged Date:", issue.getAcknowledgedAt().format(DATE_FORMATTER));
            }

            if (issue.getResolvedAt() != null) {
                addTableRow(table, "Resolved Date:", issue.getResolvedAt().format(DATE_FORMATTER));
            }

            if (issue.getAssignedTo() != null) {
                addTableRow(table, "Assigned To:",
                        issue.getAssignedTo().getFirstName() + " " + issue.getAssignedTo().getLastName());
            }

            if (issue.getContractorRequired() != null && issue.getContractorRequired()) {
                addTableRow(table, "Contractor Required:", "Yes");
            }

            if (issue.getCostEstimate() != null) {
                addTableRow(table, "Cost Estimate:", issue.getCostEstimate());
            }

            document.add(table);

            // Description & notes
            document.add(new Paragraph("\nDescription:").setBold());
            document.add(
                    new Paragraph(issue.getDescription() != null ? issue.getDescription() : "No description provided"));

            if (issue.getAdminNotes() != null && !issue.getAdminNotes().trim().isEmpty()) {
                document.add(new Paragraph("\nAdmin Notes:").setBold());
                document.add(new Paragraph(issue.getAdminNotes()));
            }

            if (issue.getResolutionNotes() != null && !issue.getResolutionNotes().trim().isEmpty()) {
                document.add(new Paragraph("\nResolution Notes:").setBold());
                document.add(new Paragraph(issue.getResolutionNotes()));
            }

            // Footer on last page
            String footerLine1 = "This is an automatically generated report.";
            String footerLine2 = "Generated on: " + java.time.LocalDateTime.now().format(DATE_FORMATTER);

            PdfPage lastPage = pdf.getLastPage();
            float footerY1 = lastPage.getPageSize().getBottom() + 30f;
            float footerY2 = lastPage.getPageSize().getBottom() + 18f;

            drawCenteredTextOnPage(pdf, pdf.getNumberOfPages(), normalFont, 10f, footerLine1, footerY1);
            drawCenteredTextOnPage(pdf, pdf.getNumberOfPages(), normalFont, 8f, footerLine2, footerY2);

            document.close();

            log.info("PDF report generated successfully for issue: {}", issue.getId());
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generating PDF report for issue {}: {}", issue.getId(), e.getMessage());
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    private void addTableRow(Table table, String key, String value) {
        table.addCell(new Paragraph(key).setBold());
        table.addCell(new Paragraph(value != null ? value : "N/A"));
    }

    /**
     * Draw text centered on a specific page (by page number). Uses kernel PdfCanvas
     * so we avoid layout property enums entirely.
     */
    private void drawCenteredTextOnPage(PdfDocument pdf, int pageNumber, PdfFont font, float fontSize,
            String text, float y) {
        PdfPage page = pdf.getPage(pageNumber);
        PdfCanvas canvas = new PdfCanvas(page);
        float pageWidth = page.getPageSize().getWidth();
        float textWidth = font.getWidth(text, fontSize);
        float x = (pageWidth - textWidth) / 2f;

        canvas.beginText()
                .setFontAndSize(font, fontSize)
                .moveText(x, y)
                .showText(text)
                .endText();
    }

    public void savePdfToFile(byte[] pdfBytes, String filename) {
        try {
            java.nio.file.Files.write(java.nio.file.Paths.get(filename), pdfBytes);
            log.info("PDF saved to file: {}", filename);
        } catch (Exception e) {
            log.error("Failed to save PDF to file {}: {}", filename, e.getMessage());
            throw new RuntimeException("Failed to save PDF to file", e);
        }
    }
}
