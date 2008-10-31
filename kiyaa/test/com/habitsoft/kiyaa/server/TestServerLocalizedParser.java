package com.habitsoft.kiyaa.server;

import java.util.Locale;

import junit.framework.TestCase;

import com.habitsoft.kiyaa.util.TestLocalizedParser;

public class TestServerLocalizedParser extends TestCase {

    public void testCurrencyFormatting() {
        ServerLocalizedParser parser = new ServerLocalizedParser(Locale.US);
        TestLocalizedParser.testUsCurrencyFormats(parser);
    }
}
