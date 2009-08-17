package com.dynarch.jscalendar.client;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ChangeListenerCollection;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.habitsoft.kiyaa.util.DateParseException;
import com.habitsoft.kiyaa.util.LocalizedParser;

/**
 * jscalendar, an LGPL javascript calendar widget.
 * 
 * Docs: http://www.dynarch.com/demos/jscalendar/doc/html/reference.html
 * 
 * Be sure to load one of the calendar style sheets when using the calendar;
 * for example, you could add this to your *.gwt.xml for your app:
 * 
 * <stylesheet src="calendar-blue.css"/>
 * 
 */
public class JsCalendar extends TextBox {
	boolean created = false;
	boolean hideOnSelect = false;
	boolean showing = false;
	boolean showOnFocus = false;
	
	LocalizedParser dateParser;

	@SuppressWarnings("unused")
	private JavaScriptObject cal;
	
	Timer parseTimer = new Timer() {
		@Override
		public void run() {
			try {
				parseText();
			} catch (DateParseException caught) {
				// TODO Show an error outline
				GWT.log("Date parsing of '"+getText()+"' failed", caught);
			}
		}
	};

	/**
	 * This is the entry point method.
	 */
	public JsCalendar() {
		cal = nativeInit();
		
		addFocusListener(new FocusListener() {
			public void onLostFocus(Widget sender) {
				boolean wasShowing = showing;
				hideCalendar();
				// We re-parse the text when we lose focus just in case the
				// user PASTEd into the text box, and we didn't notice (paste 
				// operations can't reliably be detected, sadly)
				try {
					parseText();
				} catch (DateParseException caught) {
					// TODO Show an error outline
					GWT.log("Date parsing of '"+getText()+"' failed", caught);
				}
				if(wasShowing || getText().length() > 0)
					reformatDate();
			}
		
			public void onFocus(Widget sender) {
				if(showOnFocus)
					showCalendar();
			}
		});
		addClickListener(new ClickListener() {
			public void onClick(Widget sender) {
				showCalendar();
			}
		});
		addKeyboardListener(new KeyboardListenerAdapter() {
			@Override
			public void onKeyPress(Widget sender, char keyCode, int modifiers) {
				if(keyCode == KeyboardListener.KEY_TAB || keyCode == KeyboardListener.KEY_ESCAPE) {
					hideCalendar();
				} else if(keyCode == KeyboardListener.KEY_ENTER) {
					hideCalendar();
				} else if(Character.isLetterOrDigit(keyCode) || keyCode == KeyboardListener.KEY_BACKSPACE || keyCode == KeyboardListener.KEY_DELETE) {
					parseTimer.schedule(100);
				}
			}
			
			@Override
			public void onKeyDown(Widget sender, char keyCode, int modifiers) {
				// Cursor keys open the calendar
				if(keyCode == KEY_DOWN || keyCode == KEY_UP) {
					if(!showing)
						showCalendar();
				}
			}
		});
	}
	
	native JavaScriptObject nativeInit() /*-{
		var java_this = this;
		function onSelect(cal, date) {
			java_this.@com.dynarch.jscalendar.client.JsCalendar::onSelect(Ljava/lang/String;)(date);
		}
		function onClose(cal) {
			java_this.@com.dynarch.jscalendar.client.JsCalendar::onClose()();
		}
		return new $wnd.Calendar(0, new $wnd.Date(), onSelect, onClose);
	}-*/;
	
	public native void setWeekNumbers(boolean weekNumbers) /*-{
		this.@com.dynarch.jscalendar.client.JsCalendar::cal.weekNumbers = weekNumbers;
	}-*/;
	
	public native void setShowsTime(boolean showsTime) /*-{
    	this.@com.dynarch.jscalendar.client.JsCalendar::cal.showsTime = showsTime;
    }-*/;
	
	/**
	 * Alias for setShowsTime since I always think its showTime
	 */
	public void setShowTime(boolean showsTime) {
		setShowsTime(showsTime);
	}
	
	public native void setTime24(boolean time24) /*-{
    	this.@com.dynarch.jscalendar.client.JsCalendar::cal.time24 = time24;
    }-*/;
	
