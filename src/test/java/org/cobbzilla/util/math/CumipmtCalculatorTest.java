package org.cobbzilla.util.math;

import lombok.AllArgsConstructor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.cobbzilla.util.math.CumipmtCalculator.cumipmt;

@RunWith(Parameterized.class)
@AllArgsConstructor
public class CumipmtCalculatorTest {
    private static final double PRECISSION = 0.000001;
    private static final double MAX_RATE_PERCENT = 17;
    private static final HashMap<Double, Double> RATE_PERCENT_MAPPING = new HashMap<Double, Double>() {{
        put(999.99, 16.90);
        put(1499.99, 16.85);
        put(1999.99, 16.35);
        put(2499.99, 15.85);
        put(2999.99, 15.35);
        put(3499.99, 14.85);
        put(3999.99, 14.35);
        put(4499.99, 13.85);
        put(4999.99, 12.10);
        put(9999.99, 11.30);
        put(14999.99, 10.00);
        put(19999.99, 9.30);
        put(24999.99, 8.30);
        put(29999.99, 7.80);
    }};

    private double periodCount;
    private double premium;
    private double downPercent;
    private int paymentsCount;
    private double expectedValue;

    @Parameterized.Parameters
    public static Collection paramsAndExpectedVal() {
        return Arrays.asList(new Object[][]{
                {12, 1100, 12, 10, -77.014302},
                {12, 2450, 12, 10, -164.843936},
                {12, 875, 12, 10, -61.261377},
                {12, 3850, 12, 10, -242.903207},
                {12, 6575, 12, 10, -325.710132},
                {12, 1800, 12, 10, -124.888837},
                {12, 11000, 12, 10, -544.914289},
                {12, 1136.36, 12, 10, -79.082437},
        });
    }

    @Test public void testCumipmt() throws Exception {
        double financed = premium * (1 - downPercent / 100);

        double ratePercent = MAX_RATE_PERCENT;
        for (Map.Entry<Double, Double> item : RATE_PERCENT_MAPPING.entrySet()) {
            if (ratePercent > item.getValue() && financed > item.getKey()) {
                ratePercent = item.getValue();
            }
        }

        double c1 = cumipmt(ratePercent / periodCount / 100, paymentsCount, financed, 1, paymentsCount, true);
        assertEquals(expectedValue, c1, PRECISSION);
    }
}
