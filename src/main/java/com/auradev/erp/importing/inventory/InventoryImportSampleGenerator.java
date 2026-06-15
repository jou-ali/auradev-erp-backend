package com.auradev.erp.importing.inventory;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/** Serves the committed sample workbook from classpath (see scripts/generate-import-sample.mjs). */
@Component
public class InventoryImportSampleGenerator {

    private static final String SAMPLE_PATH = "samples/inventory-import-sample-100.xlsx";

    public byte[] load() throws IOException {
        ClassPathResource resource = new ClassPathResource(SAMPLE_PATH);
        if (!resource.exists()) {
            throw new IOException("Sample file missing: " + SAMPLE_PATH);
        }
        try (InputStream in = resource.getInputStream()) {
            return in.readAllBytes();
        }
    }
}
