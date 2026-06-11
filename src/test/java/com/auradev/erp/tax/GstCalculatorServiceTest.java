package com.auradev.erp.tax;

import com.auradev.erp.tax.service.DiscountMode;
import com.auradev.erp.tax.service.GstCalculatorService;
import com.auradev.erp.tax.service.GstCalculatorService.BillTaxOutput;
import com.auradev.erp.tax.service.GstCalculatorService.LineInput;
import com.auradev.erp.tax.service.GstCalculatorService.LineOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Exhaustive table-driven unit tests for {@link GstCalculatorService}.
 *
 * <p>Each test verifies ALL output fields: {@code taxableValue}, {@code cgst},
 * {@code sgst}, {@code igst}, {@code lineTotal}, {@code cgstTotal}, {@code sgstTotal},
 * {@code igstTotal}, {@code roundOff}, and {@code grandTotal}.</p>
 *
 * <p>All expected values are derived from first principles so that the tests
 * double as specification documentation.</p>
 */
class GstCalculatorServiceTest {

    private GstCalculatorService calculator;

    @BeforeEach
    void setUp() {
        calculator = new GstCalculatorService();
    }

    // =========================================================================
    // Test cases data
    // =========================================================================

    /**
     * Each argument set carries the human-readable name of the case plus all
     * inputs and expected outputs.  The order matches the parameter list of
     * {@link #gstCalculationCases(String, List, BigDecimal, DiscountMode, boolean, boolean,
     * BigDecimal, BigDecimal, BigDecimal, BigDecimal, BigDecimal, BigDecimal, BigDecimal, BigDecimal, BigDecimal, BigDecimal)}.
     */
    static Stream<Arguments> gstCalculationCases() {
        return Stream.of(

            // ----------------------------------------------------------------
            // Case 1: Zero-rated item (gstRate = 0%)
            // price=100, qty=1, lineDiscount=0, gstRate=0 → taxable=100, no tax
            // ----------------------------------------------------------------
            arguments(
                "Zero-rated item — gstRate=0%",
                List.of(new LineInput(bd("100"), bd("1"), bd("0"), bd("0"))),
                bd("0"), DiscountMode.AMOUNT, false, true,
                /* expectedLine0 */ bd("100.00"), bd("0.00"), bd("0.00"), bd("0.00"), bd("100.00"),
                /* bill totals  */ bd("0.00"), bd("0.00"), bd("0.00"), bd("0.00"), bd("100.00")
            ),

            // ----------------------------------------------------------------
            // Case 2: 5% intra-state (CGST 2.5% + SGST 2.5%)
            // price=100, qty=2, lineDiscount=0, gstRate=5 → taxable=200, tax=10, cgst=sgst=5
            // ----------------------------------------------------------------
            arguments(
                "5% intra-state — CGST 2.5% SGST 2.5%",
                List.of(new LineInput(bd("100"), bd("2"), bd("0"), bd("5"))),
                bd("0"), DiscountMode.AMOUNT, false, true,
                /* expectedLine0 */ bd("200.00"), bd("5.00"), bd("5.00"), bd("0.00"), bd("210.00"),
                /* bill totals  */ bd("5.00"), bd("5.00"), bd("0.00"), bd("0.00"), bd("210.00")
            ),

            // ----------------------------------------------------------------
            // Case 3: 12% intra-state (CGST 6% + SGST 6%)
            // price=100, qty=1, lineDiscount=0, gstRate=12 → taxable=100, tax=12, cgst=sgst=6
            // ----------------------------------------------------------------
            arguments(
                "12% intra-state — CGST 6% SGST 6%",
                List.of(new LineInput(bd("100"), bd("1"), bd("0"), bd("12"))),
                bd("0"), DiscountMode.AMOUNT, false, true,
                /* expectedLine0 */ bd("100.00"), bd("6.00"), bd("6.00"), bd("0.00"), bd("112.00"),
                /* bill totals  */ bd("6.00"), bd("6.00"), bd("0.00"), bd("0.00"), bd("112.00")
            ),

            // ----------------------------------------------------------------
            // Case 4: 18% intra-state (CGST 9% + SGST 9%)
            // price=100, qty=1, lineDiscount=0, gstRate=18 → taxable=100, tax=18, cgst=sgst=9
            // ----------------------------------------------------------------
            arguments(
                "18% intra-state — CGST 9% SGST 9%",
                List.of(new LineInput(bd("100"), bd("1"), bd("0"), bd("18"))),
                bd("0"), DiscountMode.AMOUNT, false, true,
                /* expectedLine0 */ bd("100.00"), bd("9.00"), bd("9.00"), bd("0.00"), bd("118.00"),
                /* bill totals  */ bd("9.00"), bd("9.00"), bd("0.00"), bd("0.00"), bd("118.00")
            ),

            // ----------------------------------------------------------------
            // Case 5: 18% inter-state (IGST 18%)
            // price=100, qty=1, lineDiscount=0, gstRate=18 → taxable=100, igst=18
            // ----------------------------------------------------------------
            arguments(
                "18% inter-state — IGST 18%",
                List.of(new LineInput(bd("100"), bd("1"), bd("0"), bd("18"))),
                bd("0"), DiscountMode.AMOUNT, false, false,
                /* expectedLine0 */ bd("100.00"), bd("0.00"), bd("0.00"), bd("18.00"), bd("118.00"),
                /* bill totals  */ bd("0.00"), bd("0.00"), bd("18.00"), bd("0.00"), bd("118.00")
            ),

            // ----------------------------------------------------------------
            // Case 6: Tax-inclusive price (MRP billing)
            // sellingPrice=118 (MRP), gstRate=18 → taxable=100, tax=18, cgst=sgst=9
            // taxable = 118 / (1 + 18/100) = 118 / 1.18 = 100.00
            // ----------------------------------------------------------------
            arguments(
                "Tax-inclusive price — selling 118 @ 18% → taxable=100",
                List.of(new LineInput(bd("118"), bd("1"), bd("0"), bd("18"))),
                bd("0"), DiscountMode.AMOUNT, true /*priceIncludesTax*/, true,
                /* expectedLine0 */ bd("100.00"), bd("9.00"), bd("9.00"), bd("0.00"), bd("118.00"),
                /* bill totals  */ bd("9.00"), bd("9.00"), bd("0.00"), bd("0.00"), bd("118.00")
            ),

            // ----------------------------------------------------------------
            // Case 7: Line discount before tax
            // price=100, qty=2, lineDiscount=20, gstRate=18%
            // gross = 200 - 20 = 180; taxable=180; tax=180*0.18=32.40; cgst=sgst=16.20
            // lineTotal = 180 + 32.40 = 212.40
            // grandTotal before rounding = 212.40 → rounded = 212 → roundOff = -0.40
            // ----------------------------------------------------------------
            arguments(
                "Line discount before tax — price=100 qty=2 disc=20 gst=18%",
                List.of(new LineInput(bd("100"), bd("2"), bd("20"), bd("18"))),
                bd("0"), DiscountMode.AMOUNT, false, true,
                /* expectedLine0 */ bd("180.00"), bd("16.20"), bd("16.20"), bd("0.00"), bd("212.40"),
                /* bill totals  */ bd("16.20"), bd("16.20"), bd("0.00"), bd("-0.40"), bd("212.00")
            ),

            // ----------------------------------------------------------------
            // Case 8: Bill discount proportional across 2 lines
            // Line A: price=100, qty=1, gstRate=18
            // Line B: price=200, qty=1, gstRate=18
            // grossTotal = 300; billDiscount 10% = 30
            // Line A proportional disc = 30 * 100/300 = 10; taxable = 90
            // Line B proportional disc = 30 * 200/300 = 20; taxable = 180
            // LineA tax = 90*0.18=16.20; cgst=sgst=8.10
            // LineB tax = 180*0.18=32.40; cgst=sgst=16.20
            // subtotal = 90+180 = 270
            // cgstTotal = 8.10+16.20 = 24.30; sgstTotal same
            // grandBeforeRounding = 270+24.30+24.30 = 318.60
            // rounded = 319; roundOff = 0.40
            // ----------------------------------------------------------------
            arguments(
                "Bill discount proportional — 2 lines 10% bill discount",
                List.of(
                    new LineInput(bd("100"), bd("1"), bd("0"), bd("18")),
                    new LineInput(bd("200"), bd("1"), bd("0"), bd("18"))
                ),
                bd("10"), DiscountMode.PERCENT, false, true,
                /* line 0 expected */ bd("90.00"), bd("8.10"), bd("8.10"), bd("0.00"), bd("106.20"),
                /* bill totals    */ bd("24.30"), bd("24.30"), bd("0.00"), bd("0.40"), bd("319.00")
            ),

            // ----------------------------------------------------------------
            // Case 9: Rounding — gstRate=5%, taxable=99.99
            // tax = 99.99 * 5 / 100 = 4.9995 → HALF_UP → 5.00
            // cgst = sgst = 2.50
            // lineTotal = 99.99 + 5.00 = 104.99
            // grandTotal before round = 104.99 → rounded = 105 → roundOff = 0.01
            // ----------------------------------------------------------------
            arguments(
                "Rounding HALF_UP — taxable=99.99 gst=5% → tax=5.00",
                List.of(new LineInput(bd("99.99"), bd("1"), bd("0"), bd("5"))),
                bd("0"), DiscountMode.AMOUNT, false, true,
                /* expectedLine0 */ bd("99.99"), bd("2.50"), bd("2.50"), bd("0.00"), bd("104.99"),
                /* bill totals  */ bd("2.50"), bd("2.50"), bd("0.00"), bd("0.01"), bd("105.00")
            ),

            // ----------------------------------------------------------------
            // Case 10: Grand total round-off
            // price=100, qty=1, gstRate=28
            // taxable=100; tax=28; cgst=sgst=14
            // lineTotal = 128; grandBeforeRound = 128.00 → rounded = 128 → roundOff = 0.00
            //
            // Use a fractional amount to force round-off:
            // price=300.79, qty=1, gstRate=18
            // gross=300.79; taxable=300.79; tax=300.79*18%=54.14(rounded)
            // cgst=sgst=27.07
            // lineTotal = 300.79+54.14=354.93; grandBefore=354.93 → rounded=355 → roundOff=0.07
            // ----------------------------------------------------------------
            arguments(
                "Grand total round-off — price=300.79 qty=1 gst=18%",
                List.of(new LineInput(bd("300.79"), bd("1"), bd("0"), bd("18"))),
                bd("0"), DiscountMode.AMOUNT, false, true,
                /* expectedLine0 */ bd("300.79"), bd("27.07"), bd("27.07"), bd("0.00"), bd("354.93"),
                /* bill totals  */ bd("27.07"), bd("27.07"), bd("0.00"), bd("0.07"), bd("355.00")
            ),

            // ----------------------------------------------------------------
            // Case 11: Split payment scenario — verify grandTotal equals sum of payments
            // This test verifies the GST output contract only; payments are not in
            // GstCalculatorService. We use a simple 18% example and assert grandTotal.
            // price=500, qty=1, gstRate=18 → grandTotal=590
            // Cash=300 + UPI=290 → sum=590 = grandTotal ✓
            // ----------------------------------------------------------------
            arguments(
                "Split payment scenario — grandTotal=590 matches cash(300)+UPI(290)",
                List.of(new LineInput(bd("500"), bd("1"), bd("0"), bd("18"))),
                bd("0"), DiscountMode.AMOUNT, false, true,
                /* expectedLine0 */ bd("500.00"), bd("45.00"), bd("45.00"), bd("0.00"), bd("590.00"),
                /* bill totals  */ bd("45.00"), bd("45.00"), bd("0.00"), bd("0.00"), bd("590.00")
            ),

            // ----------------------------------------------------------------
            // Case 12: Fractional kg quantity
            // qty=0.5 kg, price=68 → gross=34.00; gstRate=5%
            // taxable=34.00; tax=34*5%=1.70; cgst=sgst=0.85
            // lineTotal=35.70; grandTotal before round=35.70 → rounded=36 → roundOff=0.30
            // ----------------------------------------------------------------
            arguments(
                "Fractional kg quantity — qty=0.5 price=68 gst=5%",
                List.of(new LineInput(bd("68"), bd("0.5"), bd("0"), bd("5"))),
                bd("0"), DiscountMode.AMOUNT, false, true,
                /* expectedLine0 */ bd("34.00"), bd("0.85"), bd("0.85"), bd("0.00"), bd("35.70"),
                /* bill totals  */ bd("0.85"), bd("0.85"), bd("0.00"), bd("0.30"), bd("36.00")
            )
        );
    }

