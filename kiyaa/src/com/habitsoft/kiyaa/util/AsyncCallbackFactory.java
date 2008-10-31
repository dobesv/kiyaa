package com.habitsoft.kiyaa.util;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class AsyncCallbackFactory {
	/**
	 * Default implement will alert() if the operation fails.  Subclasses should override this
	 * to do something more cross-browser friendly (IE blocks alerts these days).
	 */
	public AsyncCallback newInstance() {
		return new AsyncCallback() {
			public void onSuccess(Object arg0) {
			}
			public void onFailure(Throwable caught) {
				if(caught.getMessage() != null)
					Window.alert(caught.getMessage());
				else
					Window.alert(caught.toString());
			}
		};
	}
	
	static AsyncCallbackFactory defaultFactory = new AsyncCallbackFactory();

	public static AsyncCallbackFactory getDefaultFactory() {
		return defaultFactory;
	}

	public static void setDefaultFactory(AsyncCallbackFactory defaultFactory) {
		AsyncCallbackFactory.defaultFactory = defaultFactory;
	}
	
	public static AsyncCallback defaultNewInstance() {
		return defaultFactory.newInstance();
	}
}