	/**
	 * This function configures the format in which the calendar reports the date to your ``onSelect'' handler. Call it like this:
	 * 
     * calendar.setDateFormat("%y/%m/%d");
     *  
     * As you can see, it receives only one parameter, the required format. The magic characters are the following:
     * %a  abbreviated weekday name  
     *  %A  full weekday name  
     *  %b  abbreviated month name  
     *  %B  full month name  
     *  %C  century number  
     *  %d  the day of the month ( 00 .. 31 )  
     *  %e  the day of the month ( 0 .. 31 )  
     *  %H  hour ( 00 .. 23 )  
     *  %I  hour ( 01 .. 12 )  
     *  %j  day of the year ( 000 .. 366 )  
     *  %k  hour ( 0 .. 23 )  
     *  %l  hour ( 1 .. 12 )  
     *  %m  month ( 01 .. 12 )  
     *  %M  minute ( 00 .. 59 )  
     *  %n  a newline character  
     *  %p  ``PM'' or ``AM''  
     *  %P  ``pm'' or ``am''  
     *  %S  second ( 00 .. 59 )  
     *  %s  number of seconds since Epoch (since Jan 01 1970 00:00:00 UTC)  
     *  %t  a tab character  
     *  %U, %W, %V  the week number 
     *  %u  the day of the week ( 1 .. 7, 1 = MON ) 
     *  %w  the day of the week ( 0 .. 6, 0 = SUN ) 
     *  %y  year without the century ( 00 .. 99 ) 
     *  %Y  year including the century ( ex. 1979 ) 
     *  %%  a literal % character  
     *  
     *  There are more algorithms for computing the week number. All three specifiers currently implement the same one, as defined by ISO 8601: ``the week 01 is the week that has the Thursday in the current year, which is equivalent to the week that contains the fourth day of January. Weeks start on Monday.''
     *  
	 */
	public native void setDateFormat(String format) /*-{
		this.@com.dynarch.jscalendar.client.JsCalendar::cal.setDateFormat(format);
	}-*/;
	
	/**
	 * Called when a date has been selected
	 */
	protected void onSelect(String date) {
		if(dateClicked()) {
			setText(date);
    		if(changeListeners != null)
    			changeListeners.fireChange(this);
    		if(hideOnSelect) {
    			hideCalendar();
    		}
		}
	}
	
	private ChangeListenerCollection changeListeners;
	@Override
	public void addChangeListener(ChangeListener listener) {
		super.addChangeListener(listener);
		if (changeListeners == null) {
			changeListeners = new ChangeListenerCollection();
		}
		changeListeners.add(listener);
	}

	@Override
	public void removeChangeListener(ChangeListener listener) {
		super.removeChangeListener(listener);
		if (changeListeners != null) {
			changeListeners.remove(listener);
		}
	}
	
	/**
	 * Called when the calendar is closed
	 */
	protected void onClose() {
		hideCalendar();
	}
	
	/**
	 * Called to determined whether a given date can be selected
	 */
	protected String getDateStatus(Date date) {
		// TODO ...
		return "";
	}
	
	
	protected String getDateStatusMillis(long millis) {
		return getDateStatus(new Date(millis));
	}
	
	/**
	 * Return true if a date was clicked, false otherwise
	 * 
	 * Use to check during an onClose or other event whether or
	 * not the user actualy picked a date.
	 */
	public native boolean dateClicked() /*-{
		return this.@com.dynarch.jscalendar.client.JsCalendar::cal.dateClicked;
	}-*/;
	
	/**
	 * Create the calendar's elements.
	 */
	private native void nativeCreateCalendar() /*-{
		this.@com.dynarch.jscalendar.client.JsCalendar::cal.create();
	}-*/;
	
	private void createCalendar() {
		created = true;
		nativeCreateCalendar();
	}
//	private native void nativeShowCalendar(int x, int y) /*-{
//	this.@com.dynarch.jscalendar.client.JsCalendar::cal.showAt(x, y);
//}-*/;
	private native void nativeShowCalendar(Element element) /*-{
    	this.@com.dynarch.jscalendar.client.JsCalendar::cal.showAtElement(element);
    }-*/;
//	private native void nativeShowCalendar(Element element, String position) /*-{
//    	this.@com.dynarch.jscalendar.client.JsCalendar::cal.showAtElement(element, position);
//    }-*/;
	
