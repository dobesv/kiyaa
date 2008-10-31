package com.habitsoft.kiyaa.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.google.gwt.i18n.client.NumberFormat;

/**
 * Implementation of LocalizedParser for GWT on the client side.  This
 * will compile but not run if it is used on the server.
 * @author dobes
 */
public class ClientLocalizedParser implements LocalizedParser {
    public String formatCurrency(long amount, String currencyCode, boolean international) {
        return NumberFormat.getCurrencyFormat(currencyCode).format(amount);
    }

    public String formatDate(Date date, boolean shortFormat) {
        return SimpleDateFormat.getDateInstance(shortFormat?SimpleDateFormat.SHORT:SimpleDateFormat.MEDIUM).format(date);
    }

    public String formatDecimal(double val, boolean showSeperators) {
        return NumberFormat.getDecimalFormat().format(val);
    }

    public String formatPercentage(double value) {
        return NumberFormat.getPercentFormat().format(value);
    }

    public long parseCurrency(String text, String currencyCode) throws CurrencyParseException, DifferentCurrencyCodeProvided {
        double val = NumberFormat.getCurrencyFormat(currencyCode).parse(text);
        // TODO How many decimal places are there?
        int decimalPlaces = 2;
        return MathUtil.roundTo(val, decimalPlaces);
    }

    public Date parseDate(String dateString) throws DateParseException {
        try {
            return SimpleDateFormat.getDateInstance().parse(dateString);
        } catch (ParseException e) {
            throw new DateParseException(dateString+" doesn't follow a date format that I recognize", e); // TODO Should be this a localized message?
        }
    }

    public double parseDecimal(String val) throws NumberFormatException {
        return NumberFormat.getDecimalFormat().parse(val);
    }

    public double parsePercentage(String val) throws NumberFormatException {
        return NumberFormat.getPercentFormat().parse(val);
    }

}
