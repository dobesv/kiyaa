package com.habitsoft.kiyaa.views;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;

public class FrameView extends HTML implements View {

	String url;
	Element iframe;
	
	public FrameView() {
		iframe = DOM.createIFrame();
		DOM.appendChild(getElement(), iframe);
	}
	
	public void clearFields() {
		url = null;
		DOM.setElementProperty(iframe, "src", "blank");
	}

	public Widget getViewWidget() {
		return this;
	}

	public void load(AsyncCallback<Void> callback) {
		if(url != null)
			DOM.setElementProperty(iframe, "src", url);
		callback.onSuccess(null);
	}

	public void save(AsyncCallback<Void> callback) {
		callback.onSuccess(null);
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

}
