package com.maisonedition.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import com.maisonedition.entity.Commande;
import com.maisonedition.entity.StatutCommande;
import com.maisonedition.repository.CommandeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final CommandeRepository commandeRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] generateInvoice(Long commandeId, Long userId) {
        Commande commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new RuntimeException("Commande non trouvée"));

        // Security check: user must own the order
        if (!commande.getUtilisateur().getId().equals(userId)) {
            throw new RuntimeException("Accès non autorisé à cette commande");
        }

        // Only generate invoice for paid orders
        if (commande.getStatut() == StatutCommande.EN_ATTENTE ||
            commande.getStatut() == StatutCommande.ANNULEE) {
            throw new RuntimeException("Impossible de générer une facture pour cette commande");
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter writer = PdfWriter.getInstance(document, baos);

            document.open();

            // Use Helvetica for reliable PDF generation
            Font titleFont = new Font(Font.HELVETICA, 24, Font.BOLD, new java.awt.Color(0x81, 0x39, 0x20));
            Font headerFont = new Font(Font.HELVETICA, 12, Font.BOLD);
            Font normalFont = new Font(Font.HELVETICA, 10);
            Font boldFont = new Font(Font.HELVETICA, 10, Font.BOLD);
            Font smallFont = new Font(Font.HELVETICA, 8, new java.awt.Color(0x66, 0x66, 0x66));

            // Header with company info
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{1, 1});

            // Company info (left)
            PdfPCell companyCell = new PdfPCell();
            companyCell.setBorder(Rectangle.NO_BORDER);
            companyCell.setPaddingBottom(20);

            Paragraph companyName = new Paragraph("Dar Adloun", titleFont);
            companyCell.addElement(companyName);

            Paragraph companyInfo = new Paragraph();
            companyInfo.setFont(smallFont);
            companyInfo.add("Publishing House\n");
            companyInfo.add("www.dar-adloun.com\n");
            companyInfo.add("contact@dar-adloun.com");
            companyCell.addElement(companyInfo);

            headerTable.addCell(companyCell);

            // Invoice info (right)
            PdfPCell invoiceInfoCell = new PdfPCell();
            invoiceInfoCell.setBorder(Rectangle.NO_BORDER);
            invoiceInfoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            invoiceInfoCell.setPaddingBottom(20);

            Paragraph invoiceTitle = new Paragraph("INVOICE / FACTURE", headerFont);
            invoiceTitle.setAlignment(Element.ALIGN_RIGHT);
            invoiceInfoCell.addElement(invoiceTitle);

            Paragraph invoiceNumber = new Paragraph();
            invoiceNumber.setAlignment(Element.ALIGN_RIGHT);
            invoiceNumber.setFont(normalFont);
            invoiceNumber.add("Invoice #: INV-" + String.format("%06d", commande.getId()) + "\n");
            invoiceNumber.add("Date: " + commande.getDateCommande().format(DATETIME_FORMATTER));
            invoiceInfoCell.addElement(invoiceNumber);

            headerTable.addCell(invoiceInfoCell);
            document.add(headerTable);

            // Separator line
            LineSeparator separator = new LineSeparator();
            separator.setLineColor(new java.awt.Color(0x81, 0x39, 0x20));
            document.add(new Chunk(separator));
            document.add(Chunk.NEWLINE);

            // Bill To section
            PdfPTable billingTable = new PdfPTable(2);
            billingTable.setWidthPercentage(100);
            billingTable.setWidths(new float[]{1, 1});
            billingTable.setSpacingBefore(20);

            // Customer info
            PdfPCell customerCell = new PdfPCell();
            customerCell.setBorder(Rectangle.NO_BORDER);

            Paragraph billTo = new Paragraph("BILL TO:", headerFont);
            customerCell.addElement(billTo);

            Paragraph customerInfo = new Paragraph();
            customerInfo.setFont(normalFont);
            String customerName = commande.getNomComplet() != null ? commande.getNomComplet() :
                    commande.getUtilisateur().getPrenom() + " " + commande.getUtilisateur().getNom();
            customerInfo.add(customerName + "\n");
            customerInfo.add(commande.getUtilisateur().getEmail() + "\n");
            if (commande.getAdresse() != null) {
                customerInfo.add(commande.getAdresse() + "\n");
                if (commande.getVille() != null) {
                    customerInfo.add(commande.getVille());
                    if (commande.getCodePostal() != null) {
                        customerInfo.add(" " + commande.getCodePostal());
                    }
                    customerInfo.add("\n");
                }
                if (commande.getPays() != null) {
                    customerInfo.add(commande.getPays() + "\n");
                }
            }
            if (commande.getTelephone() != null) {
                customerInfo.add("Tel: " + commande.getTelephone());
            }
            customerCell.addElement(customerInfo);
            billingTable.addCell(customerCell);

            // Order status
            PdfPCell statusCell = new PdfPCell();
            statusCell.setBorder(Rectangle.NO_BORDER);
            statusCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

            Paragraph statusLabel = new Paragraph("STATUS:", headerFont);
            statusLabel.setAlignment(Element.ALIGN_RIGHT);
            statusCell.addElement(statusLabel);

            String statusText = getStatusText(commande.getStatut());
            Paragraph status = new Paragraph(statusText, boldFont);
            status.setAlignment(Element.ALIGN_RIGHT);
            statusCell.addElement(status);

            billingTable.addCell(statusCell);
            document.add(billingTable);

            // Order details table
            PdfPTable itemsTable = new PdfPTable(4);
            itemsTable.setWidthPercentage(100);
            itemsTable.setWidths(new float[]{3, 1.5f, 1, 1.5f});
            itemsTable.setSpacingBefore(30);

            // Table header
            java.awt.Color headerBg = new java.awt.Color(0x81, 0x39, 0x20);
            Font tableHeaderFont = new Font(Font.HELVETICA, 10, Font.BOLD, java.awt.Color.WHITE);

            PdfPCell descHeader = new PdfPCell(new Phrase("Description", tableHeaderFont));
            descHeader.setBackgroundColor(headerBg);
            descHeader.setPadding(8);
            itemsTable.addCell(descHeader);

            PdfPCell typeHeader = new PdfPCell(new Phrase("Type", tableHeaderFont));
            typeHeader.setBackgroundColor(headerBg);
            typeHeader.setPadding(8);
            itemsTable.addCell(typeHeader);

            PdfPCell qtyHeader = new PdfPCell(new Phrase("Qty", tableHeaderFont));
            qtyHeader.setBackgroundColor(headerBg);
            qtyHeader.setPadding(8);
            qtyHeader.setHorizontalAlignment(Element.ALIGN_CENTER);
            itemsTable.addCell(qtyHeader);

            PdfPCell priceHeader = new PdfPCell(new Phrase("Price (SAR)", tableHeaderFont));
            priceHeader.setBackgroundColor(headerBg);
            priceHeader.setPadding(8);
            priceHeader.setHorizontalAlignment(Element.ALIGN_RIGHT);
            itemsTable.addCell(priceHeader);

            // Item row
            String itemDescription;
            if (commande.getLivre() != null) {
                itemDescription = commande.getLivre().getTitre();
            } else {
                itemDescription = "Global Subscription / Abonnement Global";
            }

            PdfPCell descCell = new PdfPCell(new Phrase(itemDescription, normalFont));
            descCell.setPadding(8);
            descCell.setBackgroundColor(new java.awt.Color(0xF9, 0xF9, 0xF9));
            itemsTable.addCell(descCell);

            PdfPCell typeCell = new PdfPCell(new Phrase(getTypeText(commande.getType().name()), normalFont));
            typeCell.setPadding(8);
            typeCell.setBackgroundColor(new java.awt.Color(0xF9, 0xF9, 0xF9));
            itemsTable.addCell(typeCell);

            PdfPCell qtyCell = new PdfPCell(new Phrase("1", normalFont));
            qtyCell.setPadding(8);
            qtyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            qtyCell.setBackgroundColor(new java.awt.Color(0xF9, 0xF9, 0xF9));
            itemsTable.addCell(qtyCell);

            PdfPCell priceCell = new PdfPCell(new Phrase(commande.getMontant().toString(), normalFont));
            priceCell.setPadding(8);
            priceCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            priceCell.setBackgroundColor(new java.awt.Color(0xF9, 0xF9, 0xF9));
            itemsTable.addCell(priceCell);

            document.add(itemsTable);

            // Total section
            PdfPTable totalTable = new PdfPTable(2);
            totalTable.setWidthPercentage(40);
            totalTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalTable.setSpacingBefore(20);

            PdfPCell totalLabelCell = new PdfPCell(new Phrase("TOTAL:", boldFont));
            totalLabelCell.setBorder(Rectangle.TOP);
            totalLabelCell.setPadding(8);
            totalTable.addCell(totalLabelCell);

            PdfPCell totalValueCell = new PdfPCell(new Phrase(commande.getMontant().toString() + " SAR", boldFont));
            totalValueCell.setBorder(Rectangle.TOP);
            totalValueCell.setPadding(8);
            totalValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalTable.addCell(totalValueCell);

            document.add(totalTable);

            // Subscription period if applicable
            if (commande.getDateDebutAcces() != null && commande.getDateFinAcces() != null) {
                Paragraph subscriptionPeriod = new Paragraph();
                subscriptionPeriod.setFont(normalFont);
                subscriptionPeriod.setSpacingBefore(20);
                subscriptionPeriod.add("Access Period / Période d'accès: ");
                subscriptionPeriod.add(commande.getDateDebutAcces().format(DATE_FORMATTER));
                subscriptionPeriod.add(" - ");
                subscriptionPeriod.add(commande.getDateFinAcces().format(DATE_FORMATTER));
                document.add(subscriptionPeriod);
            }

            // Footer
            Paragraph footer = new Paragraph();
            footer.setFont(smallFont);
            footer.setSpacingBefore(50);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.add("Thank you for your purchase!\n");
            footer.add("Merci pour votre achat!\n");
            footer.add("\n");
            footer.add("For any questions, contact us at contact@dar-adloun.com");
            document.add(footer);

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération de la facture: " + e.getMessage(), e);
        }
    }

    private String getStatusText(StatutCommande statut) {
        return switch (statut) {
            case EN_ATTENTE -> "Pending / En attente";
            case PAYEE -> "Paid / Payée";
            case EN_PREPARATION -> "Processing / En préparation";
            case EXPEDIEE -> "Shipped / Expédiée";
            case LIVREE -> "Delivered / Livrée";
            case ANNULEE -> "Cancelled / Annulée";
            case REMBOURSEE -> "Refunded / Remboursée";
        };
    }

    private String getTypeText(String type) {
        return switch (type) {
            case "PAPIER" -> "Paper / Papier";
            case "NUMERIQUE" -> "Digital / Numérique";
            case "LECTURE_LIVRE" -> "E-Reading";
            case "ABONNEMENT_MENSUEL" -> "Monthly Sub.";
            case "ABONNEMENT_ANNUEL" -> "Annual Sub.";
            default -> type;
        };
    }
}
