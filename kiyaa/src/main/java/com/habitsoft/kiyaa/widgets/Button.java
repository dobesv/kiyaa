package com.habitsoft.kiyaa.widgets;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.HasHTML;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.SourcesClickEvents;
import com.google.gwt.user.client.ui.SourcesFocusEvents;
import com.google.gwt.user.client.ui.SourcesKeyboardEvents;
import com.habitsoft.kiyaa.util.FocusGroup;

/**
 * This expects you to set the style name, or provide a CSS style named "button" which makes this
 * look like a button.
 * 
 * For convenience, an outer SPAN element is provided; this makes it easier to create a flexible
 * button effect. For an example, see the "Wii Buttons":
 * 
 * http://www.hedgerwow.com/360/dhtml/css-round-button/demo.php
 * 
 * The HTML of the button would look like:
 * 
 * <span class='button'><button .../></span>
 * 
 */
public class Button extends ComplexPanel implements HasText, HasHTML, SourcesFocusEvents, SourcesClickEvents, SourcesKeyboardEvents {
	public static String STYLE_NAME = "button";
	Element span;
	com.google.gwt.user.client.ui.Button button;
	boolean active;
	FocusGroup focusGroup;
	
	public Button() {
		span = DOM.createSpan();
		setElement(span);
		
		button = new com.google.gwt.user.client.ui.Button();
		add(button, span);
		setStyleName(STYLE_NAME);
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		if(active != this.active) {
			this.active = active;
			if(active) addStyleName("active");
			else removeStyleName("active");
		}
	}

	public void addClickListener(ClickListener listener) {
		button.addClickListener(listener);
	}

	public void addFocusListener(FocusListener listener) {
		button.addFocusListener(listener);
	}

	public void addKeyboardListener(KeyboardListener listener) {
		button.addKeyboardListener(listener);
	}

	public void click() {
		button.click();
	}

	public String getHTML() {
		return button.getHTML();
	}

	public int getTabIndex() {
		return button.getTabIndex();
	}

	public String getText() {
		return button.getText();
	}

	@Override
	public String getTitle() {
		return button.getTitle();
	}

	public boolean isEnabled() {
		return button.isEnabled();
	}

	public void removeClickListener(ClickListener listener) {
		button.removeClickListener(listener);
	}

	public void removeFocusListener(FocusListener listener) {
		button.removeFocusListener(listener);
	}

	public void removeKeyboardListener(KeyboardListener listener) {
		button.removeKeyboardListener(listener);
	}

	public void setAccessKey(char key) {
		button.setAccessKey(key);
	}

	public void setEnabled(boolean enabled) {
		button.setEnabled(enabled);
	}

	public void setFocus(boolean focused) {
		button.setFocus(focused);
	}

	public void setHTML(String html) {
		button.setHTML(html);
	}

	public void setTabIndex(int index) {
		button.setTabIndex(index);
	}

	public void setText(String text) {
		button.setText(text);
	}

	@Override
	public void setTitle(String title) {
		button.setTitle(title);
	}
	
    public void setFocusGroup(FocusGroup group) {
        if(this.focusGroup != null)
            this.focusGroup.remove(button);
        this.focusGroup = group;
        if(group != null)
            group.add(button);
    }
	
    @Override
    protected void onEnsureDebugId(String baseID) {
        button.ensureDebugId(baseID);
    }
}
