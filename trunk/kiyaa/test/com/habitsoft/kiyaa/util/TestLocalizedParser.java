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

    public static void testUsCurrencyFormats(LocalizedParser parser) {
        assertEquals("US$1.23", parser.formatCurrency(123, "USD", false, false));
        //assertEquals("CAD1.23", parser.formatCurrency(123, "CAD", false, false));
        //assertEquals("INR123", parser.formatCurrency(123, "INR", false, false));
    }

    private static void testInternationalCurrencyFormats(LocalizedParser parser) {
        assertEquals("USD1.23", parser.formatCurrency(123, "USD", true, false));
        //assertEquals("CAD1.23", parser.formatCurrency(123, "CAD", true, false));
        //assertEquals("INR123", parser.formatCurrency(123, "INR", true, false));
    }

    @Override
    public String getModuleName() {
        return "com.habitsoft.kiyaa.KiyaaTests";
    }
}
