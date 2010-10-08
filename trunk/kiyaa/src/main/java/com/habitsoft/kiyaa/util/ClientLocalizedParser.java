package com.habitsoft.kiyaa.util;

import java.math.BigDecimal;
import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.CurrencyList;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.i18n.client.constants.NumberConstants;

/**
 * Implementation of LocalizedParser for GWT on the client side.  This
 * will compile but not run if it is used on the server.
 * @author dobes
 */
public class ClientLocalizedParser implements LocalizedParser {
    private static NumberConstants nc = LocaleInfo.getCurrentLocale().getNumberConstants();
    private static String stripTrailingZeroesRegex = nc.decimalSeparator().replaceAll("\\.", "\\\\.")+"?0*([^0-9]*)$";
    private DateTimeFormat dateFormat = DateTimeFormat.getMediumDateFormat();
    private DateTimeFormat shortDateFormat = DateTimeFormat.getShortDateFormat();

    public String formatCurrency(long amount, String currencyCode, boolean international, boolean showGroupings) {
        int decimalPlaces = getDecimalPlaces(currencyCode);
        final double doubleValue = MathUtil.fixedPointToDouble(amount, decimalPlaces);
        final NumberFormat format = getNumberFormat(currencyCode, international, decimalPlaces);
        return format.format(doubleValue);
    }   

	public String formatCurrency(BigDecimal amount, String currencyCode, boolean international, boolean showGroupings) {
		int decimalPlaces = getDecimalPlaces(currencyCode);
        final double doubleValue = amount.scaleByPowerOfTen(-decimalPlaces).doubleValue();
        final NumberFormat format = getNumberFormat(currencyCode, international, Math.max(decimalPlaces, amount.scale()));
        
        return format.format(doubleValue);
	}


	protected NumberFormat getNumberFormat(String currencyCode, boolean international, int decimalPlaces) {
        String fmt;
        switch(decimalPlaces) {
        case 0: fmt = international?"\u00a4\u00a4#,##0;\u00a4\u00a4-#,##0":"\u00a4#,##0;\u00a4-#,##0"; break;
        case 1: fmt = international?"\u00a4\u00a4#,##0.0;\u00a4\u00a4-#,##0.0":"\u00a4#,##0.0;\u00a4-#,##0.0"; break;        
        case 2: fmt = international?"\u00a4\u00a4#,##0.00;\u00a4\u00a4-#,##0.00":"\u00a4#,##0.00;\u00a4-#,##0.00"; break;
        case 3: fmt = international?"\u00a4\u00a4#,##0.000;\u00a4\u00a4-#,##0.000":"\u00a4#,##0.000;\u00a4-#,##0.000"; break;
        case 4: fmt = international?"\u00a4\u00a4#,##0.0000;\u00a4\u00a4-#,##0.0000":"\u00a4#,##0.0000;\u00a4-#,##0.0000"; break;        
        case 5: fmt = international?"\u00a4\u00a4#,##0.00000;\u00a4\u00a4-#,##0.00000":"\u00a4#,##0.00000;\u00a4-#,##0.00000"; break;
        case 6: fmt = international?"\u00a4\u00a4#,##0.000000;\u00a4\u00a4-#,##0.000000":"\u00a4#,##0.000000;\u00a4-#,##0.000000"; break;
        case 7: fmt = international?"\u00a4\u00a4#,##0.0000000;\u00a4\u00a4-#,##0.0000000":"\u00a4#,##0.0000000;\u00a4-#,##0.0000000"; break;        
        case 8: fmt = international?"\u00a4\u00a4#,##0.00000000;\u00a4\u00a4-#,##0.00000000":"\u00a4#,##0.00000000;\u00a4-#,##0.00000000"; break;
        case 9: fmt = international?"\u00a4\u00a4#,##0.000000000;\u00a4\u00a4-#,##0.000000000":"\u00a4#,##0.000000000;\u00a4-#,##0.000000000"; break;
        case 10: fmt = international?"\u00a4\u00a4#,##0.0000000000;\u00a4\u00a4-#,##0.0000000000":"\u00a4#,##0.0000000000;\u00a4-#,##0.0000000000"; break;
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
        return (shortFormat?shortDateFormat:dateFormat).format(date);
    }

    public String formatDecimal(double val, boolean showSeperators) {
        return NumberFormat.getDecimalFormat().format(val);
    }

    public String formatPercentage(double value) {
        final String str = NumberFormat.getFormat("#0.0000%").format(value);
        return str.replaceFirst(stripTrailingZeroesRegex, "$1"); 
    }

    public long parseCurrency(String text, String currencyCode) throws CurrencyParseException, DifferentCurrencyCodeProvided {
        double val;
        try {
            val = NumberFormat.getCurrencyFormat(currencyCode).parse(text);
        } catch (IllegalArgumentException badCurrencyCode) {
            throw new CurrencyParseException("Unknown currency "+currencyCode, badCurrencyCode);
        }
        int decimalPlaces = getDecimalPlaces(currencyCode);
        return MathUtil.roundToFixedPoint(val, decimalPlaces);
    }
    
    public BigDecimal parseCurrencyBigDecimal(String text, String currencyCode) throws CurrencyParseException, DifferentCurrencyCodeProvided {
        BigDecimal val = BigDecimal.ZERO;
        try {
            val = BigDecimal.valueOf(NumberFormat.getCurrencyFormat(currencyCode).parse(text));
        } catch (IllegalArgumentException badCurrencyCode) {
            throw new CurrencyParseException("Unknown currency "+currencyCode, badCurrencyCode);
        }
        int decimalPlaces = getDecimalPlaces(currencyCode);
        return val.scaleByPowerOfTen(decimalPlaces);
    }

    public Date parseDate(String dateString) throws DateParseException {
        try {
            return dateFormat.parse(dateString);
        } catch(Exception badDate1) {
            try {
                return shortDateFormat.parse(dateString);
            } catch(Exception badDate) {
                throw new DateParseException("Failed to parse date ("+badDate1.getLocalizedMessage()+")");
            }
        }
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

    public DateTimeFormat getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(DateTimeFormat dateFormat) {
        this.dateFormat = dateFormat;
    }

    public DateTimeFormat getShortDateFormat() {
        return shortDateFormat;
    }

    public void setShortDateFormat(DateTimeFormat shortDateFormat) {
        this.shortDateFormat = shortDateFormat;
    }

}
