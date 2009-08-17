package com.habitsoft.kiyaa.test.server;

import java.util.Locale;

import junit.framework.TestCase;

import com.habitsoft.kiyaa.server.ServerLocalization;

public class TestServerLocalization extends TestCase {

    public void testMessages() throws Exception {
        TestMessages usa = ServerLocalization.createMessagesInstance(TestMessages.class, Locale.US);
        assertEquals("Hello, world!", usa.hello("world"));
        assertEquals("Hello 2 ... world?", usa.hello2("world"));
        assertEquals("Hello 3 -- world.", usa.hello3("world"));
        assertEquals("false,1,2.1,3.5", usa.typesTest(1, 2.1, 3.5f, false));
        assertEquals("Present!", usa.noDefault());
        TestMessages esp = ServerLocalization.createMessagesInstance(TestMessages.class, new Locale("es"));
        assertEquals("Hola, world!", esp.hello("world"));
        assertEquals("Mucho Gusto, world!", esp.hello2("world"));
        assertEquals("Hello 3 -- world.", esp.hello3("world"));
        assertEquals("false 3,5 2,1 1", esp.typesTest(1, 2.1, 3.5f, false));
        assertEquals("Here I am!", esp.noDefault());
        
        // What happens if the locale is missing?
        /* Seems like ResourceBundle falls back on en_US in that case
        try {
            TestMessages jap = ServerLocalization.createMessagesInstance(TestMessages.class, new Locale("jp"));
            assertEquals("whatever", jap.noDefault());
            fail("Should have thrown");
        } catch(MissingResourceException mre) {
            // Good!
        }
        */
    }
    
    public void testConstants() throws Exception {
        TestConstants usa = ServerLocalization.createConstantsInstance(TestConstants.class, Locale.US);
        assertEquals(2, usa.someInt());
        assertEquals(2.71828, usa.someDouble());
        assertEquals("Abracadabra", usa.someString());
        assertEquals(3, usa.someStringArray().length);
        assertEquals("A1", usa.someStringArray()[0]);
        assertEquals("A2", usa.someStringArray()[1]);
        assertEquals("A3,1", usa.someStringArray()[2]);
        assertEquals(3, usa.someMap().size());
        assertEquals("X", usa.someMap().get("K1"));
        assertEquals("Y", usa.someMap().get("K2"));
        assertEquals("Z", usa.someMap().get("K3"));
        assertEquals(5, usa.intWithDefault());
        assertEquals("Default", usa.stringWithDefault());
        assertEquals(true, usa.booleanWithDefault());
        assertEquals(3.14f, usa.floatWithDefault());
        
        TestConstants esp = ServerLocalization.createConstantsInstance(TestConstants.class, new Locale("es"));
        assertEquals(1, esp.someInt());
        assertEquals(3.1415927, esp.someDouble());
        assertEquals("Pescadora", esp.someString());
        assertEquals(3, esp.someMap().size());
        assertEquals("A", esp.someMap().get("K1"));
        assertEquals("B", esp.someMap().get("K2"));
        assertEquals("C", esp.someMap().get("K3"));
        assertEquals(3, esp.someStringArray().length);
        assertEquals("A1", esp.someStringArray()[0]);
        assertEquals("A2", esp.someStringArray()[1]);
        assertEquals("A3", esp.someStringArray()[2]);
        assertEquals(1, esp.intWithDefault());
        assertEquals("No puedo pasar", esp.stringWithDefault());
        assertEquals(false, esp.booleanWithDefault());
        assertEquals(1.5f, esp.floatWithDefault());
    }
}
