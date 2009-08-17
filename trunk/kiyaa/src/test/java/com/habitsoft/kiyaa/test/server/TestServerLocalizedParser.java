package com.habitsoft.kiyaa.test.server;

import java.util.Locale;

import junit.framework.TestCase;

import com.habitsoft.kiyaa.server.ServerLocalizedParser;
import com.habitsoft.kiyaa.test.util.GwtTestLocalizedParser;

public class TestServerLocalizedParser extends TestCase {

    public void testCurrencyFormatting() throws Exception {
        ServerLocalizedParser parser = new ServerLocalizedParser(Locale.US);
        GwtTestLocalizedParser.assertEquals("$1.23", parser.formatCurrency(123, "USD", false, false));
        GwtTestLocalizedParser.assertEquals(-123, parser.parseCurrency("($1.23)", "USD"));
        GwtTestLocalizedParser.assertEquals(123, parser.parseCurrency("$1.23", "USD"));
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
    
    public void testPercentages() {
        GwtTestLocalizedParser.testPercentages(new ServerLocalizedParser(Locale.US));
    }
}
