package Services;

import Models.Course;
import Models.User;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell;
import Services.CertificateService;

import java.awt.Color;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class PDFService {
    private final CertificateService certificateService = new CertificateService();

    public String generateCertificate(User user, Course course) {
        String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "Certificat_" + user.getUsername() + "_" + course.getId() + "_" + timestamp + ".pdf";
        String filePath = "uploads/certificates/" + fileName;

        // Ensure directory exists
        java.io.File directory = new java.io.File("uploads/certificates/");
        if (!directory.exists()) {
            directory.mkdirs();
        }

        Document document = new Document(PageSize.A4.rotate());
        try {
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(filePath));
            document.open();

            PdfContentByte canvas = writer.getDirectContent();
            Rectangle pageSize = document.getPageSize();

            // 1. Background Color (Very light cream)
            canvas.saveState();
            canvas.setColorFill(new Color(255, 254, 249));
            canvas.rectangle(0, 0, pageSize.getWidth(), pageSize.getHeight());
            canvas.fill();
            canvas.restoreState();

            // 2. Decorative Borders
            canvas.saveState();
            canvas.setLineWidth(5);
            canvas.setColorStroke(new Color(184, 134, 11)); // Dark Gold
            canvas.rectangle(20, 20, pageSize.getWidth() - 40, pageSize.getHeight() - 40);
            canvas.stroke();
            
            canvas.setLineWidth(1);
            canvas.rectangle(30, 30, pageSize.getWidth() - 60, pageSize.getHeight() - 60);
            canvas.stroke();
            canvas.restoreState();

            // 3. Content - Using a Table for better positioning control
            PdfPTable mainTable = new PdfPTable(1);
            mainTable.setWidthPercentage(100);
            
            // Spacing
            addEmptyLine(mainTable, 2);

            // "Badge" replacement (No emoji to avoid font issues)
            Font badgeFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, new Color(184, 134, 11));
            addCell(mainTable, "CERTIFICATE", badgeFont);
            
            addEmptyLine(mainTable, 1);

            // Title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 42, new Color(30, 41, 59));
            addCell(mainTable, "DE RÉUSSITE", titleFont);

            addEmptyLine(mainTable, 1);

            // Subtitle
            Font subTitleFont = FontFactory.getFont(FontFactory.HELVETICA, 18, new Color(100, 116, 139));
            addCell(mainTable, "Ce document officiel atteste que", subTitleFont);

            addEmptyLine(mainTable, 1);

            // User Name
            Font nameFont = FontFactory.getFont(FontFactory.TIMES_BOLDITALIC, 50, new Color(139, 92, 246));
            addCell(mainTable, user.getUsername().toUpperCase(), nameFont);

            addEmptyLine(mainTable, 1);

            // Completion text
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 16, new Color(71, 85, 105));
            addCell(mainTable, "a complété avec succès et excellence le cursus de formation :", bodyFont);

            addEmptyLine(mainTable, 1);

            // Course Title
            Font courseFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 28, new Color(15, 23, 42));
            addCell(mainTable, "\"" + course.getTitle() + "\"", courseFont);

            addEmptyLine(mainTable, 4);

            document.add(mainTable);

            // 4. Footer Section
            PdfPTable footerTable = new PdfPTable(3);
            footerTable.setWidthPercentage(100);
            
            String dateStr = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
            String certId = certificateService.getCertificateUID(user.getId(), course.getId());
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 12, new Color(100, 116, 139));

            // Date
            PdfPCell cell1 = new PdfPCell(new Paragraph("Délivré le :\n" + dateStr, smallFont));
            cell1.setBorder(Rectangle.NO_BORDER);
            cell1.setHorizontalAlignment(Element.ALIGN_CENTER);
            footerTable.addCell(cell1);

            // ID
            PdfPCell cell2 = new PdfPCell(new Paragraph("ID Certificat :\n" + certId, smallFont));
            cell2.setBorder(Rectangle.NO_BORDER);
            cell2.setHorizontalAlignment(Element.ALIGN_CENTER);
            footerTable.addCell(cell2);

            // Signature
            Font sigFont = FontFactory.getFont(FontFactory.TIMES_BOLDITALIC, 14, new Color(15, 23, 42));
            Paragraph sigPara = new Paragraph("Signature de la Direction\n", smallFont);
            sigPara.add(new Paragraph("SkillPath Academy", sigFont));
            PdfPCell cell3 = new PdfPCell(sigPara);
            cell3.setBorder(Rectangle.NO_BORDER);
            cell3.setHorizontalAlignment(Element.ALIGN_CENTER);
            footerTable.addCell(cell3);

            document.add(footerTable);

            document.close();
            return filePath;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void addCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Paragraph(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(10);
        table.addCell(cell);
    }

    private void addEmptyLine(PdfPTable table, int number) {
        for (int i = 0; i < number; i++) {
            PdfPCell cell = new PdfPCell(new Paragraph(" "));
            cell.setBorder(Rectangle.NO_BORDER);
            table.addCell(cell);
        }
    }
}
