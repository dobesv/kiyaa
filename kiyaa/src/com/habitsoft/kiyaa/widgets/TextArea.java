package com.habitsoft.kiyaa.widgets;

import com.habitsoft.kiyaa.util.FocusGroup;

public class TextArea extends com.google.gwt.user.client.ui.TextArea {
	String oldText;
    FocusGroup focusGroup;
    
	@Override
	public void setText(String text) {
		if(oldText != null && text != null && oldText.equals(text))
			return;
		super.setText(text);
		oldText = text;
	}

	@Override
	public String getText() {
	    return (oldText = super.getText());
	}
    public void setFocusGroup(FocusGroup group) {
        if(this.focusGroup != null)
            this.focusGroup.remove(this);
        this.focusGroup = group;
        if(group != null)
            group.add(this);
    }
    
}
