package com.habitsoft.kiyaa.widgets;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.habitsoft.kiyaa.util.FocusGroup;


public class PasswordTextBox extends com.google.gwt.user.client.ui.PasswordTextBox {
    FocusListener focusListener;
    boolean helpShowing=false;
    FocusGroup focusGroup;
    boolean focusNextOnEnter=true;
    
    public PasswordTextBox() {
        super();
        setStylePrimaryName("ui-textbox password");
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
