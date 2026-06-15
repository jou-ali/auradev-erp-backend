package com.auradev.erp.importing.inventory;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
public class InventoryImportTemplateGenerator {

    public byte[] generate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(InventoryImportSchema.SHEET_NAME);
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row header = sheet.createRow(0);
            for (int i = 0; i < InventoryImportSchema.HEADERS.size(); i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(InventoryImportSchema.HEADERS.get(i));
                cell.setCellStyle(headerStyle);
            }

            Object[][] samples = {
                    {
                            "Sona Masoori Rice", "GRN-RICE-25", "8901234500011", "Grains",
                            "weight_kg", "kg", 78, 68, 58, 5, 100, 20
                    },
                    {
                            "Nandini Toned Milk 500ml", "DRY-MILK-05", "8901234500073", "Dairy",
                            "unit", "pcs", 26, 25, 23, 0, 50, 10
                    }
            };

            for (int r = 0; r < samples.length; r++) {
                Row row = sheet.createRow(r + 1);
                Object[] values = samples[r];
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
                    "Fill the Products sheet. Do not change header names.",
                    "Required: name, sku, category, mrp, selling_price",
                    "initial_stock = opening quantity for new products only.",
                    "SKU and barcode must be unique. Duplicate SKUs are skipped.",
                    "For restocking existing products, use the Stock sample template instead.",
                    "category: created automatically if it does not exist.",
                    "unit_type: unit | weight_kg | weight_g | volume_l | volume_ml (or kg, pcs)",
            };
            for (int i = 0; i < notes.length; i++) {
                help.createRow(i).createCell(0).setCellValue(notes[i]);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }
}
