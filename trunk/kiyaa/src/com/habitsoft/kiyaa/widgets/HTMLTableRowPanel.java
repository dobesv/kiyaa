package com.habitsoft.kiyaa.widgets;

import java.util.Iterator;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.ClickListenerCollection;
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.FocusListenerCollection;
import com.google.gwt.user.client.ui.HTMLTable;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.KeyboardListenerCollection;
import com.google.gwt.user.client.ui.MouseListener;
import com.google.gwt.user.client.ui.MouseListenerCollection;
import com.google.gwt.user.client.ui.MouseWheelListener;
import com.google.gwt.user.client.ui.MouseWheelListenerCollection;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SourcesClickEvents;
import com.google.gwt.user.client.ui.SourcesFocusEvents;
import com.google.gwt.user.client.ui.SourcesKeyboardEvents;
import com.google.gwt.user.client.ui.SourcesMouseEvents;
import com.google.gwt.user.client.ui.SourcesMouseWheelEvents;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import com.habitsoft.kiyaa.util.HoverStyleHandler;

public class HTMLTableRowPanel extends Panel implements SourcesClickEvents,
		SourcesMouseEvents, SourcesFocusEvents, SourcesKeyboardEvents,
		SourcesMouseWheelEvents {
	protected final HTMLTable table;
	protected int row;
	protected int column;
	private ClickListenerCollection clickListeners;
    private ClickListenerCollection contextMenuListeners;
	private FocusListenerCollection focusListeners;
	private KeyboardListenerCollection keyboardListeners;
	private MouseListenerCollection mouseListeners;
	private MouseWheelListenerCollection mouseWheelListeners;

	public HTMLTableRowPanel(HTMLTable table, int row, String stylePrimaryName, boolean selectable, HoverStyleHandler.Group hoverGroup) {
		this.table = table;
		this.column = 0;
		this.row = row;
		setStyleName(stylePrimaryName + ((row & 1) == 1 ? " even":" odd"));
		
		if(selectable) {
			addMouseListener(new HoverStyleHandler(this, hoverGroup));
		}
	}

	@Override
    public void onAttach() {
        DOM.setEventListener(getElement(), this);
        int events = Event.FOCUSEVENTS | Event.KEYEVENTS | Event.ONCLICK
            | Event.MOUSEEVENTS | Event.ONMOUSEWHEEL;
        if(contextMenuListeners != null) events |= Event.ONCONTEXTMENU;
        sinkEvents(events);
	}
	
	@Override
	public void addStyleDependentName(String styleSuffix) {
		table.getRowFormatter().addStyleName(row, table.getRowFormatter().getStylePrimaryName(row)+"-"+styleSuffix);
	}
	@Override
	public void removeStyleDependentName(String styleSuffix) {
		table.getRowFormatter().removeStyleName(row, table.getRowFormatter().getStylePrimaryName(row)+"-"+styleSuffix);
	}
	@Override
	public String getStylePrimaryName() {
		return table.getRowFormatter().getStylePrimaryName(row);
	}
	@Override
	public void setStylePrimaryName(String style) {
		table.getRowFormatter().setStylePrimaryName(row, style);
	}
	@Override
	public void setStyleName(String style) {
		table.getRowFormatter().setStyleName(row, style);
	}
    @Override
	public String getStyleName() {
    	return table.getRowFormatter().getStyleName(row);
    }
	@Override
	public void addStyleName(String style) {
		table.getRowFormatter().addStyleName(row, style);
	}

	@Override
	public void removeStyleName(String style) {
		table.getRowFormatter().removeStyleName(row, style);
	}

	@Override
	public void add(Widget widget) {
		table.setWidget(row, column, widget);
		column++;
	}

	@Override
	public Element getElement() {
		return table.getRowFormatter().getElement(row);
	}

	public Iterator iterator() {
		return new Iterator() {
			int currentColumn = 0;
			Widget lastResult;

			public void remove() {
				if (lastResult != null)
					table.remove(lastResult);
			}

			public Object next() {
				lastResult = table.getWidget(row, currentColumn);
				currentColumn++;
				return lastResult;
			}

			public boolean hasNext() {
				return currentColumn < DOM.getChildCount(getElement());
			}

		};
	}

	@Override
	public boolean remove(Widget w) {
		return table.remove(w);
	}

	public void addClickListener(ClickListener listener) {
		if (clickListeners == null) {
			clickListeners = new ClickListenerCollection();
		}
		clickListeners.add(listener);
	}

	public void addFocusListener(FocusListener listener) {
		if (focusListeners == null) {
			focusListeners = new FocusListenerCollection();
		}
		focusListeners.add(listener);
	}

	public void addKeyboardListener(KeyboardListener listener) {
		if (keyboardListeners == null) {
			keyboardListeners = new KeyboardListenerCollection();
		}
		keyboardListeners.add(listener);
	}

	public void addMouseListener(MouseListener listener) {
		if (mouseListeners == null) {
			mouseListeners = new MouseListenerCollection();
		}
		mouseListeners.add(listener);
	}

	public void addMouseWheelListener(MouseWheelListener listener) {
		if (mouseWheelListeners == null) {
			mouseWheelListeners = new MouseWheelListenerCollection();
		}
		mouseWheelListeners.add(listener);
	}

	@Override
	public void onBrowserEvent(Event event) {
		switch (DOM.eventGetType(event)) {
		case Event.ONCLICK:
			if (clickListeners != null) {
				clickListeners.fireClick(this);
			}
			break;

		case Event.ONCONTEXTMENU:
		    if(contextMenuListeners != null) {
		        contextMenuListeners.fireClick(this);
		    }
		    break;
		case Event.ONMOUSEDOWN:
		case Event.ONMOUSEUP:
		case Event.ONMOUSEMOVE:
		case Event.ONMOUSEOVER:
		case Event.ONMOUSEOUT:
			if (mouseListeners != null) {
				mouseListeners.fireMouseEvent(this, event);
			}
			break;

		case Event.ONMOUSEWHEEL:
			if (mouseWheelListeners != null) {
				mouseWheelListeners.fireMouseWheelEvent(this, event);
			}
			break;

		case Event.ONBLUR:
		case Event.ONFOCUS:
			if (focusListeners != null) {
				focusListeners.fireFocusEvent(this, event);
			}
			break;

		case Event.ONKEYDOWN:
		case Event.ONKEYUP:
		case Event.ONKEYPRESS:
			if (keyboardListeners != null) {
				keyboardListeners.fireKeyboardEvent(this, event);
			}
			break;
		}
	}

	public void removeClickListener(ClickListener listener) {
		if (clickListeners != null) {
			clickListeners.remove(listener);
		}
	}

	public void removeFocusListener(FocusListener listener) {
		if (focusListeners != null) {
			focusListeners.remove(listener);
		}
	}

	public void removeKeyboardListener(KeyboardListener listener) {
		if (keyboardListeners != null) {
			keyboardListeners.remove(listener);
		}
	}

	public void removeMouseListener(MouseListener listener) {
		if (mouseListeners != null) {
			mouseListeners.remove(listener);
		}
	}

	public void removeMouseWheelListener(MouseWheelListener listener) {
		if (mouseWheelListeners != null) {
			mouseWheelListeners.remove(listener);
		}
	}

	public int getRow() {
		return row;
	}

	/**
	 * Called when an earlier row has been deleted, and this row's element is now
	 * in a different position.
	 */
	public void setRow(int row) {
		this.row = row;
		UIObject.setStyleName(getElement(), "even", (row & 1) == 1);
		UIObject.setStyleName(getElement(), "odd", (row & 1) == 0);
	}

	public void addContextMenuListener(ClickListener listener) {
	    if(contextMenuListeners == null) {
	        sinkEvents(Event.ONCONTEXTMENU);
	        contextMenuListeners = new ClickListenerCollection();
	    }
	    contextMenuListeners.add(listener);
	}
	public void removeContextMenuListener(ClickListener listener) {
	    if(contextMenuListeners != null) {
	        contextMenuListeners.remove(listener);
	        if(contextMenuListeners.isEmpty()) {
	            contextMenuListeners = null;
	            unsinkEvents(Event.ONCONTEXTMENU);
	        }
	    }
	}
}
