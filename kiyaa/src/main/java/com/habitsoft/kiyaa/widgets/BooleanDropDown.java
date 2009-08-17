package com.habitsoft.kiyaa.widgets;

import com.google.gwt.user.client.ui.ListBox;
import com.habitsoft.kiyaa.util.FocusGroup;

public class BooleanDropDown extends ListBox {
    FocusGroup focusGroup;
    
    public BooleanDropDown() {
        addItem("No");
        addItem("Yes");
    }
    
    public boolean getBooleanValue() {
        return getSelectedIndex() != 0;
    }
    
    public void setBooleanValue(boolean value) {
        setSelectedIndex(value?1:0);
    }
    
    public void setTrueLabel(String label) {
        setItemText(1, label);
    }
    
    public void setFalseLabel(String label) {
        setItemText(0, label);
    }
    
    public void setFocusGroup(FocusGroup group) {
        if(this.focusGroup != null)
            this.focusGroup.remove(this);
        this.focusGroup = group;
        if(group != null)
            group.add(this);
    }
    
    
}
