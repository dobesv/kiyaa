package com.habitsoft.kiyaa.widgets;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ChangeListenerCollection;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.HasFocus;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.PopupListener;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SourcesChangeEvents;
import com.google.gwt.user.client.ui.SourcesClickEvents;
import com.google.gwt.user.client.ui.SourcesFocusEvents;
import com.google.gwt.user.client.ui.SourcesKeyboardEvents;
import com.google.gwt.user.client.ui.Widget;
import com.habitsoft.kiyaa.util.ClientLocalizedParser;
import com.habitsoft.kiyaa.util.DateParseException;
import com.habitsoft.kiyaa.util.FocusGroup;
import com.habitsoft.kiyaa.util.LocalizedParserWithDateJs;

public class DatePicker extends FlowPanel implements SourcesChangeEvents, SourcesClickEvents, SourcesKeyboardEvents, SourcesFocusEvents, HasText, HasFocus {

	Calendar calendar = new Calendar();
	PopupPanel popup = new PopupPanel(true, false);
	boolean showing;
	ClientLocalizedParser dateParser;
	boolean hideOnSelect=true;
	boolean showOnFocus;
	DateTimeFormat dateFormat = DateTimeFormat.getMediumDateFormat();
	TextBox textbox = new TextBox();
	Label calendarIcon = new Label();
	boolean optional=true;
	
	Timer parseTimer = new Timer() {
		@Override
		public void run() {
			try {
				parseText();
			} catch (DateParseException caught) {
				// TODO Show an error outline
				GWT.log("Date parsing of '"+textbox.getText()+"' failed", caught);
			}
		}
	};
	Timer hideTimer = new Timer() {
		@Override
		public void run() {
			hideCalendar();
		}
	};
	
