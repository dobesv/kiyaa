package com.habitsoft.kiyaa.widgets;

import com.google.gwt.i18n.client.NumberFormat;


/**
 *
 * Input field for percentages.  The field automatically converts 1% to 0.01.
 * 
 */
public class PercentageTextBox extends TextBox {
	double cachedValue=0;
	String cachedText="0%";
	
	public double getValue() {
	    return NumberFormat.getPercentFormat().parse(getText().trim());
	}
	public void setValue(double value) {
		// Try to avoid expensive string operations
		if(value == cachedValue) {
			setText(cachedText);
			return;
		}
		
		String text = NumberFormat.getPercentFormat().format(value);
		cachedValue = value;
		cachedText = text;
		setText(text);
	}
}
