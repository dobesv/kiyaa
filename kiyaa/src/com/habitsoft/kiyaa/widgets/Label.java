package com.habitsoft.kiyaa.widgets;

import com.google.gwt.user.client.DOM;
import com.habitsoft.kiyaa.views.TakesElementName;


/**
 * A wrapper for a GWT Label that comes with a few tweaks for use in
 * templates.
 * 
 * Specifically, for performance it remembers the last value of text
 * and doesn't update the DOM if you set the same text twice.  This
 * makes more of a difference than you would expect.
 * 
 * Second, it implements TakesElementName so it can be used in a template
 * to create a <span .../> like:
 * 
 * <span kc="ui:label"> ... </span>
 * 
 */
public class Label extends com.google.gwt.user.client.ui.HTML implements TakesElementName {
	String oldText;
	public Label() {
		setText(null);
	}
	public Label(String text) {
		setText(text);
	}
	public Label(String tagName, String tagNamespace) {
	    super("span".equalsIgnoreCase(tagName)?DOM.createSpan():DOM.createDiv());
	}
	
	@Override
	public void setText(String text) {
		if(oldText != null && text != null && oldText.equals(text))
			return;
		if(text == null || text.trim().length()==0)
			super.setHTML("&nbsp;");
		else
			super.setText(text);
		oldText = text;
	}
}