	private native void nativeHideCalendar() /*-{
    	this.@com.dynarch.jscalendar.client.JsCalendar::cal.hide();
    }-*/;
	
	@Override
	protected void onAttach() {
		super.onAttach();
	}
	
	@Override
	public void setVisible(boolean visible) {
		if(!visible && created) nativeHideCalendar();
		super.setVisible(visible);
	}
	
	public Date getDate() {
		String text = getText();
		if(text.equals(""))
			return null;
		final long dateMillis = (long)getDateMillis();
		return new Date(dateMillis);
	}
	
	/**
	 * Note: this can only be called AFTER createCalendar has been called.
	 * @param millis
	 */
	private native void nativeSetDate(double millis) /*-{
		var cal = this.@com.dynarch.jscalendar.client.JsCalendar::cal
		if(this.@com.dynarch.jscalendar.client.JsCalendar::created) {
			cal.setDate(new $wnd.Date(millis));
		} else {
			cal.dateStr = cal.date = new $wnd.Date(millis);
		}
	}-*/;
	
	native double getDateMillis() /*-{
		return this.@com.dynarch.jscalendar.client.JsCalendar::cal.date.getTime();
	}-*/;
	
	native void nativeSetDateString(String date) /*-{
    	this.@com.dynarch.jscalendar.client.JsCalendar::cal.parseDate(date);
    }-*/;
    
	public void setDateString(String dateString) throws DateParseException {
		if(!getText().equals(dateString)) {
			setText(dateString);
			parseText();
		}
	}

	private void parseText() throws DateParseException {
		if(!created)
			createCalendar();
		String text = getText();
		if(text.length() == 0)
			return;
		if(dateParser != null) {
			nativeSetDate(dateParser.parseDate(text).getTime());
		}
		else nativeSetDateString(text);
	}
	
	public native String getDateString() /*-{
		if(this.@com.dynarch.jscalendar.client.JsCalendar::cal == undefined) alert('missing cal');
		if(this.@com.dynarch.jscalendar.client.JsCalendar::cal.date == undefined) alert('missing date');
		if(this.@com.dynarch.jscalendar.client.JsCalendar::cal.date.print == undefined) alert('missing print method');
		var str = this.@com.dynarch.jscalendar.client.JsCalendar::cal.date.print(this.@com.dynarch.jscalendar.client.JsCalendar::cal.dateFormat);
		if(str == undefined) alert('print method returns undefined for '+this.@com.dynarch.jscalendar.client.JsCalendar::cal.date+' and format '+this.@com.dynarch.jscalendar.client.JsCalendar::cal.dateFormat);
	}-*/;
	
	public void setDate(Date date) {
		if(date == null) setText(""); 
		else {
			nativeSetDate(date.getTime());
			reformatDate();
		}
	}

	private void reformatDate() {
		setText(getDateString());
	}

	public boolean isCreated() {
		return created;
	}

	public void setCreated(boolean created) {
		this.created = created;
	}

	public LocalizedParser getDateParser() {
		return dateParser;
	}

	public void setDateParser(LocalizedParser dateParser) {
		this.dateParser = dateParser;
	}

	public boolean isHideOnSelect() {
		return hideOnSelect;
	}

	public void setHideOnSelect(boolean hideOnSelect) {
		this.hideOnSelect = hideOnSelect;
	}
	
	public void showCalendar() {
		if(!showing) {
			if(!created) {
				createCalendar();
			}
			showing = true;
			nativeShowCalendar(getElement());
		}
	}
	public void hideCalendar() {
		if(showing) {
			showing = false;
			nativeHideCalendar();
		}
	}

	public boolean isShowOnFocus() {
		return showOnFocus;
	}

	public void setShowOnFocus(boolean showOnFocus) {
		this.showOnFocus = showOnFocus;
	}
}
