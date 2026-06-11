package com.auradev.erp.billing.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listens for {@link BillCreatedEvent} and triggers asynchronous post-sale
 * processing, currently receipt PDF generation.
 *
 * <p>The listener runs on the {@code erp-async-} thread pool (configured in
 * {@link com.auradev.erp.config.AsyncConfig}) so that the originating HTTP request
 * returns immediately after the bill transaction commits.  Any failure here must
 * NOT affect the bill; errors are logged and should trigger a retry via a dead-letter
 * mechanism in production.</p>
 *
 * <h2>PDF generation — production implementation notes</h2>
 * <ol>
 *   <li>Load the Bill aggregate (items + payments) from the database.</li>
 *   <li>Render a GST-compliant PDF receipt using
 *       <a href="https://github.com/LibrePDF/OpenPDF">OpenPDF</a>:
 *       {@code com.lowagie:openpdf} (already in {@code pom.xml}).</li>
 *   <li>Upload the PDF to Supabase Storage using the AWS SDK S3-compatible client
 *       ({@code software.amazon.awssdk:s3} in {@code pom.xml}).</li>
 *   <li>Update {@code bills.receipt_url} with the public signed URL.</li>
 * </ol>
 */
@Slf4j
@Component
public class BillEventListener {

    /**
     * Handle a {@link BillCreatedEvent} asynchronously.
     *
     * <p>Logs a placeholder message for PDF generation. In production, replace the
     * log statement with the OpenPDF + Supabase Storage pipeline described in the
     * class Javadoc above.</p>
     *
     * @param event the bill-created event carrying the bill UUID and tenant
     */
    @Async("asyncExecutor")
    @EventListener
    public void onBillCreated(BillCreatedEvent event) {
        log.info(
            "Receipt PDF generation for bill {} (tenant={}) — implement with OpenPDF",
            event.billId(),
            event.tenantId()
        );
        // TODO: implement PDF generation with OpenPDF and upload to Supabase Storage.
        //
        // Example outline:
        //
        //   Bill bill = billRepository.findById(event.billId()).orElseThrow();
        //
        //   Document doc = new Document(PageSize.A4);
        //   ByteArrayOutputStream out = new ByteArrayOutputStream();
        //   PdfWriter.getInstance(doc, out);
        //   doc.open();
        //   // ... render GST invoice layout ...
        //   doc.close();
        //
        //   String key = "receipts/" + event.tenantId() + "/" + event.billId() + ".pdf";
        //   s3Client.putObject(PutObjectRequest.builder()
        //       .bucket(bucketName).key(key).contentType("application/pdf").build(),
        //       RequestBody.fromBytes(out.toByteArray()));
        //
        //   String url = s3Client.utilities().getUrl(b -> b.bucket(bucketName).key(key)).toString();
        //   billRepository.updateReceiptUrl(event.billId(), url);
    }
}
