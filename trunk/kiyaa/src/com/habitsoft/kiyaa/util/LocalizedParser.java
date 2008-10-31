/**
 * 
 */
package com.habitsoft.kiyaa.util;

import java.util.Date;



public interface LocalizedParser {
    /**
     * Parse a date in as felxible a format as possible using the locale
     * of this parser.
     * @throws DateParseException If the date could not be recognized by the parser
     */
	public Date parseDate(String dateString) throws DateParseException;
	
	/**
	 * Format a date using a standard output format, based on the locale
	 * of this parser.
	 * @param date
	 * @param shortFormat  If true, use a short format like 12/5/2007 instead of Dec 5, 2007
	 * @return A string for that date
	 */
	public String formatDate(Date date, boolean shortFormat);
	
	/**
	 * Parse a currency value.  The currencyString may optionally start or
	 * end with currency-specific suffix and prefix as support by this
	 * parser.  The input may be using the international or non-international
	 * currency code; the parser should allow both, if it is capable of
	 * outputting the international code.
	 */
	public long parseCurrency(String currencyString, String currencyCode) throws CurrencyParseException, DifferentCurrencyCodeProvided;
	
	/**
	 * Convert a fixed-point currency amount into a string.  If international is true, the
	 * international currency symbol will be used; e.g. $5.25 becomes C$5.25 or US$5.25
	 * depending on the currency.  This allows currency values to be distinguished among
	 * countries using the same currency symbol.
	 */
	public String formatCurrency(long amount, String currencyCode, boolean international);
	
	/**
	 * Parse a decimal number, supporting the locale-specific convention for decimal and
	 * thousand seperators.
	 */
	public double parseDecimal(String val) throws NumberFormatException;
	
	/**
	 * Format a double number.  If showSeperators is true, locale-specific seperators
	 * will be added to the string, e.g. 1,234,567.89 in most places, or 1.234.567,89 in
	 * some European locales.
	 */
	public String formatDecimal(double val, boolean showSeperators);
	
	/**
	 * Parse a percentage; any "%" suffix should be stripped and the
	 * number divided by 100.0.  Like parseDecimal this will honor
	 * the locale-specific separators.
	 */
	public double parsePercentage(String val) throws NumberFormatException;
	
	/**
	 * Format a percentage.  Typically this means adding "%" to end
	 * and formatting as a decimal number.  This should round to 
	 * a reasonable number of decimal places, and ideally strip
	 * off any trailing zeroes and even the decimal point if 
	 * possible.
	 */
	public String formatPercentage(double value);
}