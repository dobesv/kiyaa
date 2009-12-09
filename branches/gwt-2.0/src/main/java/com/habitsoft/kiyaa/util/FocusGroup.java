package com.habitsoft.kiyaa.util;

import java.util.ArrayList;
import java.util.TreeMap;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.FocusListenerCollection;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HasFocus;
import com.google.gwt.user.client.ui.SourcesFocusEvents;
import com.google.gwt.user.client.ui.TextBoxBase;
import com.google.gwt.user.client.ui.Widget;

/**
 * Wrap a group of widgets which can receive focus.
 * 
 * This serves two purposes above and beyond the built-in focus support:
 * 
 * 1. We can programmatically advance focus within the group by calling focusNext()
 * 2. We can check whether any member of the group has focus, i.e. to check whether a
 *    particular panel currently has focus.
 *    
 */
public class FocusGroup extends ArrayList<Widget> implements FocusListener, SourcesFocusEvents {
    private static final long serialVersionUID = 1L;
    
    Widget currentFocus;
    Widget lostFocus; // When lostFocus != null, currentFocus should be null, and vice versa
    final Widget root;
    TreeMap<String,Widget> sorted;
    
    public FocusGroup(Widget root) {
        this.root = root;
    }

    Timer focusEventTimer = new Timer() {
        @Override
        public void run() {
            sendFocusEvent();
        }
    };
    final FocusListenerCollection focusListeners = new FocusListenerCollection();

    public Widget first() {
        updateSorted();
        for(Widget w : sorted.values()) {
            if(canFocus(false, w))
                return w;
        }
        return null;
    }

    private void updateSorted() {
        if(sorted == null) {
            sorted = new TreeMap<String,Widget>();
            for(Widget w : this) {
                sorted.put(getDomPositionString(w.getElement(), root.getElement()), w);
            }
        }
    }
    
    public void focus(Widget target) {
        if(target == null)
            return;
        try {
	        ((HasFocus)target).setFocus(true);
	        if(target instanceof TextBoxBase) {
	            ((TextBoxBase)target).selectAll();
	        }
        } catch(Exception e) {
        	GWT.log("Error setting focus to "+target, e);
        }
    }
    public void focus() {
        focus(first());
    }

    static void getDomPositionString(Element elt, Element rootElt, StringBuffer sb) {
        final Element parentElement = elt.getParentElement();
        if(parentElement != null && parentElement != rootElt)
            getDomPositionString(parentElement, rootElt, sb);
        char idx=' ';
        Node prev = elt;
        while((prev = prev.getPreviousSibling()) != null) idx++;
        sb.append(idx);
    }
    static String getDomPositionString(Element elt, Element rootElt) {
        StringBuffer sb = new StringBuffer();
        getDomPositionString(elt, rootElt, sb);
        return sb.toString();
    }
    private Widget getNext(boolean buttonOnly) {
        if(isEmpty())
            return null;
        updateSorted();
        String curkey = currentFocus==null?sorted.firstKey():getDomPositionString(currentFocus.getElement(), root.getElement());
        for(Widget w : sorted.tailMap(curkey).values()) {
            if(w == currentFocus)
                continue;
            if(canFocus(buttonOnly, w))
                return w;
        }
        for(Widget w : sorted.headMap(curkey).values()) {
            if(canFocus(buttonOnly, w))
                return w;
        }
        return null;
    }

    private boolean canFocus(boolean buttonOnly, Widget w) {
        return w.isAttached() 
            && w.isVisible()
            && (!(w instanceof FocusWidget) || ((FocusWidget)w).isEnabled())
            && (!buttonOnly 
                || w.getClass().getName().matches(".*(Button|Anchor).*"));
    }
    
    public void focusNext() {
        DeferredCommand.addCommand(new Command() {
            public void execute() {
                focus(getNext(false));
            }
        });
    }
    
    /**
     * Focus the next member of the group whose class name contains "Anchor" or "Button".
     */
    public void focusNextButton() {
        DeferredCommand.addCommand(new Command() {
            public void execute() {
                focus(getNext(true));
            }
        });
    }
    /* Not sure if this is useful...
    public void clickNextButton() {
        DeferredCommand.addCommand(new Command() {
            public void execute() {
                HasFocus next = getNext(true);
                if(next instanceof com.google.gwt.user.client.ui.Button) {
                    ((com.google.gwt.user.client.ui.Button)next).click();
                } else if(next instanceof Button) {
                    ((Button)next).click();
                }
            }
        });
    }
    */
    
    public boolean isFocused() {
        return currentFocus != null;
    }
    
    /**
     * Add a new focus child; it will be inserted according to
     * tab index, or insertion order if tab indexes are equal
     * or -1.
     */
    @Override
    public boolean add(Widget w) {
        if(contains(w))
            return false;
        addListener(w);
        sorted = null; // Need to rebuild the sorted list
        return super.add(w);
    }
    
    private void addListener(Widget e) {
        ((HasFocus)e).addFocusListener(this);
    }
    
    public void remove(Widget widget) {
        if(super.remove(widget)) {
            removeListener(widget);
            sorted = null;
        }
    }
    
    @Override
    public void clear() {
        sorted = null;
        for(Widget widget : this) {
            removeListener(widget);
        }
        super.clear();
    }
    
    private Widget removeListener(Widget remove) {
        ((HasFocus)remove).removeFocusListener(this);
        return remove;
    }

    public void onFocus(Widget sender) {        
        currentFocus = sender;
        lostFocus = null;
        focusEventTimer.schedule(100);
    }

    public void onLostFocus(Widget sender) {
        if(currentFocus == sender) {
            if(lostFocus == null)
                lostFocus = currentFocus;
            currentFocus = null;
        }
        focusEventTimer.schedule(100);
    }
    
    protected void sendFocusEvent() {
        for(FocusListener listener : focusListeners) {
            if(currentFocus != null) {
                listener.onFocus((Widget)currentFocus);
            } else if(lostFocus != null) {
                listener.onLostFocus((Widget)lostFocus);
            }
        }
        
        // Clear this one out after sending the event
        lostFocus = null;
    }
    
    public void addFocusListener(FocusListener listener) {
        focusListeners.add(listener);
    }
    
    public void removeFocusListener(FocusListener listener) {
        focusListeners.remove(listener);
    }
}
