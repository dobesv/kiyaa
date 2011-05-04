package com.habitsoft.kiyaa.server;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;

import com.habitsoft.kiyaa.util.CurrencyParseException;
import com.habitsoft.kiyaa.util.DateParseException;
import com.habitsoft.kiyaa.util.DifferentCurrencyCodeProvided;
import com.habitsoft.kiyaa.util.LocalizedParser;
import com.habitsoft.kiyaa.util.MathUtil;

public class ServerLocalizedParser implements LocalizedParser {

    final Locale locale;
    
    public ServerLocalizedParser() {
        this(Locale.getDefault());
    }

    public ServerLocalizedParser(Locale locale) {
        this.locale = locale;
    }

    @Override
    public String formatCurrency(long amount, String currencyCode, boolean international, boolean showGroupings) {
        final Currency currency = Currency.getInstance(currencyCode);
        final int decimalPlaces = getDecimalPlaces(currency);
        final NumberFormat numberFormat = getNumberFormat(currency, international, showGroupings);
        return numberFormat.format(MathUtil.fixedPointToDouble(amount, decimalPlaces));
    }

    @Override
	public String formatCurrency(BigDecimal amount, String currencyCode, boolean international, boolean showGroupings) {
    	final Currency currency = Currency.getInstance(currencyCode);
        final int decimalPlaces = getDecimalPlaces(currency);
        final NumberFormat numberFormat = getNumberFormat(currency, international, showGroupings);
        numberFormat.setMaximumFractionDigits(amount.scale()+decimalPlaces);
        return numberFormat.format(amount.scaleByPowerOfTen(-decimalPlaces));
	}

	protected int getDecimalPlaces(final Currency currency) {
        return currency.getDefaultFractionDigits();
    }

    protected NumberFormat getNumberFormat(final Currency currency, boolean international, boolean showGroupings) {
        final NumberFormat numberFormat = NumberFormat.getCurrencyInstance(locale);        
        numberFormat.setCurrency(currency);
        numberFormat.setGroupingUsed(showGroupings);
        numberFormat.setMaximumFractionDigits(getDecimalPlaces(currency));
        if(getDecimalPlaces(currency) == 0) {
            numberFormat.setParseIntegerOnly(true);
        }
        if(international && numberFormat instanceof DecimalFormat) {
            DecimalFormat df = (DecimalFormat) numberFormat;
            df.applyPattern(df.toPattern().replaceAll("\u00A4+", "\u00A4\u00A4")); // TODO This seems to be less efficient than I'd like
        }
        return numberFormat;
    }

    @Override
    public String formatDate(Date date, boolean shortFormat) {
        return SimpleDateFormat.getDateInstance(shortFormat?SimpleDateFormat.SHORT:SimpleDateFormat.MEDIUM).format(date);
    }

    @Override
    public String formatDecimal(double val, boolean groupings) {
        return DecimalFormat.getNumberInstance(locale).format(val);
    }

    @Override
    public String formatPercentage(double value) {
        final DecimalFormat df = new DecimalFormat("#0.0000%");
        df.setMinimumFractionDigits(0);
        df.setMaximumFractionDigits(4);
        df.setDecimalSeparatorAlwaysShown(false);
        return df.format(value);
    }

    @Override
    public long parseCurrency(String currencyString, String currencyCode) throws CurrencyParseException, DifferentCurrencyCodeProvided {
        final NumberFormat numberFormat = NumberFormat.getCurrencyInstance(locale);
        final Currency currency = Currency.getInstance(currencyCode);
        numberFormat.setCurrency(currency);
        double val;
        try {
            // TODO Missing support for DifferentCurrencyCodeProvided and for internation currency formats
            val = numberFormat.parse(currencyString).doubleValue();
        } catch (ParseException e) {
            throw new CurrencyParseException(e);
        }
        return MathUtil.roundToFixedPoint(val, getDecimalPlaces(currency));
    }
    
    @Override
    public BigDecimal parseCurrencyBigDecimal(String currencyString, String currencyCode) throws CurrencyParseException, DifferentCurrencyCodeProvided {
        final NumberFormat numberFormat = NumberFormat.getCurrencyInstance(locale);
        final Currency currency = Currency.getInstance(currencyCode);
        numberFormat.setCurrency(currency);
        BigDecimal val = BigDecimal.ZERO;
        try {
            // TODO Missing support for DifferentCurrencyCodeProvided and for internation currency formats
            val = BigDecimal.valueOf(numberFormat.parse(currencyString).doubleValue());
        } catch (ParseException e) {
            throw new CurrencyParseException(e);
        }
        return val;
    }

    @Override
    public Date parseDate(String dateString) throws DateParseException {
        try {
            return SimpleDateFormat.getDateInstance().parse(dateString);
        } catch (ParseException e) {
            throw new DateParseException(dateString+" doesn't follow a date format that I recognize", e); // TODO Should be this a localized message?
        }
    }

    @Override
    public double parseDecimal(String val) throws NumberFormatException {
        try {
            return NumberFormat.getNumberInstance(locale).parse(val).doubleValue();
        } catch (ParseException e) {
            // TODO This error should be localized based on our own locale instead of the default locale
            throw new NumberFormatException(e.getLocalizedMessage());
        }
    }

    @Override
    public double parsePercentage(String val) throws NumberFormatException {
        try {
            return NumberFormat.getPercentInstance(locale).parse(val).doubleValue();
        } catch (ParseException e) {
            return parseDecimal(val)/100.0;
        }
    }
}
