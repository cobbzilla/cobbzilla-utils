package org.cobbzilla.util.math;

import static java.lang.Math.pow;

public class CumipmtCalculator {

    public static double cumipmt(double ratePerPeriod, int paymentsCount, double financed, int startPeriod,
                                 int endPeriod, boolean isDueOnPeriodEnd) {
        if (startPeriod < 1 || endPeriod < startPeriod || ratePerPeriod <= 0 || endPeriod > paymentsCount  ||
                paymentsCount <= 0 || financed <= 0) throw new IllegalArgumentException();

        double payment = 0;

        if (startPeriod == 1) {
            if (isDueOnPeriodEnd) payment = -financed;
            startPeriod++;
        }

        double rmz = -financed * ratePerPeriod / (1 - 1 / pow(1 + ratePerPeriod, paymentsCount));
        if (!isDueOnPeriodEnd) rmz /= 1 + ratePerPeriod;

        for (int i=startPeriod; i<=endPeriod; i++) {
            payment += getPeriodPaymentAddOn(ratePerPeriod, i, rmz, financed, isDueOnPeriodEnd);
        }

        return payment * ratePerPeriod;
    }

    private static double getPeriodPaymentAddOn(double ratePerPeriod, double periodIndex, double rmz, double financed,
                                                boolean isDueOnPeriodEnd) {
        double term = pow(1 + ratePerPeriod, periodIndex - (isDueOnPeriodEnd ? 1 : 2));

        double addOn = rmz * (term - 1) / ratePerPeriod;
        if (!isDueOnPeriodEnd) addOn *= 1 + ratePerPeriod;

        double res = -(financed * term + addOn);
        if (!isDueOnPeriodEnd) res -= rmz;

        return res;
    }
}
