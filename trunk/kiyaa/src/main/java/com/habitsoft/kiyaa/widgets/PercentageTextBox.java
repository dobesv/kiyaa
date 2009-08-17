package com.habitsoft.kiyaa.widgets;

import com.habitsoft.kiyaa.util.ClientLocalizedParser;
import com.habitsoft.kiyaa.util.LocalizedParser;


/**
 *
 * Input field for percentages.  The field automatically converts 1% to 0.01.
 * 
 */
public class PercentageTextBox extends TextBox {
	double cachedValue=0;
	String cachedText="0%";
	LocalizedParser parser = new ClientLocalizedParser();
	double defaultValue=0;
	
	public double getDoubleValue() {
	    String text = getText().trim();
	    if(text.length() == 0) return defaultValue;
        try {
            return parser.parsePercentage(text);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
	}
	public void setDoubleValue(double value) {
		// Try to avoid expensive string operations
		if(value == cachedValue) {
			setText(cachedText);
			return;
		}
		
		String text = parser.formatPercentage(value);
		cachedValue = value;
		cachedText = text;
		setText(text);
	}
    public double getDefaultValue() {
        return defaultValue;
    }
    public void setDefaultValue(double defaultValue) {
        this.defaultValue = defaultValue;
    }
    public LocalizedParser getParser() {
        return parser;
    }
    public void setParser(LocalizedParser parser) {
        this.parser = parser;
    }
}
