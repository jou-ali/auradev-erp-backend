package com.auradev.erp.importing.spreadsheet;

import com.auradev.erp.common.error.BusinessException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
public class SpreadsheetReaderRegistry {

    private final List<SpreadsheetReader> readers;

    public SpreadsheetReaderRegistry(List<SpreadsheetReader> readers) {
        this.readers = readers;
    }

    public SpreadsheetTable read(String filename, String contentType, InputStream input) throws IOException {
        for (SpreadsheetReader reader : readers) {
            if (reader.supports(filename, contentType)) {
                return reader.read(input);
            }
        }
        throw new BusinessException(
                "UNSUPPORTED_FILE",
                "Unsupported file type. Upload an Excel workbook (.xlsx).");
    }
}
