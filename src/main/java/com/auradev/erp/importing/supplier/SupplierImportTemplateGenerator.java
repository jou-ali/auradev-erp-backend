package com.auradev.erp.importing.supplier;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
public class SupplierImportTemplateGenerator {

    public byte[] generate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(SupplierImportSchema.SHEET_NAME);
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row header = sheet.createRow(0);
            for (int i = 0; i < SupplierImportSchema.HEADERS.size(); i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(SupplierImportSchema.HEADERS.get(i));
                cell.setCellStyle(headerStyle);
            }

            Object[][] samples = {
                    {"Sri Venkateshwara Distributors", "Ramesh", "9448123456", "ramesh@svd.in", "29AABCS1234F1Z5", "Hubli Road, Belagavi"},
                    {"Coastal Beverages Co.", "Deepa", "9731987654", "orders@coastalbev.in", "29AABCC3456J1Z1", "Mangaluru"},
            };

            for (int r = 0; r < samples.length; r++) {
                Row row = sheet.createRow(r + 1);
                Object[] values = samples[r];
                for (int c = 0; c < values.length; c++) {
                    row.createCell(c).setCellValue(String.valueOf(values[c]));
                }
            }

            Sheet help = workbook.createSheet("Instructions");
            String[] notes = {
                    "Required column: name",
                    "Duplicate supplier names are skipped.",
                    "GSTIN must be unique when provided.",
                    "Use this sheet to add many suppliers before bulk purchase import.",
            };
            for (int i = 0; i < notes.length; i++) {
                help.createRow(i).createCell(0).setCellValue(notes[i]);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }
}
