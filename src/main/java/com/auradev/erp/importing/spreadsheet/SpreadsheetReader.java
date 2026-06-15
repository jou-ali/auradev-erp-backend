package com.auradev.erp.importing.spreadsheet;

import java.io.IOException;
import java.io.InputStream;

/** Parses spreadsheet bytes into a normalized table. Implementations stay format-specific. */
public interface SpreadsheetReader {

    boolean supports(String filename, String contentType);

    SpreadsheetTable read(InputStream input) throws IOException;
}
