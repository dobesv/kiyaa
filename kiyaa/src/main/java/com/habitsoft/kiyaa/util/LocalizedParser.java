/**
 * 
 */
package com.habitsoft.kiyaa.util;

import java.math.BigDecimal;
import java.util.Date;


/**
 * The purpose of this class is to allow some degree of code sharing
 * between client and server code.  Unfortunately for us, GWT's
 * parsing and formatting classes don't work on the server, and java's
 * parsing and formatting classes don't work on the client.  This
 * class allows client or server to produce an instance which can be
 * used by widgets and classes which need to do some localized parsing or
 * formatting and which are shared between client and server code.
 * @author dobes
 *
 */
public interface LocalizedParser {
    /**
     * Parse a date in as flexible a format as possible using the locale
     * of this parser.
     * 
     * On the client side, this is typically implemented using DateJs.
     * 
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
	 * 
	 * @throws DifferentCurrencyCodeProvided If the algorithm is smart enough to detect that the currency string given
	 *         is indicating a different currency than the one given, it'll throw this exception and you can do with
	 *         it as you please (you might use it to adjust the currency selection in the UI automatically).
	 */
	public long parseCurrency(String currencyString, String currencyCode) throws CurrencyParseException, DifferentCurrencyCodeProvided;
	public BigDecimal parseCurrencyBigDecimal(String currencyString, String currencyCode) throws CurrencyParseException, DifferentCurrencyCodeProvided;

	/**
	 * Convert a fixed-point currency amount into a string.  If international is true, the
	 * international currency symbol will be used; e.g. $5.25 becomes C$5.25 or US$5.25
	 * depending on the currency.  This allows currency values to be distinguished among
	 * countries using the same currency symbol.
	 * 
	 * The fixed-point representation of the currency value is the number of the smallest
	 * denomination of currency; e.g. the number of cents.  The parser will have to know
	 * how many subdivisions there are to a single unit.  For CAD, USD, and EUR (as well
	 * as many other currencies) there are two decimal places, meaning the number is
	 * divided by 100 and shown with 2 decimal places.  Other currencies have no
	 * subdivisions (INR for example) and can be formatted with no decimal places and
	 * do not have to be divided by anything.
	 * @param showGroupings TODO
	 */
	public String formatCurrency(long amount, String currencyCode, boolean international, boolean showGroupings);
	public String formatCurrency(BigDecimal amount, String currency, boolean international, boolean showGroupings);

	//public String formatCurrencyBigDecimal(BigDecimal amount, String currency,boolean international, boolean showGroupings);

	/**
	 * Parse a decimal number, supporting the locale-specific convention for decimal and
	 * thousand seperators.
	 */
	public double parseDecimal(String val) throws NumberFormatException;
	
	/**
	 * Format a double number.  If showGroupings is true, locale-specific seperators
	 * will be added to the string, e.g. 1,234,567.89 in most places, or 1.234.567,89 in
	 * European locales.
	 */
	public String formatDecimal(double val, boolean showGroupings);
	
	/**
	 * Parse a percentage; any "%" suffix should be stripped and the
	 * number divided by 100.0.  Like parseDecimal this should honor
	 * the locale-specific separators if they are present.
	 */
	public double parsePercentage(String val) throws NumberFormatException;
	
	/**
	 * Format a percentage.  Typically this means adding "%" to end
	 * and formatting as a decimal number.  This should round to 
	 * a reasonable number of decimal places, and ideally strip
	 * off any trailing zeroes and even the decimal point if 
	 * possible.
	 * 
	 * The algorithm should keep up to 4 decimal places, but should
	 * strip any trailings zeroes and remove the decimal seperator
	 * it it is unnecessary.
	 */
	public String formatPercentage(double value);

}