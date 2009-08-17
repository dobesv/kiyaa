package com.habitsoft.kiyaa.widgets;

import java.util.ArrayList;
import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.dom.client.TableRowElement;
import com.google.gwt.dom.client.TableSectionElement;
import com.google.gwt.i18n.client.constants.DateTimeConstants;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ChangeListenerCollection;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.ClickListenerCollection;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.SourcesChangeEvents;
import com.google.gwt.user.client.ui.SourcesClickEvents;
import com.google.gwt.user.client.ui.Widget;
import com.habitsoft.kiyaa.util.HoverStyleHandler;

public class Calendar extends ComplexPanel implements SourcesChangeEvents, SourcesClickEvents {
	
	class CalendarButton extends Label {
		public CalendarButton() {
			setStylePrimaryName("button");
		}
	}
	
	Date date;
	boolean closable = true;
	
	TableElement table;
	TableSectionElement thead;
	TableSectionElement tbody;
	TableSectionElement tfoot;
	TableRowElement titleRow;
	TableRowElement navRow;
	TableRowElement dayNames;
	
	final CalendarConstants constants = GWT.create(CalendarConstants.class);
	final DateTimeConstants dtc = GWT.create(DateTimeConstants.class);
	
	ClickListenerCollection closeListeners;
	ChangeListenerCollection changeListeners;
	ClickListenerCollection clickListeners;
	private Label closeButton;
	
	int minYear = 1970;
	int maxYear = 2999;
	int firstDayOfWeek = 0; // 0 for sunday
	
	boolean showOtherMonths;
	
	HoverStyleHandler.Group hoverGroup = new HoverStyleHandler.Group();
	HoverStyleHandler.Group rowHoverGroup = new HoverStyleHandler.Group();
	
