package com.habitsoft.kiyaa.views;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import com.habitsoft.kiyaa.metamodel.Value;
import com.habitsoft.kiyaa.util.AsyncCallbackProxy;

public class LoadView implements View {
	Value from;
	Value to;
	final Widget nullDisplay = new HTML("");
	
	public void clearFields() {
	}

	public Widget getViewWidget() {
		return new HTML("");
	}

	public void load(AsyncCallback<Void> callback) {
		from.getValue(new AsyncCallbackProxy<Object,Void>(callback) {
			@Override
			public void onSuccess(Object result) {
				to.setValue(result, takeCallback());
			}
		});
	}

	public void save(AsyncCallback<Void> callback) {
		to.getValue(new AsyncCallbackProxy<Object,Void>(callback) {
			@Override
			public void onSuccess(Object result) {
				from.setValue(result, takeCallback());
			}
		});
	}

	public Value getFrom() {
		return from;
	}

	public void setFrom(Value from) {
		this.from = from;
	}

	public Value getTo() {
		return to;
	}

	public void setTo(Value to) {
		this.to = to;
	}

}
