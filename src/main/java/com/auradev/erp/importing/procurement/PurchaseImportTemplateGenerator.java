package com.auradev.erp.importing.procurement;

import com.auradev.erp.catalog.entity.Product;
import com.auradev.erp.catalog.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PurchaseImportTemplateGenerator {

    private final ProductRepository productRepository;

    public byte[] generate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(PurchaseImportSchema.SHEET_NAME);
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row header = sheet.createRow(0);
            for (int i = 0; i < PurchaseImportSchema.HEADERS.size(); i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(PurchaseImportSchema.HEADERS.get(i));
                cell.setCellStyle(headerStyle);
            }

            String today = LocalDate.now().toString();
            String due = LocalDate.now().plusDays(30).toString();
            List<Object[]> samples = buildSampleRows(today, due);

            for (int r = 0; r < samples.size(); r++) {
                Row row = sheet.createRow(r + 1);
                Object[] values = samples.get(r);
                for (int c = 0; c < values.length; c++) {
                    Cell cell = row.createCell(c);
                    Object v = values[c];
                    if (v instanceof Number n) {
                        cell.setCellValue(n.doubleValue());
                    } else {
                        cell.setCellValue(String.valueOf(v));
                    }
                }
            }

            Sheet help = workbook.createSheet("Instructions");
            String[] notes = {
                    "Select the supplier in the app first — this sheet is for ONE supplier only.",
                    "SKU must match Inventory exactly (open Inventory → copy SKU column).",
                    "Sample rows below use real SKUs from your catalogue.",
                    "One row per product. Same purchase_ref = one draft bill (multiple lines).",
                    "Required: bill_date, sku, quantity, rate.",
                    "Creates DRAFT purchases — Receive GRN when goods arrive.",
            };
            for (int i = 0; i < notes.length; i++) {
                help.createRow(i).createCell(0).setCellValue(notes[i]);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private List<Object[]> buildSampleRows(String today, String due) {
        List<Product> products = productRepository.findByActiveTrue(PageRequest.of(0, 3)).getContent();
        if (products.isEmpty()) {
            List<Object[]> rows = new ArrayList<>();
            rows.add(new Object[]{"INV-001", today, due, "Example invoice ref", "YOUR-SKU-HERE", 10, 50});
            return rows;
        }

        List<Object[]> rows = new ArrayList<>();
        Product first = products.getFirst();
        rows.add(new Object[]{
                "INV-001", today, due, "Supplier invoice ref",
                first.getSku(), 10, sampleRate(first)
        });
        if (products.size() > 1) {
            Product second = products.get(1);
            rows.add(new Object[]{
                    "INV-001", today, due, "",
                    second.getSku(), 5, sampleRate(second)
            });
        }
        if (products.size() > 2) {
            Product third = products.get(2);
            rows.add(new Object[]{
                    "INV-002", today, "", "",
                    third.getSku(), 20, sampleRate(third)
            });
        }
        return rows;
    }

    private static double sampleRate(Product p) {
        BigDecimal cost = p.getCostPrice();
        if (cost != null && cost.signum() > 0) {
            return cost.doubleValue();
        }
        return p.getPriceSelling() != null ? p.getPriceSelling().doubleValue() : 1;
    }
}
