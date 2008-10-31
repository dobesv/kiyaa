package com.habitsoft.kiyaa.widgets;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.Widget;
import com.habitsoft.kiyaa.util.FocusGroup;


public class TextBox extends com.google.gwt.user.client.ui.TextBox {
	boolean autoTrim=true;
	String innerHelp=null;
	FocusListener focusListener;
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
	}
	
	String currentText="";
	
	@Override
	public void setText(String text) {
		if(text != null && text.equals(currentText))
			return;
        currentText = text;
		if(innerHelp != null && (text == null || text.length()==0)) {
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
        String oldInnerHelp = this.innerHelp;
        this.innerHelp = innerHelp;
        if(innerHelp != null && oldInnerHelp == null) {
            if(focusListener == null) {
                focusListener = new FocusListener() {
                    public void onLostFocus(Widget sender) {
                        String text = getText();
                        if(text == null || text.length() == 0) {
                            showHelp();
                        }
                    }
                    public void onFocus(Widget sender) {
                        if(helpShowing) {
                            hideHelp();
                            setCursorPos(0);
                        }
                    }
                };                
            }
            addFocusListener(focusListener);
            if(helpShowing)
                super.setText(innerHelp);
            else if(currentText == null || currentText.length() == 0)
                showHelp();
        } else if(innerHelp == null && oldInnerHelp != null) {
            if(helpShowing)
                hideHelp();
            removeFocusListener(focusListener);
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
            && DOM.eventGetKeyCode(event) == KeyboardListener.KEY_ENTER) {
            if(DOM.eventGetAltKey(event) || DOM.eventGetMetaKey(event) || DOM.eventGetCtrlKey(event) || DOM.eventGetShiftKey(event))
                focusGroup.focusNextButton();
            else
                focusGroup.focusNext();
        }
    }

	
}
