package com.habitsoft.kiyaa.widgets;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Label;

public class Title extends Label {
	public static final String STYLE_NAME="title";
	public Title(String tagName) {
		setElement(DOM.createElement(tagName));
		setStyleName(STYLE_NAME);
		setWordWrap(true);
	}

	public Title(String tagName, String text) {
		this(tagName);
		setText(text);
	}
}
