package com.habitsoft.kiyaa.views;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.habitsoft.kiyaa.util.AsyncCallbackProxy;

public class WhenView implements View {

	ViewFactory viewFactory;
	boolean shouldShow;
	View view;
	SimplePanel panel = new SimplePanel();
	String placeholderHtml;
	
	public WhenView() {
		DOM.setStyleAttribute(panel.getElement(), "display", "inline");
	}
	public ViewFactory getViewFactory() {
		return viewFactory;
	}

	public void setViewFactory(ViewFactory viewFactory) {
		this.viewFactory = viewFactory;
	}
	
	public void setTest(boolean truth) {
		shouldShow = truth;
	}
	
	public void setTestNot(boolean truth) {
		setTest(!truth);
	}
	
	public void clearFields() {
		if(view != null) {
			view.clearFields();
		}
	}

	public Widget getViewWidget() {
		return panel;
	}

	public void load(AsyncCallback callback) {
		if(shouldShow) {
			if(view == null) {
				if(viewFactory == null) {
					callback.onSuccess(null);
					return;
				}
				view = viewFactory.createView();
				
			}
			view.load(new AsyncCallbackProxy(callback) {
				@Override
				public void onSuccess(Object result) {
					// View may have become null while we waited, for whatever reason
					if(view != null && panel.getWidget() != view.getViewWidget())
						panel.setWidget(view.getViewWidget());
					super.onSuccess(result);
				}
			});
		} else {
			if(view != null) {
				view.getViewWidget().removeFromParent();
				view = null;
			}
			if(placeholderHtml != null) {
				DOM.setInnerHTML(panel.getElement(), placeholderHtml);
			}
			callback.onSuccess(null);
		}
	}

	public void save(AsyncCallback callback) {
		if(view != null) {
			view.save(callback);
		} else {
			callback.onSuccess(null);
		}
	}
	public String getPlaceholderHtml() {
		return placeholderHtml;
	}
	public void setPlaceholderHtml(String placeholderHtml) {
		this.placeholderHtml = placeholderHtml;
	}
	public void setPlaceholderNbsp(boolean useNbsp) {
		if(useNbsp) {
			placeholderHtml = "&nbsp;";
		}
	}
}
