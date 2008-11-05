package com.habitsoft.kiyaa.test.server;

import com.google.gwt.i18n.client.LocalizableResource.Key;
import com.google.gwt.i18n.client.Messages.DefaultMessage;

public interface TestMessages {
    @DefaultMessage("Hello, {0}!")
    String hello(String who);
    
    @Key("prop.hello")
    String hello2(String who);
    
    String hello3(String who);
    
    String typesTest(int a, double b, float c, boolean d);
    
    String noDefault();
}
