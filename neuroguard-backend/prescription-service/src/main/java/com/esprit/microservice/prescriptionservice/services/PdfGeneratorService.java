package com.esprit.microservice.prescriptionservice.services;

import com.esprit.microservice.prescriptionservice.entities.Prescription;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PdfGeneratorService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Génère un PDF pour une prescription
     */
    public byte[] generatePrescriptionPdf(Prescription prescription) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        // En-tête
        addHeader(document, "ORDONNANCE MÉDICALE - NEUROGUARD");

        // Informations générales
        addPrescriptionInfo(document, prescription);

        // Contenu de la prescription
        addPrescriptionContent(document, prescription);

        // Pied de page
        addFooter(document);

        document.close();
        return out.toByteArray();
    }

    /**
     * Génère un PDF combiné (Plan de soins + Prescription)
     * Fusionne deux documents PDF
     */
    public byte[] generateCombinedPdf(byte[] carePlanPdf, Prescription prescription) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        // Créer un nouveau document qui contiendra les deux PDFs
        PdfWriter writer = new PdfWriter(out);
        PdfDocument targetDoc = new PdfDocument(writer);
        
        try {
            // Ajouter le PDF du plan de soins
            if (carePlanPdf != null && carePlanPdf.length > 0) {
                PdfDocument sourcePlanDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(carePlanPdf)));
                for (int i = 1; i <= sourcePlanDoc.getNumberOfPages(); i++) {
                    PdfPage page = sourcePlanDoc.getPage(i);
                    targetDoc.addPage(page.copyTo(targetDoc));
                }
                sourcePlanDoc.close();
            }
            
            // Ajouter une page de séparation
            Document doc = new Document(targetDoc);
            doc.add(new AreaBreak());
            
            // Ajouter le PDF de la prescription
            PdfWriter prescriptionWriter = new PdfWriter(new ByteArrayOutputStream());
            PdfDocument prescriptionDoc = new PdfDocument(prescriptionWriter);
            Document prescriptionDocument = new Document(prescriptionDoc);
            
            addHeader(prescriptionDocument, "ORDONNANCE MÉDICALE - NEUROGUARD");
            addPrescriptionInfo(prescriptionDocument, prescription);
            addPrescriptionContent(prescriptionDocument, prescription);
            addFooter(prescriptionDocument);
            
            prescriptionDocument.close();
            
            // Récupérer le contenu généré
            ByteArrayOutputStream prescriptionOut = (ByteArrayOutputStream) prescriptionWriter.getOutputStream();
            byte[] prescriptionBytes = prescriptionOut.toByteArray();
            
            // Fusionner le PDF de prescription au document cible
            if (prescriptionBytes.length > 0) {
                PdfDocument sourcePrescriptionDoc = new PdfDocument(new PdfReader(new ByteArrayInputStream(prescriptionBytes)));
                for (int i = 1; i <= sourcePrescriptionDoc.getNumberOfPages(); i++) {
                    PdfPage page = sourcePrescriptionDoc.getPage(i);
                    targetDoc.addPage(page.copyTo(targetDoc));
                }
                sourcePrescriptionDoc.close();
            }
            
            doc.close();
            
        } finally {
            targetDoc.close();
        }
        
        return out.toByteArray();
    }

    private void addHeader(Document document, String title) {
        Paragraph header = new Paragraph(title)
                .setFontSize(20)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(header);

        // Ligne de séparation
        Paragraph separator = new Paragraph("_____________________")
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(separator);
    }

    private void addPrescriptionInfo(Document document, Prescription prescription) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{2, 3}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20);

        table.addCell(new Cell().add(new Paragraph("Patient ID:").setBold()));
        table.addCell(new Cell().add(new Paragraph(String.valueOf(prescription.getPatientId()))));

        table.addCell(new Cell().add(new Paragraph("Provider ID:").setBold()));
        table.addCell(new Cell().add(new Paragraph(String.valueOf(prescription.getProviderId()))));

        if (prescription.getCreatedAt() != null) {
            table.addCell(new Cell().add(new Paragraph("Date de création:").setBold()));
            table.addCell(new Cell().add(new Paragraph(prescription.getCreatedAt().format(DATE_FORMATTER))));
        }

        if (prescription.getUpdatedAt() != null) {
            table.addCell(new Cell().add(new Paragraph("Dernière modification:").setBold()));
            table.addCell(new Cell().add(new Paragraph(prescription.getUpdatedAt().format(DATE_FORMATTER))));
        }

        if (prescription.getJour() != null && !prescription.getJour().trim().isEmpty()) {
            table.addCell(new Cell().add(new Paragraph("Jour:").setBold()));
            table.addCell(new Cell().add(new Paragraph(prescription.getJour())));
        }

        if (prescription.getDosage() != null && !prescription.getDosage().trim().isEmpty()) {
            table.addCell(new Cell().add(new Paragraph("Dosage:").setBold()));
            table.addCell(new Cell().add(new Paragraph(prescription.getDosage())));
        }

        document.add(table);
    }

    private void addPrescriptionContent(Document document, Prescription prescription) {
        if (prescription.getContenu() != null && !prescription.getContenu().trim().isEmpty()) {
            addSectionTitle(document, "Contenu de l'Ordonnance");
            Paragraph content = new Paragraph(prescription.getContenu())
                    .setMarginBottom(15);
            document.add(content);
        }

        if (prescription.getNotes() != null && !prescription.getNotes().trim().isEmpty()) {
            addSectionTitle(document, "Notes");
            Paragraph notes = new Paragraph(prescription.getNotes())
                    .setItalic()
                    .setMarginBottom(15);
            document.add(notes);
        }
    }

    private void addSectionTitle(Document document, String title) {
        Paragraph sectionTitle = new Paragraph(title)
                .setFontSize(16)
                .setTextAlignment(TextAlignment.LEFT)
                .setMarginTop(20)
                .setMarginBottom(10);
        document.add(sectionTitle);
    }

    private void addFooter(Document document) {
        document.add(new AreaBreak());

        Paragraph footer = new Paragraph("Document généré automatiquement par NeuroGuard")
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER)
                .setItalic()
                .setMarginTop(30);
        document.add(footer);

        Paragraph disclaimer = new Paragraph("Ce document est confidentiel et destiné à un usage médical uniquement.")
                .setFontSize(8)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10);
        document.add(disclaimer);
    }
}
