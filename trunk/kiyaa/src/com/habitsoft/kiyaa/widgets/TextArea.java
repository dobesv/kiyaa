package com.habitsoft.kiyaa.widgets;

import com.habitsoft.kiyaa.util.FocusGroup;

public class TextArea extends com.google.gwt.user.client.ui.TextArea {
	String oldText;
    FocusGroup focusGroup;
    int maxLength;
    
	@Override
	public void setText(String text) {
		if(oldText != null && text != null && oldText.equals(text))
			return;
		super.setText(text);
		oldText = text;
	}

	@Override
	public String getText() {
	    String text = super.getText();
	    if(maxLength != 0 && text.length() > maxLength)
	        text = text.substring(0, maxLength);
        return (oldText = text);
	}
    public void setFocusGroup(FocusGroup group) {
        if(this.focusGroup != null)
            this.focusGroup.remove(this);
        this.focusGroup = group;
        if(group != null)
            group.add(this);
    }
    
    /**
     * Gets the maximum allowable length of the text area.
     * 
     * @return the maximum length, in characters
     */
    public int getMaxLength() {
      return maxLength;
    }

    /**
     * Sets the maximum allowable length of the text area.
     * 
     * @param length the maximum length, in characters
     */
    public void setMaxLength(int length) {
        maxLength = length;
    }
    
}
