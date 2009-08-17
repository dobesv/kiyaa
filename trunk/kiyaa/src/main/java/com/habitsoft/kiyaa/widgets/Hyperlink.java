package com.habitsoft.kiyaa.widgets;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.ui.Widget;

public class Hyperlink extends com.google.gwt.user.client.ui.Hyperlink implements HistoryListener {
	String prefix="";

	public Hyperlink() {
		super();
	}
	public Hyperlink(String text, boolean asHTML, String targetHistoryToken) {
		super(text, asHTML, targetHistoryToken);
	}
	public Hyperlink(String text, String targetHistoryToken) {
		super(text, targetHistoryToken);
	}
	
	public boolean isActive() {
		final String targetHistoryToken = getTargetHistoryToken();
        return targetHistoryToken != null &&
               targetHistoryToken.length() > 0 &&
		       History.getToken().equals(targetHistoryToken);
	}
	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}
	
	public void setTarget(String target) {
		super.setTargetHistoryToken(prefix + target);
	}
	public void clearFields() {
	}
	public Widget getViewWidget() {
		return this;
	}
	@Override
	protected void onAttach() {
		super.onAttach();
		History.addHistoryListener(this);
		onHistoryChanged(History.getToken());
	}
	
	@Override
	protected void onDetach() {
		History.removeHistoryListener(this);
		super.onDetach();				
	}
	
	public void onHistoryChanged(String historyToken) {
		if(isActive())
			addStyleName("active");
		else
			removeStyleName("active");
	}
}
