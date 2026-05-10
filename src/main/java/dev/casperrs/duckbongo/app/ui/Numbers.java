package dev.casperrs.duckbongo.app.ui;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class Numbers {

    private static final DecimalFormat FORMAT;
    static {
        DecimalFormatSymbols sym = new DecimalFormatSymbols(Locale.ROOT);
        sym.setGroupingSeparator('.');
        FORMAT = new DecimalFormat("#,###", sym);
    }

    public static String grouped(long n) { return FORMAT.format(n); }

    private Numbers() {}
}
