package com.auradev.erp.importing.spreadsheet;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Component
public class ExcelSpreadsheetReader implements SpreadsheetReader {

    @Override
    public boolean supports(String filename, String contentType) {
        if (filename != null) {
            String lower = filename.toLowerCase(Locale.ROOT);
            if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) return true;
        }
        return contentType != null && (
                contentType.contains("spreadsheetml") ||
                contentType.contains("ms-excel") ||
                contentType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Override
    public SpreadsheetTable read(InputStream input) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(input)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                return new SpreadsheetTable(List.of(), List.of());
            }

            Iterator<Row> rowIt = sheet.iterator();
            if (!rowIt.hasNext()) {
                return new SpreadsheetTable(List.of(), List.of());
            }

            Row headerRow = rowIt.next();
            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(normalizeHeader(readCell(cell)));
            }

            List<Map<String, String>> rows = new ArrayList<>();
            int rowNum = 2;
            while (rowIt.hasNext()) {
                Row row = rowIt.next();
                if (isEmptyRow(row)) {
                    rowNum++;
                    continue;
                }
                Map<String, String> values = new LinkedHashMap<>();
                for (int i = 0; i < headers.size(); i++) {
                    String key = headers.get(i);
                    if (key.isBlank()) continue;
                    Cell cell = row.getCell(i, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    values.put(key, readCell(cell));
                }
                values.put("_row", String.valueOf(rowNum));
                rows.add(values);
                rowNum++;
            }

            return new SpreadsheetTable(headers.stream().filter(h -> !h.isBlank()).toList(), rows);
        }
    }

    private boolean isEmptyRow(Row row) {
        if (row == null) return true;
        for (Cell cell : row) {
            if (!readCell(cell).isBlank()) return false;
        }
        return true;
    }

    private String readCell(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double v = cell.getNumericCellValue();
                if (v == Math.rint(v)) yield String.valueOf((long) v);
                yield BigDecimalish.format(v);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield readCellValue(cell);
                } catch (Exception e) {
                    yield cell.getCellFormula();
                }
            }
            default -> "";
        };
    }

    private String readCellValue(Cell cell) {
        return switch (cell.getCachedFormulaResultType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double v = cell.getNumericCellValue();
                if (v == Math.rint(v)) yield String.valueOf((long) v);
                yield BigDecimalish.format(v);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private String normalizeHeader(String raw) {
        return raw.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_|_$", "");
    }

    private static final class BigDecimalish {
        static String format(double v) {
            return new java.math.BigDecimal(String.valueOf(v))
                    .stripTrailingZeros()
                    .toPlainString();
        }
    }
}
