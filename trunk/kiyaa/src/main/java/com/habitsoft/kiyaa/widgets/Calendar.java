package com.habitsoft.kiyaa.widgets;

import java.util.ArrayList;
import java.util.Date;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.dom.client.TableSectionElement;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.event.dom.client.HasChangeHandlers;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.HasCloseHandlers;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.i18n.client.constants.DateTimeConstants;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ChangeListenerCollection;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.Widget;
import com.habitsoft.kiyaa.util.HoverStyleHandler;

public class Calendar extends ComplexPanel implements HasCloseHandlers<Calendar>, HasClickHandlers, HasChangeHandlers {
	
	class CalendarButton extends Label {
		public CalendarButton() {
			setStylePrimaryName("button");
		}
	}
	
	int year, month, dayOfMonth;
	boolean closable = true;
	
	TableElement table;
	TableSectionElement thead;
	TableSectionElement tbody;
	TableSectionElement tfoot;
	TableRowElement titleRow;
	TableRowElement navRow;
	TableRowElement dayNames;
	
	final CalendarConstants constants = GWT.create(CalendarConstants.class);
	final DateTimeConstants dtc = LocaleInfo.getCurrentLocale().getDateTimeConstants();
	
	HandlerManager handlerManager;
	private Label closeButton;
	
	int minYear = 1970;
	int maxYear = 2999;
	int firstDayOfWeek = 0; // 0 for sunday
	
	boolean showOtherMonths;
	
	HoverStyleHandler.Group hoverGroup = new HoverStyleHandler.Group();
	HoverStyleHandler.Group rowHoverGroup = new HoverStyleHandler.Group();
	
	public Calendar() {
		handlerManager = new HandlerManager(this);
		setElement(DOM.createDiv());
		setStylePrimaryName("calendar");
		table = TableElement.as(DOM.createTable());
		table.setCellPadding(0);
		table.setCellSpacing(0);
		thead = table.createTHead();
		createTitleRow();
		createNavRow();
		createDayNameRow();
		tbody = TableSectionElement.as(DOM.createTBody());
		table.appendChild(tbody);
		rows = new ArrayList();
		for(int i=0; i < 6; i++) {
			rows.add(new WeekRow());
		}
		setDate(new Date());
		getElement().appendChild(table);
	}

	static native void disableSelection(Element elt) /*-{
		elt.onselectstart = function(){return false;}
	}-*/;
	
	class NavButton extends Label {
		public NavButton() {
			setStyleName("button");
			addMouseListener(new HoverStyleHandler(this, hoverGroup));
			disableSelection(getElement());
		}
		
		public NavButton(String text) {
			this();
			setText(text);
		}
		
		@Override
		public void onBrowserEvent(Event event) {
			if((event.getTypeInt() & (Event.ONMOUSEDOWN|Event.ONMOUSEUP|Event.ONCLICK|Event.ONDBLCLICK)) != 0) {
				event.cancelBubble(true);
				event.preventDefault();
			}
			super.onBrowserEvent(event);
		}
	}
	class DayLabel extends NavButton implements ClickHandler {
		int row;
		int col;
		boolean otherMonth;
		boolean disabled;
		Date date;
		
		public DayLabel(int row, int col) {
			this.row = row;
			this.col = col;
			setStyleName("day");
			addClickHandler(this);
		}
		
		public void onClick(ClickEvent event) {
			if(!disabled)
				cellClicked(this, event);
		}

		public void setOtherMonth(boolean otherMonth) {
			this.otherMonth = otherMonth;
			if(otherMonth) {
				if(showOtherMonths) {
					addStyleName("othermonth");
					disabled = false;
				} else {
					addStyleName("emptycell");
					setHTML("&nbsp;");
					disabled = true;
				}
			} else {
				disabled = false;
			}
		}

		public void setDate(Date date) {
			this.date = date;
		}
		
		public Date getDate() {
			return date;
		}
	}
	class WeekRow extends Widget {
		ArrayList<DayLabel> days = new ArrayList();
		TableRowElement row;
		int rowNum;
		public WeekRow() {
			rowNum = tbody.getRows().getLength();
			row = tbody.insertRow(-1);
			setElement(row);
			HoverStyleHandler hoverStyleHandler = new HoverStyleHandler(this, rowHoverGroup);
			for(int j=7; j > 0; j--) {
				TableCellElement cell = row.insertCell(-1);
				DayLabel cellLabel = new DayLabel(rowNum,j);
				cellLabel.addMouseListener(hoverStyleHandler);
				add(cellLabel, cell);
				days.add(cellLabel);
			}
		}
		
	}
	ArrayList<WeekRow> rows;
	private Label title;
	public void cellClicked(DayLabel cellLabel, ClickEvent click) {
		//GWT.log("Calendar cell clicked - "+cellLabel.col+","+cellLabel.row+" date "+cellLabel.getDate(), null);
		if(setDate(cellLabel.getDate())) {
			DomEvent.fireNativeEvent(Document.get().createChangeEvent(), this);
		} else if(closable) {
			CloseEvent.fire(this, this);
		}
		handlerManager.fireEvent(click);
	}

