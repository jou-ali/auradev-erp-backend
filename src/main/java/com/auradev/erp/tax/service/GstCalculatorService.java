package com.auradev.erp.tax.service;

import com.auradev.erp.common.util.MoneyUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * The single authoritative source for all GST arithmetic in the ERP.
 *
 * <h2>Design goals</h2>
 * <ul>
 *   <li>Never use {@code float} or {@code double} — every intermediate value
 *       is a {@link BigDecimal} rounded with {@link RoundingMode#HALF_UP}.</li>
 *   <li>Support both tax-exclusive and tax-inclusive (MRP) pricing.</li>
 *   <li>Support both intra-state (CGST + SGST) and inter-state (IGST) billing.</li>
 *   <li>Support line-level and bill-level discounts; bill-level discounts are
 *       distributed proportionally so that per-line tax figures remain accurate.</li>
 *   <li>Apply a whole-rupee round-off to the grand total.</li>
 * </ul>
 *
 * <h2>Caller contract</h2>
 * <p>All {@link BigDecimal} inputs must be non-null and non-negative.
 * {@code gstRate} is expressed as a percentage value (e.g. {@code 18} for
 * 18 %, not {@code 0.18}).  {@code billDiscount} must be 0 when no
 * bill-level discount applies.</p>
 */
@Service
public class GstCalculatorService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal TWO     = new BigDecimal("2");
    private static final BigDecimal ZERO    = BigDecimal.ZERO;

    // =========================================================================
    // Data records
    // =========================================================================

    /**
     * Per-line input to the calculator.
     *
     * @param unitPrice    the price per unit (MRP when {@code priceIncludesTax=true},
     *                     otherwise the base/taxable price per unit)
     * @param quantity     the quantity sold
     * @param lineDiscount an absolute line-level discount applied before tax
     * @param gstRate      the applicable GST rate as a percentage (0 / 5 / 12 / 18 / 28)
     */
    public record LineInput(
            BigDecimal unitPrice,
            BigDecimal quantity,
            BigDecimal lineDiscount,
            BigDecimal gstRate
    ) {}

    /**
     * Calculated tax breakdown for a single line item.
     *
     * @param taxableValue         the net taxable amount after all discounts and
     *                             tax extraction (if price-inclusive)
     * @param cgst                 CGST component (intra-state only; 0 for inter-state)
     * @param sgst                 SGST component (intra-state only; 0 for inter-state)
     * @param igst                 IGST component (inter-state only; 0 for intra-state)
     * @param lineTotal            taxableValue + cgst + sgst + igst
     */
    public record LineOutput(
            BigDecimal taxableValue,
            BigDecimal cgst,
            BigDecimal sgst,
            BigDecimal igst,
            BigDecimal lineTotal
    ) {}

    /**
     * Aggregated tax breakdown for the whole bill.
     *
     * @param lines                per-line outputs, in the same order as the inputs
     * @param subtotal             sum of all per-line taxable values (after bill discount)
     * @param billDiscountApplied  the bill-level discount amount actually applied
     *                             (converted from percent to amount if needed)
     * @param cgstTotal            sum of per-line CGST
     * @param sgstTotal            sum of per-line SGST
     * @param igstTotal            sum of per-line IGST
     * @param roundOff             whole-rupee adjustment (may be negative)
     * @param grandTotal           the amount the customer actually pays
     */
    public record BillTaxOutput(
            List<LineOutput> lines,
            BigDecimal subtotal,
            BigDecimal billDiscountApplied,
            BigDecimal cgstTotal,
            BigDecimal sgstTotal,
            BigDecimal igstTotal,
            BigDecimal roundOff,
            BigDecimal grandTotal
    ) {}

    // =========================================================================
    // Main calculation
    // =========================================================================

    /**
     * Compute the full GST breakdown for a bill.
     *
     * @param lines            the line items
     * @param billDiscount     the bill-level discount; interpretation depends on
     *                         {@code discountMode}
     * @param discountMode     whether {@code billDiscount} is an absolute amount
     *                         or a percentage
     * @param priceIncludesTax {@code true} if {@code unitPrice} is the MRP
     *                         (tax-inclusive); {@code false} if it is the base price
     * @param intraState       {@code true} for intra-state sales (CGST + SGST);
     *                         {@code false} for inter-state sales (IGST)
     * @return a fully populated {@link BillTaxOutput}
     */
    public BillTaxOutput calculate(
            List<LineInput> lines,
            BigDecimal billDiscount,
            DiscountMode discountMode,
            boolean priceIncludesTax,
            boolean intraState) {

        // ------------------------------------------------------------------
        // Step 1: Compute pre-bill-discount gross value per line
        //         (unitPrice * qty - lineDiscount) before extracting tax.
        //         This is used to determine proportional bill-discount allocation.
        // ------------------------------------------------------------------
        List<BigDecimal> grossValues = new ArrayList<>(lines.size());
        BigDecimal grossTotal = ZERO;

        for (LineInput line : lines) {
            BigDecimal gross = MoneyUtils.roundHalfUp2(
                    line.unitPrice().multiply(line.quantity()).subtract(line.lineDiscount()));
            grossValues.add(gross);
            grossTotal = grossTotal.add(gross);
        }

        // ------------------------------------------------------------------
        // Step 2: Convert percent bill-discount to amount
        // ------------------------------------------------------------------
        BigDecimal billDiscountAmount;
        if (discountMode == DiscountMode.PERCENT) {
            billDiscountAmount = MoneyUtils.roundHalfUp2(
                    grossTotal.multiply(billDiscount).divide(HUNDRED, 10, RoundingMode.HALF_UP));
        } else {
            billDiscountAmount = MoneyUtils.roundHalfUp2(billDiscount);
        }

        // ------------------------------------------------------------------
        // Step 3: Per-line calculation
        // ------------------------------------------------------------------
        List<LineOutput> lineOutputs = new ArrayList<>(lines.size());
        BigDecimal subtotal    = ZERO;
        BigDecimal cgstTotal   = ZERO;
        BigDecimal sgstTotal   = ZERO;
        BigDecimal igstTotal   = ZERO;

        for (int i = 0; i < lines.size(); i++) {
            LineInput line      = lines.get(i);
            BigDecimal gross    = grossValues.get(i);

            // Proportional share of the bill-level discount for this line
            BigDecimal proportionalBillDisc;
            if (grossTotal.compareTo(ZERO) == 0) {
                proportionalBillDisc = ZERO;
            } else {
                proportionalBillDisc = MoneyUtils.roundHalfUp2(
                        billDiscountAmount
                                .multiply(gross)
                                .divide(grossTotal, 10, RoundingMode.HALF_UP));
            }

            // Taxable value
            BigDecimal taxableValue;
            if (priceIncludesTax) {
                // Back-calculate: taxable = (gross - proportionalBillDisc) / (1 + rate/100)
                BigDecimal divisor = BigDecimal.ONE.add(
                        line.gstRate().divide(HUNDRED, 10, RoundingMode.HALF_UP));
                taxableValue = MoneyUtils.roundHalfUp2(
                        gross.subtract(proportionalBillDisc)
                             .divide(divisor, 10, RoundingMode.HALF_UP));
            } else {
                // Tax-exclusive: gross is already the base; subtract the proportional discount
                taxableValue = MoneyUtils.roundHalfUp2(gross.subtract(proportionalBillDisc));
            }

            // Tax amount
            BigDecimal tax = MoneyUtils.roundHalfUp2(
                    taxableValue.multiply(line.gstRate()).divide(HUNDRED, 10, RoundingMode.HALF_UP));

            BigDecimal cgst;
            BigDecimal sgst;
            BigDecimal igst;

            if (intraState) {
                cgst = MoneyUtils.roundHalfUp2(tax.divide(TWO, 10, RoundingMode.HALF_UP));
                sgst = MoneyUtils.roundHalfUp2(tax.divide(TWO, 10, RoundingMode.HALF_UP));
                igst = ZERO;
            } else {
                cgst = ZERO;
                sgst = ZERO;
                igst = tax;
            }

            BigDecimal lineTotal = MoneyUtils.roundHalfUp2(
                    taxableValue.add(cgst).add(sgst).add(igst));

            lineOutputs.add(new LineOutput(taxableValue, cgst, sgst, igst, lineTotal));

            subtotal  = subtotal.add(taxableValue);
            cgstTotal = cgstTotal.add(cgst);
            sgstTotal = sgstTotal.add(sgst);
            igstTotal = igstTotal.add(igst);
        }

        // Final rounding of totals
        subtotal  = MoneyUtils.roundHalfUp2(subtotal);
        cgstTotal = MoneyUtils.roundHalfUp2(cgstTotal);
        sgstTotal = MoneyUtils.roundHalfUp2(sgstTotal);
        igstTotal = MoneyUtils.roundHalfUp2(igstTotal);

        // ------------------------------------------------------------------
        // Step 4: Grand total and round-off
        // ------------------------------------------------------------------
        BigDecimal grandTotalBeforeRounding = MoneyUtils.roundHalfUp2(
                subtotal.add(cgstTotal).add(sgstTotal).add(igstTotal));

        BigDecimal grandTotalRounded = MoneyUtils.toWhole(grandTotalBeforeRounding);
        BigDecimal roundOff = MoneyUtils.roundHalfUp2(
                grandTotalRounded.subtract(grandTotalBeforeRounding));
        BigDecimal grandTotal = MoneyUtils.roundHalfUp2(grandTotalBeforeRounding.add(roundOff));

        return new BillTaxOutput(
                lineOutputs,
                subtotal,
                billDiscountAmount,
                cgstTotal,
                sgstTotal,
                igstTotal,
                roundOff,
                grandTotal
        );
    }
}
