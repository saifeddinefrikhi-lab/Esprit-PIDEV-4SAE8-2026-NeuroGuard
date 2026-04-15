package com.esprit.microservice.careplanservice.services;

import com.esprit.microservice.careplanservice.entities.CarePlan;
import com.esprit.microservice.careplanservice.entities.Prescription;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PdfGeneratorService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Génère un PDF pour un plan de soins
     */
    public byte[] generateCarePlanPdf(CarePlan carePlan) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        // En-tête
        addHeader(document, "PLAN DE SOINS - NEUROGUARD");

        // Informations générales
        addCarePlanInfo(document, carePlan);

        // Sections du plan
        addCarePlanSections(document, carePlan);

        // Pied de page
        addFooter(document);

        document.close();
        return out.toByteArray();
    }

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
     */
    public byte[] generateCombinedPdf(CarePlan carePlan, Prescription prescription) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        // En-tête principal
        addHeader(document, "DOCUMENT MÉDICAL COMPLET - NEUROGUARD");

        // Section Plan de soins
        document.add(new AreaBreak());
        addSectionTitle(document, "PLAN DE SOINS");
        addCarePlanInfo(document, carePlan);
        addCarePlanSections(document, carePlan);

        // Section Prescription
        document.add(new AreaBreak());
        addSectionTitle(document, "ORDONNANCE MÉDICALE");
        addPrescriptionInfo(document, prescription);
        addPrescriptionContent(document, prescription);

        // Pied de page
        addFooter(document);

        document.close();
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

    private void addSectionTitle(Document document, String title) {
        Paragraph sectionTitle = new Paragraph(title)
                .setFontSize(16)
                .setTextAlignment(TextAlignment.LEFT)
                .setMarginTop(20)
                .setMarginBottom(10);
        document.add(sectionTitle);
    }

    private void addCarePlanInfo(Document document, CarePlan carePlan) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{2, 3}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginBottom(20);

        // Informations du patient
        table.addCell(new Cell().add(new Paragraph("Patient ID:").setBold()));
        table.addCell(new Cell().add(new Paragraph(String.valueOf(carePlan.getPatientId()))));

        table.addCell(new Cell().add(new Paragraph("Provider ID:").setBold()));
        table.addCell(new Cell().add(new Paragraph(String.valueOf(carePlan.getProviderId()))));

        table.addCell(new Cell().add(new Paragraph("Priorité:").setBold()));
        table.addCell(new Cell().add(new Paragraph(carePlan.getPriority() != null ? carePlan.getPriority().name() : "MEDIUM")));

        document.add(table);
    }

    private void addCarePlanSections(Document document, CarePlan carePlan) {
        // Plan nutritionnel
        if (carePlan.getNutritionPlan() != null && !carePlan.getNutritionPlan().trim().isEmpty()) {
            addSectionTitle(document, "Plan Nutritionnel");
            Paragraph nutrition = new Paragraph(carePlan.getNutritionPlan())
                    .setMarginBottom(10);
            document.add(nutrition);

            Paragraph nutritionStatus = new Paragraph("Statut: " +
                    (carePlan.getNutritionStatus() != null ? carePlan.getNutritionStatus().name() : "TODO"))
                    .setItalic()
                    .setMarginBottom(15);
            document.add(nutritionStatus);
        }

        // Plan de sommeil
        if (carePlan.getSleepPlan() != null && !carePlan.getSleepPlan().trim().isEmpty()) {
            addSectionTitle(document, "Plan de Sommeil");
            Paragraph sleep = new Paragraph(carePlan.getSleepPlan())
                    .setMarginBottom(10);
            document.add(sleep);

            Paragraph sleepStatus = new Paragraph("Statut: " +
                    (carePlan.getSleepStatus() != null ? carePlan.getSleepStatus().name() : "TODO"))
                    .setItalic()
                    .setMarginBottom(15);
            document.add(sleepStatus);
        }

        // Plan d'activité
        if (carePlan.getActivityPlan() != null && !carePlan.getActivityPlan().trim().isEmpty()) {
            addSectionTitle(document, "Plan d'Activité");
            Paragraph activity = new Paragraph(carePlan.getActivityPlan())
                    .setMarginBottom(10);
            document.add(activity);

            Paragraph activityStatus = new Paragraph("Statut: " +
                    (carePlan.getActivityStatus() != null ? carePlan.getActivityStatus().name() : "TODO"))
                    .setItalic()
                    .setMarginBottom(15);
            document.add(activityStatus);
        }

        // Plan médicamenteux
        if (carePlan.getMedicationPlan() != null && !carePlan.getMedicationPlan().trim().isEmpty()) {
            addSectionTitle(document, "Plan Médicamenteux");
            Paragraph medication = new Paragraph(carePlan.getMedicationPlan())
                    .setMarginBottom(10);
            document.add(medication);

            Paragraph medicationStatus = new Paragraph("Statut: " +
                    (carePlan.getMedicationStatus() != null ? carePlan.getMedicationStatus().name() : "TODO"))
                    .setItalic()
                    .setMarginBottom(15);
            document.add(medicationStatus);
        }
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
