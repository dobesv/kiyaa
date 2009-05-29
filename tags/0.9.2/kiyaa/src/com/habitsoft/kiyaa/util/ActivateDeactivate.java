package com.habitsoft.kiyaa.util;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface ActivateDeactivate {

	/**
	 * Tells the widget to create it's UI elements.
	 */
	public void activate(AsyncCallback whenReady);
	
	/**
	 * Tells the widget to destroy it's UI elements and remove all
	 * references to them.
	 */
	public void deactivate();
	
}