	private void createDayNameRow() {
		dayNames = thead.insertRow(-1);
		setStyleName(dayNames, "daynames");
		
		for(String wd : dtc.shortWeekdays()) {
			TableCellElement td = dayNames.insertCell(-1);
			td.setInnerText(wd);
			setStyleName(td, "name");
			boolean weekend = wd.equals(dtc.shortWeekdays()[0]) || wd.equals(dtc.shortWeekdays()[6]);
			if(weekend) setStyleName(td, "weekend", true);
		}
	}
	private void createTitleRow() {
		titleRow = thead.insertRow(-1);
		
		/*
		NavButton help = new NavButton("?");
		help.setStyleName("button");
		help.addStyleName("nav");
		help.addClickHandler(new ClickHandler() {
			public void onClick(Widget sender) {
				showHelpPopup();
			}
		});
		add(help, titleRow.insertCell(-1));
		help.addMouseListener(new HoverStyleHandler(help, hoverGroup));
		*/
		
		addPlaceholder();
		
		title = new Label();
		title.setStyleName("title");
		
		TableCellElement titleCell = titleRow.insertCell(-1);
		titleCell.setColSpan(5);
		add(title, titleCell);
		
		if(closable) {
			closeButton = new NavButton("\u00d7");
			closeButton.setTitle(constants.getClose());
			closeButton.addStyleName("title");
			closeButton.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					CloseEvent.fire(Calendar.this, Calendar.this);
				}
			});
			add(closeButton, titleRow.insertCell(-1));
		} else {
			addPlaceholder();
		}
		//if(weekNumbers) titleLength++;
	}
	private void addPlaceholder() {
		Label placeHolder = new Label(" ");
		placeHolder.setStyleName("title");
		add(placeHolder, titleRow.insertCell(-1));
	}

	protected void showHelpPopup() {
		Window.alert(constants.getAbout());
	}

	private void createNavRow() {
		navRow = thead.insertRow(-1);
		setStyleName(navRow, "headrow");
		
		Label prevYear = new NavButton("\u00ab");
		prevYear.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				gotoPrevYear();
			}
		});
		add(prevYear, navRow.insertCell(-1));
		
		Label prevMonth = new NavButton("\u2039");
		prevMonth.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				gotoPrevMonth();
			}
		});
		add(prevMonth, navRow.insertCell(-1));

		Label today = new NavButton(constants.getToday());
		today.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				gotoToday();
			}
		});
		today.setTitle(constants.getGoToday());
		TableCellElement todayCell = navRow.insertCell(-1);
		todayCell.setColSpan(3);
		add(today, todayCell);
		
		Label nextMonth = new NavButton("\u203a");
		nextMonth.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				gotoNextMonth();
			}
		});
		add(nextMonth, navRow.insertCell(-1));
		
		Label nextYear = new NavButton("\u00bb");
		nextYear.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent event) {
				gotoNextYear();
			}
		});
		add(nextYear, navRow.insertCell(-1));
	}
	private void add(Label child, com.google.gwt.dom.client.Element container) {
	    // Detach new child.
	    child.removeFromParent();

	    // Logical attach.
	    getChildren().add(child);

	    // Physical attach.
	    container.appendChild(child.getElement());

	    // Adopt.
	    adopt(child);
	}
	

	public void gotoPrevYear() {
		year -= 1;
		dateChanged();
	}
	

	public void gotoNextYear() {
		year += 1;
		dateChanged();
	}
	

	public void gotoPrevMonth() {
		if(month <= 1) {
			year--;
			month = 12;
		} else {
			month--;
		}
		dateChanged();
	}
	

	public void gotoNextMonth() {
		if(month == 12) {
			month = 1;
			year++;
		} else {
			month++;
		}
		dateChanged();
	}
	
	public void gotoToday() {
		setDate(new Date());
	}

	public Date getDate() {
		return createDate();
	}

	private Date createDate() {
		return new Date(year-1900, month-1, dayOfMonth);
	}

	@SuppressWarnings("deprecation")
	public boolean setDate(Date selectedDate) {
		if(selectedDate == null)
			throw new NullPointerException("Null dates not allowed for Calendar display");
		int newYear = selectedDate.getYear()+1900;
		int newMonth = selectedDate.getMonth()+1;
		int newDayOfMonth = selectedDate.getDate();
		
		if(newYear != this.year || newMonth != this.month || newDayOfMonth != this.dayOfMonth) {
			this.year = newYear;
			this.month = newMonth;
			this.dayOfMonth = newDayOfMonth;
			dateChanged();
			return true;
		}
		return false;
	}

	@SuppressWarnings("deprecation")
	private void dateChanged() {
		Date today = new Date();
		int thisYear = today.getYear()+1900;
		int thisMonth = today.getMonth();
		int thisDay = today.getDate();
		setVisible(table, false);
		try {
			if(year < minYear) {
				year = minYear;
			} else if(year > maxYear) {
				year = maxYear;
			}
			// calendar voodoo for computing the first day that would actually be
			// displayed in the calendar, even if it's from the previous month.
			// WARNING: this is magic. ;-)		
			Date tempDate = createDate();
			tempDate.setDate(1);

			int day1 = (tempDate.getDay() - firstDayOfWeek) % 7;
			if(day1 < 0)
				day1 += 7;
			tempDate.setDate(-day1 + 1);
			//tempDate.setDate(tempDate.getDate()+1);

			for(WeekRow row : rows) { // up to 6 rows
				setStyleName(row.row, "daysrow");
				boolean hasDays = false;
				for(DayLabel dayLabel : row.days) {
					int iday = tempDate.getDate();
					dayLabel.setStyleName("day");
					dayLabel.setText(String.valueOf(iday));
					dayLabel.setDate(new Date(tempDate.getTime()));
					
					boolean currentMonth = (tempDate.getMonth() == (month-1));
					dayLabel.setOtherMonth(!currentMonth);
					if(!dayLabel.disabled) {
	    				hasDays = true;
	    				
	    				if(currentMonth && iday == dayOfMonth) {
	    					dayLabel.addStyleName("selected");
	    				}
	    				if(tempDate.getYear() == thisYear 
	    					&& tempDate.getMonth() == thisMonth
	    					&& tempDate.getDate() == thisDay) {
	    					dayLabel.addStyleName("today");
	    				}
	    				int wday = tempDate.getDay();
	    				boolean isWeekend = wday == 0 || wday == 6;
	    				if(isWeekend)
	    					dayLabel.addStyleName(currentMonth?"weekend":"oweekend");
	//				} else {
	//					GWT.log("Cell date "+tempDate+" disabled; not in the same month as "+date, null);
					}
					
					// Advance to the next day
					tempDate.setDate(iday+1);
				}
				if(!(hasDays || showOtherMonths)) {
					setStyleName(row.row, "emptyrow");
				}
			}
			
			title.setText(dtc.months()[month-1]+", "+year);
		} catch(Exception e) {
			Log.error("Error updating calendar for year="+thisYear+" month="+thisMonth+" day="+thisDay+": "+e, e);
		} finally {
			setVisible(table, true);
		}
	}

	public boolean isClosable() {
		return closable;
	}

	public void setClosable(boolean closable) {
		this.closable = closable;
	}

	public HandlerRegistration addCloseHandler(CloseHandler<Calendar> handler) {
		return handlerManager.addHandler(CloseEvent.getType(), handler);
	}

	public HandlerRegistration addChangeHandler(ChangeHandler handler) {
		return handlerManager.addHandler(ChangeEvent.getType(), handler);
	}

	public HandlerRegistration addClickHandler(ClickHandler handler) {
		return handlerManager.addHandler(ClickEvent.getType(), handler);
	}

	@SuppressWarnings("deprecation")
	public void gotoNextWeek() {
		Date d = createDate();
		d.setDate(d.getDate()+7);
		setDate(d);
	}
	
	@SuppressWarnings("deprecation")
	public void gotoPrevWeek() {
		Date d = createDate();
		d.setDate(d.getDate()-7);
		setDate(d);
	}

	@SuppressWarnings("deprecation")
	public void gotoPrevDay() {
		Date d = createDate();
		d.setDate(d.getDate()-1);
		setDate(d);
	}

	@SuppressWarnings("deprecation")
	public void gotoNextDay() {
		Date d = createDate();
		d.setDate(d.getDate()+1);
		setDate(d);
	}

	public int getYear() {
		return year;
	}

	public void setYear(int year) {
		this.year = year;
	}

	public int getMonth() {
		return month;
	}

	public void setMonth(int month) {
		this.month = month;
	}

	public int getDayOfMonth() {
		return dayOfMonth;
	}

	public void setDayOfMonth(int dayOfMonth) {
		this.dayOfMonth = dayOfMonth;
	}
	
}
