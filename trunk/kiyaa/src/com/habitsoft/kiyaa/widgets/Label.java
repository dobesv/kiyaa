package com.habitsoft.kiyaa.widgets;



public class Label extends com.google.gwt.user.client.ui.HTML {
	String oldText;
	public Label() {
		setText(null);
	}
	public Label(String text) {
		setText(text);
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
