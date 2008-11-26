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
	
	public double getDoubleValue() {
	    return parser.parsePercentage(getText().trim());
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
}
