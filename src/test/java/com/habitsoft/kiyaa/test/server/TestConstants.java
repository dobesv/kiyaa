package com.habitsoft.kiyaa.test.server;

import java.util.Map;

import com.google.gwt.i18n.client.Constants;

public interface TestConstants extends Constants {
    int someInt();
    double someDouble();
    String someString();
    Map<String,String> someMap();
    String[] someStringArray();
    boolean someBoolean();
    
    @DefaultIntValue(5)
    int intWithDefault();
    @DefaultStringValue("Default")
    String stringWithDefault();
    @DefaultBooleanValue(true)
    boolean booleanWithDefault();
    @DefaultFloatValue(3.14f)
    float floatWithDefault();
}
