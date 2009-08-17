package com.habitsoft.kiyaa.widgets;

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.habitsoft.kiyaa.util.FocusGroup;


public class TextBox extends com.google.gwt.user.client.ui.TextBox implements FocusHandler, BlurHandler {
	boolean autoTrim=true;
	String innerHelp=null;
	boolean helpShowing=false;
	FocusGroup focusGroup;
	boolean focusNextOnEnter=true;
	
	public boolean isAutoTrim() {
		return autoTrim;
	}

	public void setAutoTrim(boolean autoTrim) {
		this.autoTrim = autoTrim;
	}

	public TextBox() {
		super();
		setStylePrimaryName("ui-textbox");
        addFocusHandler(this);
        addBlurHandler(this);
	}
	
	String currentText="";
    private boolean focused;
	
	@Override
	public void setText(String text) {
		if(text != null && text.equals(currentText))
			return;
        currentText = text;
		if(innerHelp != null && (text == null || text.length()==0) && !focused) {
		    showHelp();
		} else if(helpShowing) {
		    hideHelp();
		} else {
            super.setText(text);
		}
	}
	
	private void hideHelp() {
        removeStyleName("help-showing");
        helpShowing = false;
        super.setText(currentText);
    }

    private void showHelp() {
	    if(!helpShowing) {
	        addStyleName("help-showing");
	        helpShowing = true;
	        super.setText(innerHelp);
	    }
    }

    @Override
	public String getText() {
        if(helpShowing)
            return currentText;
		currentText = super.getText();
		if(autoTrim) currentText = currentText.trim();
		return currentText;
	}

    public String getInnerHelp() {
        return innerHelp;
    }

    public void setInnerHelp(String innerHelp) {
        if(this.innerHelp == innerHelp || (innerHelp != null && innerHelp.equals(this.innerHelp)))
            return; // No change
        String oldInnerHelp = this.innerHelp;
        this.innerHelp = innerHelp;
        if(!focused) {
            if(innerHelp != null) {
                if(helpShowing)
                    super.setText(innerHelp);
                else if(currentText == null || currentText.length() == 0)
                    showHelp();
            } else if(oldInnerHelp != null) {
                if(helpShowing)
                    hideHelp();
            }
        }
    }
	
	public void setFocusGroup(FocusGroup group) {
	    if(this.focusGroup != null)
	        this.focusGroup.remove(this);
	    this.focusGroup = group;
	    if(group != null) {
	        sinkEvents(Event.ONKEYPRESS);
	        group.add(this);
	    }
	}
	
	@Override
	public void onBrowserEvent(Event event) {
        super.onBrowserEvent(event);
        
        if(focusNextOnEnter 
            && focusGroup != null
            && DOM.eventGetType(event) == Event.ONKEYPRESS
            && event.getKeyCode() == KeyCodes.KEY_ENTER) {
            if(DOM.eventGetAltKey(event) || DOM.eventGetMetaKey(event) || DOM.eventGetCtrlKey(event) || DOM.eventGetShiftKey(event))
                focusGroup.focusNextButton();
            else
                focusGroup.focusNext();
        }
    }

    @Override
    public void onFocus(FocusEvent event) {        
        focused = true;
        if(helpShowing) {
            hideHelp();
            setCursorPos(0);
        }
    }

    @Override
    public void onBlur(BlurEvent event) {
        focused = false;
        String text = getText();
        if(text == null || text.length() == 0) {
            showHelp();
        }
    }
}
