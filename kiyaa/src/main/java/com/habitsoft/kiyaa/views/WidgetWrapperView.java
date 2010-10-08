package com.habitsoft.kiyaa.views;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;

/**
 * Class to wrap a normal widget into a view, so it can be used
 * by some of the classes that normally wrap something more complex.
 * 
 * Typically the widget is a Label or HTML.
 *
 */
public class WidgetWrapperView implements View {

	protected final Widget widget;
	public WidgetWrapperView(Widget widget) {
		this.widget = widget;
	}
	
	public void clearFields() {}
	public Object getModel() {return null;}
	public Widget getViewWidget() {	
		return widget; 
	}
	

	public Widget getWidget() {
		return widget;
	}

	public void load(AsyncCallback<Void> callback) {
		callback.onSuccess(null);
	}

	public void save(AsyncCallback<Void> callback) {
		callback.onSuccess(null);
	}
}
