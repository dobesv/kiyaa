package com.habitsoft.kiyaa.widgets;

import com.google.gwt.i18n.client.Constants;

public interface CalendarConstants extends Constants {
	@DefaultStringValue("About the calendar")
	String getInfo();
	@DefaultStringValue("Date/Time Selector\n" +
		"Date selection:\n" +
		"- Use the \u00ab, \u00bb buttons to select year\n" +
		"- Use the \u2039, \u203a buttons to select month\n")
	String getAbout();
	@DefaultStringValue("Prev. year (hold for menu)")
	String getPrevYear();
	@DefaultStringValue("Prev. month (hold for menu)")
	String getPrevMonth();
	@DefaultStringValue("Go Today")
	String getGoToday();
	@DefaultStringValue("Next month (hold for menu)")
	String getNextMonth();
	@DefaultStringValue("Next year (hold for menu)")
	String getNextYear();
	@DefaultStringValue("Select date")
	String getSelDate();
	@DefaultStringValue("Drag to move")
	String getDragToMove();
	@DefaultStringValue(" (today)")
	String getPartToday();
	@DefaultStringValue("Display %s first")
	String getDayFirst();
	@DefaultStringValue("0,6")
	String getWeekend();
	@DefaultStringValue("Close")
	String getClose();
	@DefaultStringValue("Today")
	String getToday();
	@DefaultStringValue("(Shift-)Click or drag to change value")
	String getTimePart();
	@DefaultStringValue("%Y-%m-%d")
	String getDefaultDateFormat();
	@DefaultStringValue("%a, %b %e")
	String getToolTipDateFormat();
	@DefaultStringValue("wk")
	String getWk();
	@DefaultStringValue("Time:")
	String getTime();
}