	public DatePicker() {
		setStyleName("date-picker");
		textbox.setStyleName("date-picker");
		// Disable autocomplete on our custom combobox, since autocomplete interferes with our use of the cursor keys!
		DOM.setElementProperty(textbox.getElement(), "autocomplete", "off");
		popup.addAutoHidePartner(getElement());
		popup.setWidget(calendar);
		popup.addCloseHandler(new CloseHandler<PopupPanel>() {
			@Override
			public void onClose(CloseEvent<PopupPanel> event) {
				showing = false;
			}
		});
		calendar.addCloseHandler(new CloseHandler<Calendar>() {
			public void onClose(CloseEvent<Calendar> event) {
				hideCalendar();
			}
		});
		calendar.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				textbox.setFocus(true);
				reformatDate();
				if(changeListeners != null)
					changeListeners.fireChange(DatePicker.this);
				if(hideOnSelect)
					hideCalendar();
			}
		});
		textbox.addFocusHandler(new FocusHandler() {
			
			@Override
			public void onFocus(FocusEvent event) {
				if (showOnFocus)
					showCalendar();
			}
		});
		textbox.addBlurHandler(new BlurHandler() {
			
			@Override
			public void onBlur(BlurEvent event) {
				boolean wasShowing = showing;
				//hideTimer.schedule(200);
				
				// We re-parse the text when we lose focus just in case the
				// user PASTEd into the text box, and we didn't notice (paste
				// operations can't reliably be detected, sadly)
				try {
					parseText();
				} catch (DateParseException caught) {
					// TODO Show an error outline
					GWT.log("Date parsing of '" + textbox.getText() + "' failed", caught);
				}
				if (wasShowing || textbox.getText().length() > 0)
					reformatDate();
			}
		});
		calendarIcon.setStyleName("date-picker-icon");
		calendarIcon.addClickHandler(new ClickHandler() {
			
			@Override
			public void onClick(ClickEvent event) {
				showCalendar();
			}
		});
		textbox.addKeyboardListener(new KeyboardListenerAdapter() {
			@Override
			public void onKeyPress(Widget sender, char keyCode, int modifiers) {
				if (keyCode == KEY_TAB || keyCode == KEY_ESCAPE) {
					hideCalendar();
				} else if (keyCode == KEY_ENTER) {
					try {
						parseText();
					} catch (DateParseException caught) {
						GWT.log("Date parsing of '" + textbox.getText() + "' failed", caught);
					}
					reformatDate();
					hideCalendar();
					if(changeListeners != null)
					    changeListeners.fireChange(DatePicker.this);
				} else if (Character.isLetterOrDigit(keyCode) || keyCode == KEY_BACKSPACE
					|| keyCode == KEY_DELETE) {
					parseTimer.schedule(100);
				}
			}

			@Override
			public void onKeyDown(Widget sender, char keyCode, int modifiers) {
				// Cursor keys open the calendar
				if (keyCode == KEY_TAB || keyCode == KEY_ESCAPE) {
					hideCalendar();
				} else if (keyCode == KEY_DOWN || keyCode == KEY_UP) {
					if (!showing)
						showCalendar();
					if(keyCode == KEY_DOWN)
						calendar.gotoNextWeek();
					else
						calendar.gotoPrevWeek();
					reformatDate();
				} else if(keyCode == KEY_LEFT || keyCode == KEY_RIGHT) {
					if(showing) {
						if(keyCode == KEY_RIGHT)
							calendar.gotoNextDay();
						else
							calendar.gotoPrevDay();
						reformatDate();
					}
				}
			}
		});
		setDateParser(new LocalizedParserWithDateJs());
		add(textbox);
		add(calendarIcon);
	}
	
	public void showCalendar() {
		hideTimer.cancel(); // just in case it was running
		if(showing) return;
		showing = true;
		popup.setPopupPosition(getAbsoluteLeft(), getAbsoluteTop()+getOffsetHeight());
		popup.show();
	}
	
	public void hideCalendar() {
		hideTimer.cancel(); // just in case it was running
		if(!showing) return;
		showing = false;
		popup.hide();
	}
	
	private ChangeListenerCollection changeListeners;
	public void addChangeListener(ChangeListener listener) {
		textbox.addChangeListener(listener);
		if (changeListeners == null) {
			changeListeners = new ChangeListenerCollection();
		}
		changeListeners.add(listener);
	}

	public void removeChangeListener(ChangeListener listener) {
		textbox.removeChangeListener(listener);
		if (changeListeners != null) {
			changeListeners.remove(listener);
		}
	}

	public Date getDate() {
		String text = textbox.getText();
		if(optional && text.equals(""))
			return null;
		return calendar.getDate();
	}
	
	public void setDateString(String dateString) throws DateParseException {
		if(!textbox.getText().equals(dateString)) {
			textbox.setText(dateString);
			parseText();
		}
	}

	private void parseText() throws DateParseException {
		String text = textbox.getText();
		if(text.length() == 0)
			return;
		try {
			calendar.setDate(dateParser.parseDate(text));
			removeStyleName("invalid");
		} catch(DateParseException dpe) {
			calendar.setDate(new Date());
			addStyleName("invalid");
		}
		
	}
	
	protected void reformatDate() {
		textbox.setText(getDateString());
	}

	public String getDateString() {
		return dateFormat.format(calendar.getDate());
	}
	
	public ClientLocalizedParser getDateParser() {
		return dateParser;
	}

	public void setDateParser(ClientLocalizedParser dateParser) {
		this.dateParser = dateParser;
	}

	public boolean isHideOnSelect() {
		return hideOnSelect;
	}

	public void setHideOnSelect(boolean hideOnSelect) {
		this.hideOnSelect = hideOnSelect;
	}
	public boolean isShowOnFocus() {
		return showOnFocus;
	}

	public void setShowOnFocus(boolean showOnFocus) {
		this.showOnFocus = showOnFocus;
	}

	public void setDate(Date selectedDate) {
		if(selectedDate == null) {
			textbox.setText("");
		} else {
			calendar.setDate(selectedDate);
			reformatDate();
		}
	}

	public void setDate(int year, int month, int dayOfMonth) {
		setYear(year);
		setMonth(month);
		setDayOfMonth(dayOfMonth);
		reformatDate();
	}
	public void addClickListener(ClickListener listener) {
		textbox.addClickListener(listener);
		calendarIcon.addClickListener(listener);
	}

	public void addFocusListener(FocusListener listener) {
		textbox.addFocusListener(listener);
	}

	public void addKeyboardListener(KeyboardListener listener) {
		textbox.addKeyboardListener(listener);
	}

	public int getMaxLength() {
		return textbox.getMaxLength();
	}

	public String getText() {
		return textbox.getText();
	}

	public boolean isEnabled() {
		return textbox.isEnabled();
	}

	public boolean isReadOnly() {
		return textbox.isReadOnly();
	}

	public void removeClickListener(ClickListener listener) {
		textbox.removeClickListener(listener);
	}

	public void removeFocusListener(FocusListener listener) {
		textbox.removeFocusListener(listener);
	}

	public void removeKeyboardListener(KeyboardListener listener) {
		textbox.removeKeyboardListener(listener);
	}

	public void setEnabled(boolean enabled) {
		textbox.setEnabled(enabled);
	}

	public void setFocus(boolean focused) {
		textbox.setFocus(focused);
	}

	public void setMaxLength(int length) {
		textbox.setMaxLength(length);
	}

	public void setReadOnly(boolean readOnly) {
		textbox.setReadOnly(readOnly);
		calendarIcon.setVisible(!readOnly);
	}

	public void setText(String text) {
		textbox.setText(text);
	}

	public int getTabIndex() {
		return textbox.getTabIndex();
	}

	public void setTabIndex(int index) {
		textbox.setTabIndex(index);
	}

	public void setAccessKey(char key) {
		textbox.setAccessKey(key);
	}

    public void setFocusGroup(FocusGroup group) {
        textbox.setFocusGroup(group);
    }

    public boolean isShowing() {
        return showing;
    }

    public void setShowing(boolean showing) {
        this.showing = showing;
    }

    public DateTimeFormat getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(DateTimeFormat dateFormat) {
        this.dateFormat = dateFormat;
        dateParser.setDateFormat(dateFormat);
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }
    
    @Override
    protected void onEnsureDebugId(String baseID) {
        super.onEnsureDebugId(baseID);
        textbox.ensureDebugId(baseID+"-text");
        calendarIcon.ensureDebugId(baseID+"-icon");
    }

	public int getDayOfMonth() {
		return calendar.getDayOfMonth();
	}

	public int getMonth() {
		return calendar.getMonth();
	}

	public int getYear() {
		return calendar.getYear();
	}

	public void setDayOfMonth(int dayOfMonth) {
		calendar.setDayOfMonth(dayOfMonth);
	}

	public void setMonth(int month) {
		calendar.setMonth(month);
	}

	public void setYear(int year) {
		calendar.setYear(year);
	}
}