    // =========================================================================
    // Single parameterised test method
    // =========================================================================

    /**
     * Runs the GST calculator with the supplied inputs and asserts every output field.
     *
     * <p>Because {@code @ParameterizedTest} maps positional arguments, the parameter
     * list below is ordered exactly as the {@link #gstCalculationCases()} stream entries.</p>
     *
     * @param displayName      human-readable case label (used by JUnit in the test report)
     * @param lines            line inputs
     * @param billDiscount     bill-level discount value
     * @param discountMode     whether the discount is an amount or a percentage
     * @param priceIncludesTax true if unit prices are tax-inclusive (MRP)
     * @param intraState       true for CGST+SGST; false for IGST
     * @param expTaxable       expected {@code LineOutput.taxableValue} for line 0
     * @param expCgst          expected {@code LineOutput.cgst} for line 0
     * @param expSgst          expected {@code LineOutput.sgst} for line 0
     * @param expIgst          expected {@code LineOutput.igst} for line 0
     * @param expLineTotal     expected {@code LineOutput.lineTotal} for line 0
     * @param expCgstTotal     expected bill-level {@code cgstTotal}
     * @param expSgstTotal     expected bill-level {@code sgstTotal}
     * @param expIgstTotal     expected bill-level {@code igstTotal}
     * @param expRoundOff      expected {@code roundOff}
     * @param expGrandTotal    expected {@code grandTotal}
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("gstCalculationCases")
    @DisplayName("GST calculator — exhaustive table-driven cases")
    void shouldCalculateGstCorrectly(
            String displayName,
            List<LineInput> lines,
            BigDecimal billDiscount,
            DiscountMode discountMode,
            boolean priceIncludesTax,
            boolean intraState,
            // line 0 expectations
            BigDecimal expTaxable,
            BigDecimal expCgst,
            BigDecimal expSgst,
            BigDecimal expIgst,
            BigDecimal expLineTotal,
            // bill-level expectations
            BigDecimal expCgstTotal,
            BigDecimal expSgstTotal,
            BigDecimal expIgstTotal,
            BigDecimal expRoundOff,
            BigDecimal expGrandTotal) {

        // Act
        BillTaxOutput output = calculator.calculate(
                lines, billDiscount, discountMode, priceIncludesTax, intraState);

        // Assert line 0
        LineOutput line0 = output.lines().get(0);
        assertThat(line0.taxableValue())
                .as("line0.taxableValue [%s]", displayName)
                .isEqualByComparingTo(expTaxable);
        assertThat(line0.cgst())
                .as("line0.cgst [%s]", displayName)
                .isEqualByComparingTo(expCgst);
        assertThat(line0.sgst())
                .as("line0.sgst [%s]", displayName)
                .isEqualByComparingTo(expSgst);
        assertThat(line0.igst())
                .as("line0.igst [%s]", displayName)
                .isEqualByComparingTo(expIgst);
        assertThat(line0.lineTotal())
                .as("line0.lineTotal [%s]", displayName)
                .isEqualByComparingTo(expLineTotal);

        // Assert bill totals
        assertThat(output.cgstTotal())
                .as("cgstTotal [%s]", displayName)
                .isEqualByComparingTo(expCgstTotal);
        assertThat(output.sgstTotal())
                .as("sgstTotal [%s]", displayName)
                .isEqualByComparingTo(expSgstTotal);
        assertThat(output.igstTotal())
                .as("igstTotal [%s]", displayName)
                .isEqualByComparingTo(expIgstTotal);
        assertThat(output.roundOff())
                .as("roundOff [%s]", displayName)
                .isEqualByComparingTo(expRoundOff);
        assertThat(output.grandTotal())
                .as("grandTotal [%s]", displayName)
                .isEqualByComparingTo(expGrandTotal);

        // Additional contract assertion for split-payment case (Case 11):
        // Verify that the hypothetical payments 300 + 290 = 590 equals grandTotal.
        if (displayName.contains("Split payment")) {
            BigDecimal cash = bd("300");
            BigDecimal upi  = bd("290");
            assertThat(cash.add(upi))
                    .as("split payment sum should equal grandTotal")
                    .isEqualByComparingTo(output.grandTotal());
        }
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private static BigDecimal bd(String val) {
        return new BigDecimal(val);
    }
}
