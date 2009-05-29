package com.habitsoft.kiyaa.util;

public class DifferentCurrencyCodeProvided extends CurrencyParseException {
    private static final long serialVersionUID = -813840981158435977L;
    
    final long amount;
    final String currencyCode;
    
    public DifferentCurrencyCodeProvided(long amount, String currencyCode) {
        super("Given amount specifies a different currency: "+currencyCode);
        this.amount = amount;
        this.currencyCode = currencyCode;
    }

    public long getAmount() {
        return amount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }
    
    
}
