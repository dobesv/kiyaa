package com.habitsoft.kiyaa.util;

import com.google.gwt.junit.client.GWTTestCase;

public class TestLocalizedParser extends GWTTestCase {

    /**
     * Test whether the currency formattings done by GWT are what we expect.
     * 
     * The answer in general is NO.  GWT doesn't help us format or parse
     * currencies.  We'll have to implement our own currencies database,
     * which I leave as an exercise to the reader.
     */
    public void testCurrencyFormatting() throws Exception {
        final ClientLocalizedParser parser = new ClientLocalizedParser();
        testUsCurrencyFormats(parser);
        testInternationalCurrencyFormats(parser);
    }

    public static void testUsCurrencyFormats(LocalizedParser parser) throws DifferentCurrencyCodeProvided, CurrencyParseException {
        assertEquals("US$1.23", parser.formatCurrency(123, "USD", false, false));
        assertEquals(-123, parser.parseCurrency("US$-1.23", "USD"));
        assertEquals(123, parser.parseCurrency("US$1.23", "USD"));
        /* None of these are supported - the parser is very strict
        assertEquals(123, parser.parseCurrency("$-1.23", "USD"));
        assertEquals(123, parser.parseCurrency("$1.23", "USD"));
        assertEquals(123, parser.parseCurrency("1.23", "USD"));
        assertEquals(120, parser.parseCurrency("1.2", "USD"));
        assertEquals(120, parser.parseCurrency("$1.2", "USD"));
        assertEquals(23, parser.parseCurrency("$0.23", "USD"));
        assertEquals(23, parser.parseCurrency("$.23", "USD"));
        assertEquals(23, parser.parseCurrency(".23", "USD"));
        assertEquals(20, parser.parseCurrency(".2", "USD"));
        assertEquals(0, parser.parseCurrency("0", "USD"));
        assertEquals(0, parser.parseCurrency("$0", "USD"));
        assertEquals(0, parser.parseCurrency("$0.00", "USD"));
        assertEquals(0, parser.parseCurrency("$0.0", "USD"));
        */
        //assertEquals("CAD1.23", parser.formatCurrency(123, "CAD", false, false));
        //assertEquals("INR123", parser.formatCurrency(123, "INR", false, false));
    }

    private static void testInternationalCurrencyFormats(LocalizedParser parser) {
        assertEquals("USD1.23", parser.formatCurrency(123, "USD", true, false));
        //assertEquals("CAD1.23", parser.formatCurrency(123, "CAD", true, false));
        //assertEquals("INR123", parser.formatCurrency(123, "INR", true, false));
    }

    
    public void testPercentageFormatting() throws Exception {
        final ClientLocalizedParser parser = new ClientLocalizedParser();
        testPercentages(parser);
    }

    public static void testPercentages(LocalizedParser parser) {
        assertEquals("0%", parser.formatPercentage(0));
        assertEquals("5%", parser.formatPercentage(0.05));
        assertEquals("5.01%", parser.formatPercentage(0.0501));
        assertEquals("5.01%", parser.formatPercentage(0.0501));
        assertEquals("5.5%", parser.formatPercentage(0.055));
        assertEquals("5.5%", parser.formatPercentage(5.5/100.0));
        assertEquals("5%", parser.formatPercentage(5.0/100.0));
        assertEquals("-5.5%", parser.formatPercentage(-0.055));
        assertEquals("-5.5%", parser.formatPercentage(-5.5/100.0));
        assertEquals("-5.55%", parser.formatPercentage(-5.55/100.0));
        assertEquals("-5.555%", parser.formatPercentage(-5.555/100.0));
        assertEquals("-5.5555%", parser.formatPercentage(-5.5555/100.0));
        assertEquals("-5.5556%", parser.formatPercentage(-5.55555/100.0));
        assertEquals("-5.5556%", parser.formatPercentage(-5.555555/100.0));
        assertEquals("5.5556%", parser.formatPercentage(5.555555/100.0));
        
        assertEquals(0.055556, parser.parsePercentage("5.5556%"));
        assertEquals(0.055556, parser.parsePercentage("5.5556"));
        assertEquals(5.0, parser.parsePercentage("500%"));
        assertEquals(1.0, parser.parsePercentage("100%"));
        assertEquals(1.01, parser.parsePercentage("101%"));
        assertEquals(0.01, parser.parsePercentage("1%"));
        assertEquals(0.01, parser.parsePercentage("1.000%"));
        assertEquals(1.01, parser.parsePercentage("101.000%"));
        assertEquals(0.0, parser.parsePercentage("0%"));
        assertEquals(1.01, parser.parsePercentage("101"));
        assertEquals(0.01, parser.parsePercentage("1"));
        assertEquals(0.01, parser.parsePercentage("1.000"));
        assertEquals(1.01, parser.parsePercentage("101.000"));
        assertEquals(0.0, parser.parsePercentage("0"));
    }
    
    @Override
    public String getModuleName() {
        return "com.habitsoft.kiyaa.KiyaaTests";
    }
}
