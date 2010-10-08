package com.habitsoft.kiyaa.views;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HasHTML;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.SourcesClickEvents;
import com.google.gwt.user.client.ui.Widget;
import com.habitsoft.kiyaa.metamodel.Action;

public class ButtonView implements View, ClickListener {
	Action action;
	String label;
	Widget button;
	
	public ButtonView() {
		button = new Button();
		hookEvents();
	}
	public ButtonView(Widget button) {
		this.button = button;
		hookEvents();
	}
	
	protected void hookEvents() {
		if(button instanceof SourcesClickEvents) {
			((SourcesClickEvents)button).addClickListener(this);
		}
	}
	
	public void clearFields() {
	}

	public Widget getViewWidget() {
		return button;
	}

	public void load(AsyncCallback<Void> completionCallback) {
		completionCallback.onSuccess(null);
	}

	public void save(AsyncCallback<Void> callback) {
		callback.onSuccess(null);
	}

	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		if(button instanceof HasText) {
			((HasText)button).setText(label);
		} else if(button instanceof HasHTML) {
			((HasHTML)button).setHTML(label);
		}
	}
	public void onClick(Widget sender) {
		action.perform(new AsyncCallback<Void>() {
			public void onSuccess(Void result) {
			}
			public void onFailure(Throwable caught) {
				GWT.log("Action failed", caught);
			}
		});
	}
	public Action getAction() {
		return action;
	}
	public void setAction(Action action) {
		this.action = action;
	}

}
