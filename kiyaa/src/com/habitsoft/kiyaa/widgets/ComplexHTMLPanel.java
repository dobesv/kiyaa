package com.habitsoft.kiyaa.widgets;

import java.util.HashMap;
import java.util.NoSuchElementException;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.Widget;

public class ComplexHTMLPanel extends ComplexPanel {
    private static int sUid;

    protected HashMap elementsById;

    /**
     * A helper method for creating unique IDs for elements within dynamically- generated HTML. This
     * is important because no two elements in a document should have the same id.
     * 
     * @return a new unique identifier
     */
    public static String createUniqueId() {
        return "elt_" + (++sUid);
    }

    public ComplexHTMLPanel() {
        setElement(DOM.createDiv());
    }

    /**
     * Creates an HTML panel with the specified HTML contents. Any element within this HTML that has
     * a specified id can contain a child widget.
     * 
     * @param html
     *                the panel's HTML
     */
    public ComplexHTMLPanel(String html) {
        this();
        setTemplate(html);
    }

    protected void process(Element elem) {
        String elemId = DOM.getElementProperty(elem, "id");
        if (elemId != null  && elemId.length() > 0) {
            elementsById.put(elemId, elem);
            //DOM.setElementProperty(elem, "srcId", elemId);
            DOM.setElementProperty(elem, "id", createUniqueId());
        }
        /*
        if(DOM.getElementProperty(elem, "nodeName").equalsIgnoreCase("label")) {
        	String elemFor = DOM.getElementAttribute(elem, "for");
        	if(elemFor != null) {
        		Element targetElem = (Element)elementsById.get(elemFor);
        		if(targetElem != null) {
        			DOM.setElementAttribute(elem, "for", DOM.getElementAttribute(targetElem, "id"));
        		}
        	}
        }
        */
        for (Element child = DOM.getFirstChild(elem); child != null; child = DOM.getNextSibling(child)) {
            process(child);
        }
    }

    /**
     * Set the template being used by this panel
     * 
     * @param html
     */
    public void setTemplate(String html) {
        elementsById = new HashMap();
        DOM.setInnerHTML(getElement(), html);
        process(getElement());
    }

    public void setDomTemplate(Element element) {
        elementsById = new HashMap();
        while(getElement().hasChildNodes()) {
        	getElement().removeChild(getElement().getLastChild());
        }
        getElement().appendChild(element);
        //setElement(element);
        process(element);
    }
    /**
     * Adds a child widget to the panel, contained within the HTML element specified by a given id.
     * 
     * @param widget
     *                the widget to be added
     * @param id
     *                the id of the element within which it will be contained
     */
    public void add(Widget widget, String id) {
        Element elem = getElementById(id);
        if (elem == null) {
            throw new NoSuchElementException(id);
        }

        super.add(widget, elem);
    }

    /**
     * Adds a child widget to the panel, replacing the HTML element specified by a given id.
     * 
     * @param widget
     *                the widget to be added
     * @param id
     *                the id of the element within which it will be contained
     */
    public void replace(Widget widget, String id) {
        if (widget == null)
            throw new NullPointerException();
        if (id == null)
            throw new NullPointerException();
        Element elem = getElementById(id);
        if (elem == null)
            throw new IllegalArgumentException("Id " + id + " not found in the template");
        Element parent = DOM.getParent(elem);
        DOM.insertBefore(parent, widget.getElement(), elem);
        DOM.removeChild(parent, elem);
        elementsById.put(id, widget.getElement());
        
        // The following code is copied from super.add()
        try {
        	widget.removeFromParent();
        } catch(Throwable t) {
        	GWT.log("Failed to remove widget from previous parent", t);
        }
        getChildren().add(widget);
        adopt(widget);
    }

   
    public void setText(String id, String text) {
    	Element elem = getElementById(id);
        if (elem == null)
            throw new IllegalArgumentException("Id " + id + " not found in the template");
        DOM.setInnerText(elem, text);
    }
    
    public void setHTML(String id, String html) {
    	Element elem = getElementById(id);
        if (elem == null)
            throw new IllegalArgumentException("Id " + id + " not found in the template");
        DOM.setInnerHTML(elem, html);
    }
    
    public Element getElementById(String id) {
        return (Element) elementsById.get(id);
    }

    public HashMap getElementsById() {
        return elementsById;
    }

    public void setElementsById(HashMap elementsById) {
        this.elementsById = elementsById;
    }
}
