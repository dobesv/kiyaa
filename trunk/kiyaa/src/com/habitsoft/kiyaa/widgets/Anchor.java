package com.habitsoft.kiyaa.widgets;

import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.ButtonBase;
import com.google.gwt.user.client.ui.ClickListenerCollection;
import com.google.gwt.user.client.ui.SourcesClickEvents;
import com.habitsoft.kiyaa.util.FocusGroup;

/**
 * Anchors (the <a> tag) have many useful property not available to DIV's, like
 * cross-browser support for hover and linking to a file to download
 * without triggering a security warning.
 */
public class Anchor extends ButtonBase implements SourcesClickEvents {
	ClickListenerCollection clickListeners;
	AnchorElement anchor;
    FocusGroup focusGroup;
	
	public Anchor() {
		super(DOM.createAnchor());
		anchor = AnchorElement.as(getElement());
		sinkEvents(Event.ONCLICK);
		setHref("javascript:void(0)");
	}
	
	public void setHtml(String html) {
		anchor.setInnerHTML(html);
	}
	
	public String getHtml() {
		return anchor.getInnerHTML();
	}
	
	public void setHref(String href) {
		anchor.setHref(href);
	}
	
	public String getHref() {
		return anchor.getHref();
	}
	
	public void setTarget(String target) {
		anchor.setTarget(target);
	}
	
	public String getTarget() {
		return anchor.getTarget();
	}
	
	public void setName(String name) {
		anchor.setName(name);
	}
	
	public String getName() {
		return anchor.getName();
	}

	public void setActive(boolean active) {
		if(active) addStyleName("active");
		else removeStyleName("active");
	}
	
    public void setFocusGroup(FocusGroup group) {
        if(this.focusGroup != null)
            this.focusGroup.remove(this);
        this.focusGroup = group;
        if(group != null)
            group.add(this);
    }
    
	
}