	public Calendar() {
		setElement(DOM.createDiv());
		setStylePrimaryName("calendar");
		date = new Date();
		create();
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
	class DayLabel extends NavButton implements ClickListener {
		int row;
		int col;
		boolean otherMonth;
		boolean disabled;
		Date date;
		
		public DayLabel(int row, int col) {
			this.row = row;
			this.col = col;
			setStyleName("day");
			addClickListener(this);
		}
		
		public void onClick(Widget sender) {
			if(!disabled)
				cellClicked(this);
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
	private void create() {
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
		dateChanged();
		getElement().appendChild(table);
		
	}
	public void cellClicked(DayLabel cellLabel) {
		//GWT.log("Calendar cell clicked - "+cellLabel.col+","+cellLabel.row+" date "+cellLabel.getDate(), null);
		if(setDate(cellLabel.getDate())) {
    		if(changeListeners != null)
    			changeListeners.fireChange(this);
		} else if(closable) {
			if(closeListeners != null)
				// Double-click means set and close
				closeListeners.fireClick(this);
		}
		if(clickListeners != null)
			clickListeners.fireClick(this);
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
		help.addClickListener(new ClickListener() {
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
			closeButton.addClickListener(new ClickListener() {
				public void onClick(Widget sender) {
					closeListeners.fireClick(sender);
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
		prevYear.addClickListener(new ClickListener() {
			public void onClick(Widget sender) {
				gotoPrevYear();
			}
		});
		add(prevYear, navRow.insertCell(-1));
		
		Label prevMonth = new NavButton("\u2039");
		prevMonth.addClickListener(new ClickListener() {
			public void onClick(Widget sender) {
				gotoPrevMonth();
			}
		});
		add(prevMonth, navRow.insertCell(-1));

		Label today = new NavButton(constants.getToday());
		today.addClickListener(new ClickListener() {
			public void onClick(Widget sender) {
				gotoToday();
			}
		});
		today.setTitle(constants.getGoToday());
		TableCellElement todayCell = navRow.insertCell(-1);
		todayCell.setColSpan(3);
		add(today, todayCell);
		
		Label nextMonth = new NavButton("\u203a");
		nextMonth.addClickListener(new ClickListener() {
			public void onClick(Widget sender) {
				gotoNextMonth();
			}
		});
		add(nextMonth, navRow.insertCell(-1));
		
		Label nextYear = new NavButton("\u00bb");
		nextYear.addClickListener(new ClickListener() {
			public void onClick(Widget sender) {
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
	
	@SuppressWarnings("deprecation")
	public void gotoPrevYear() {
		Date d = new Date(date.getTime());
		d.setYear(d.getYear()-1);
		setDate(d);
	}
	
	@SuppressWarnings("deprecation")
	public void gotoNextYear() {
		Date d = new Date(date.getTime());
		d.setYear(d.getYear()+1);
		setDate(d);
	}
	
	@SuppressWarnings("deprecation")
	public void gotoPrevMonth() {
		Date d = new Date(date.getTime());
		if(d.getMonth() == 0) {
			d.setYear(d.getYear()-1);
			d.setMonth(11);
		} else {
			d.setMonth(d.getMonth()-1);
		}
		setDate(d);
	}
	
	@SuppressWarnings("deprecation")
	public void gotoNextMonth() {
		Date d = new Date(date.getTime());
		if(d.getMonth() == 11) {
			d.setYear(d.getYear()+1);
			d.setMonth(0);
		} else {
			d.setMonth(d.getMonth()+1);
		}
		setDate(d);
	}
	
	public void gotoToday() {
		setDate(new Date());
	}

	public Date getDate() {
		return date;
	}

	public boolean setDate(Date selectedDate) {
		if(selectedDate == null)
			throw new NullPointerException("Null dates not allowed for Calendar display");
		if(!selectedDate.equals(this.date)) {
			this.date = selectedDate;
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
		int year = date.getYear()+1900;
		if(year < minYear) {
			year = minYear;
			date.setYear(year-1900);
		} else if(year > maxYear) {
			year = maxYear;
			date.setYear(year-1900);
		}
		int month = date.getMonth();
		int mday = date.getDate();

		// calendar voodoo for computing the first day that would actually be
		// displayed in the calendar, even if it's from the previous month.
		// WARNING: this is magic. ;-)		
		Date tempDate = new Date(date.getTime());
		tempDate.setDate(1);
		int day1 = (tempDate.getDay() - firstDayOfWeek) % 7;
		if(day1 < 0)
			day1 += 7;
		tempDate.setDate(-day1);
		tempDate.setDate(tempDate.getDate()+1);
		
		for(WeekRow row : rows) { // up to 6 rows
			setStyleName(row.row, "daysrow");
			boolean hasDays = false;
			for(DayLabel dayLabel : row.days) {
				int iday = tempDate.getDate();
				dayLabel.setStyleName("day");
				dayLabel.setText(String.valueOf(iday));
				dayLabel.setDate(new Date(tempDate.getTime()));
				
				boolean currentMonth = (tempDate.getMonth() == date.getMonth());
				dayLabel.setOtherMonth(!currentMonth);
				if(!dayLabel.disabled) {
    				hasDays = true;
    				
    				if(currentMonth && iday == mday) {
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
		
		title.setText(dtc.months()[month]+", "+year);
		setVisible(table, true);
	}

	public boolean isClosable() {
		return closable;
	}

	public void setClosable(boolean closable) {
		this.closable = closable;
	}

	public void addCloseListener(ClickListener listener) {
		if(closeListeners == null) closeListeners = new ClickListenerCollection();
		closeListeners.add(listener);
	}

	public void removeCloseListener(ClickListener listener) {
		if(closeListeners != null)
			closeListeners.remove(listener);
	}

	public void addChangeListener(ChangeListener listener) {
		if(changeListeners == null) changeListeners = new ChangeListenerCollection();
		changeListeners.add(listener);
	}

	public void removeChangeListener(ChangeListener listener) {
		if(changeListeners != null)
			changeListeners.remove(listener);
	}

	public void addClickListener(ClickListener listener) {
		if(clickListeners == null) clickListeners = new ClickListenerCollection();
		clickListeners.add(listener);
	}

	public void removeClickListener(ClickListener listener) {
		if(clickListeners != null)
			clickListeners.remove(listener);
	}

	@SuppressWarnings("deprecation")
	public void gotoNextWeek() {
		Date d = new Date(date.getTime());
		d.setDate(d.getDate()+7);
		setDate(d);
	}
	
	@SuppressWarnings("deprecation")
	public void gotoPrevWeek() {
		Date d = new Date(date.getTime());
		d.setDate(d.getDate()-7);
		setDate(d);
	}

	@SuppressWarnings("deprecation")
	public void gotoPrevDay() {
		Date d = new Date(date.getTime());
		d.setDate(d.getDate()-1);
		setDate(d);
	}

	@SuppressWarnings("deprecation")
	public void gotoNextDay() {
		Date d = new Date(date.getTime());
		d.setDate(d.getDate()+1);
		setDate(d);
	}
	
}
