package com.auradev.erp.importing.inventory;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/** Sample spreadsheet for bulk stock adjustments — same fields as Quick adjust. */
@Component
public class InventoryStockAdjustmentSampleGenerator {

    private static final Object[][] SAMPLE_ROWS = {
            {"GRN-RICE-25", "add", 50, "grn", "Morning delivery - rice sacks"},
            {"GRN-TOOR-01", "add", 30, "grn", "Dal restock"},
            {"DRY-MILK-05", "add", 48, "grn", "Dairy truck"},
            {"BEV-COK-75", "add", 24, "grn", ""},
            {"SNK-PRLG-01", "remove", 5, "damage", "Broken packets"},
            {"PC-SOAP-12", "add", 36, "count", "Monthly stock count correction"},
    };

    public byte[] generate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(StockAdjustmentImportSchema.SHEET_NAME);
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row header = sheet.createRow(0);
            for (int i = 0; i < StockAdjustmentImportSchema.HEADERS.size(); i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(StockAdjustmentImportSchema.HEADERS.get(i));
                cell.setCellStyle(headerStyle);
            }

            for (int r = 0; r < SAMPLE_ROWS.length; r++) {
                Row row = sheet.createRow(r + 1);
                Object[] values = SAMPLE_ROWS[r];
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
                    "Bulk stock adjustments — same as Quick adjust in Inventory.",
                    "Required columns: sku, adjustment, quantity, reason",
                    "adjustment: add | remove",
                    "quantity: positive number (always)",
                    "reason: grn | damage | return | count",
                    "notes: optional description shown in movement history",
                    "grn = goods received, damage = wastage, return = customer return, count = stock correction",
                    "Each row must match an existing product SKU in your catalogue.",
            };
            for (int i = 0; i < notes.length; i++) {
                help.createRow(i).createCell(0).setCellValue(notes[i]);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }
}
