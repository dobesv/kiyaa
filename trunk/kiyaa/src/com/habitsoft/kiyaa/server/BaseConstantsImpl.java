package com.habitsoft.kiyaa.server;

import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import com.google.gwt.i18n.client.ConstantsWithLookup;

public class BaseConstantsImpl implements ConstantsWithLookup {
    private final ResourceBundle res;
    
    public BaseConstantsImpl(ResourceBundle res) {
        this.res = res;
    }

    @Override
    public boolean getBoolean(String methodName) throws MissingResourceException {
        return Boolean.parseBoolean(getString(methodName));
    }

    @Override
    public double getDouble(String methodName) throws MissingResourceException {
        return Double.parseDouble(getString(methodName));
    }

    @Override
    public float getFloat(String methodName) throws MissingResourceException {
        return Float.parseFloat(getString(methodName));
    }

    @Override
    public int getInt(String methodName) throws MissingResourceException {
        return Integer.parseInt(getString(methodName));
    }

    @Override
    public Map<String, String> getMap(String methodName) throws MissingResourceException {
        return ServerLocalization.getMap(res, methodName);
    }

    @Override
    public String getString(String methodName) throws MissingResourceException {        
        return res.getString(methodName);
    }

    @Override
    public String[] getStringArray(String methodName) throws MissingResourceException {
        return ServerLocalization.getStringArray(res, methodName);
    }

}
