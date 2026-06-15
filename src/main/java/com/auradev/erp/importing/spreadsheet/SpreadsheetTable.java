package com.auradev.erp.importing.spreadsheet;

import java.util.List;
import java.util.Map;

/** Normalized tabular data from a spreadsheet file (format-agnostic). */
public record SpreadsheetTable(
        List<String> headers,
        List<Map<String, String>> rows
) {}
