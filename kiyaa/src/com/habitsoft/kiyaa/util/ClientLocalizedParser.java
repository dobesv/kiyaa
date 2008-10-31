package com.habitsoft.kiyaa.util;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.i18n.client.constants.NumberConstants;
import com.google.gwt.i18n.client.impl.CurrencyList;

/**
 * Implementation of LocalizedParser for GWT on the client side.  This
 * will compile but not run if it is used on the server.
 * @author dobes
 */
public class ClientLocalizedParser implements LocalizedParser {
    private static NumberConstants nc = GWT.create(NumberConstants.class);
    private static String stripTrailingZeroesRegex = nc.decimalSeparator().replaceAll("\\.", "\\\\.")+"?0*([^0-9]*)$";

    public String formatCurrency(long amount, String currencyCode, boolean international, boolean showGroupings) {
        int decimalPlaces = getDecimalPlaces(currencyCode);
        final double doubleValue = MathUtil.fixedPointToDouble(amount, decimalPlaces);
        final NumberFormat format = getNumberFormat(currencyCode, international, decimalPlaces);
        return format.format(doubleValue);
    }

    protected NumberFormat getNumberFormat(String currencyCode, boolean international, int decimalPlaces) {
        String fmt;
        switch(decimalPlaces) {
        case 0: fmt = international?"\u00a4\u00a4#,##0;\u00a4\u00a4-#,##0":"\u00a4#,##0;\u00a4-#,##0"; break;
        case 2: fmt = international?"\u00a4\u00a4#,##0.00;\u00a4\u00a4-#,##0.00":"\u00a4#,##0.00;\u00a4-#,##0.00"; break;        
        default: throw new IllegalArgumentException("Currency "+currencyCode+" not supported because it has "+decimalPlaces+" decimal places!");
        }
        final NumberFormat format = NumberFormat.getFormat(fmt, currencyCode);
        return format;
    }

    protected int getDecimalPlaces(String currencyCode) {
        int decimalPlaces = CurrencyList.get().lookup(currencyCode).getDefaultFractionDigits();
        System.out.println("Decimal places for "+currencyCode+" in "+LocaleInfo.getCurrentLocale().getLocaleName()+" is "+decimalPlaces);
        return decimalPlaces;
    }

    public String formatDate(Date date, boolean shortFormat) {
        return (shortFormat?DateTimeFormat.getShortDateFormat():DateTimeFormat.getMediumDateFormat()).format(date);
    }

    public String formatDecimal(double val, boolean showSeperators) {
        return NumberFormat.getDecimalFormat().format(val);
    }

    public String formatPercentage(double value) {
        final String str = NumberFormat.getFormat("#0.0000%").format(value);
        return str.replaceFirst(stripTrailingZeroesRegex, "$1"); 
    }

    public long parseCurrency(String text, String currencyCode) throws CurrencyParseException, DifferentCurrencyCodeProvided {
        double val = NumberFormat.getCurrencyFormat(currencyCode).parse(text);
        int decimalPlaces = getDecimalPlaces(currencyCode);
        return MathUtil.roundToFixedPoint(val, decimalPlaces);
    }

    public Date parseDate(String dateString) throws DateParseException {
        return DateTimeFormat.getShortDateFormat().parse(dateString);
    }

    public double parseDecimal(String val) throws NumberFormatException {
        return NumberFormat.getDecimalFormat().parse(val);
    }

    public double parsePercentage(String val) throws NumberFormatException {
        try {
            return NumberFormat.getPercentFormat().parse(val)/100.0;
        } catch(NumberFormatException nfe) {
            return parseDecimal(val)/100.0;
        }
    }

}
