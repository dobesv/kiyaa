package com.habitsoft.kiyaa.util;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class AsyncCallbackFactory {
	/**
	 * Default implement will alert() if the operation fails.  Subclasses should override this
	 * to do something more cross-browser friendly (IE blocks alerts these days).
	 */
	public AsyncCallback newInstance() {
        return newInstance(null);
	}
	
	public AsyncCallback newInstance(final Object marker) {
        return new AsyncCallback() {
            public void onSuccess(Object arg0) {
            }
            public void onFailure(Throwable caught) {
                if(marker != null && (caught instanceof Error) || (caught instanceof RuntimeException)) try {
                    Log.error("At "+marker.toString(), caught);
                } catch(Exception e) { }
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
    public static AsyncCallback defaultNewInstance(Object marker) {
        return defaultFactory.newInstance(marker);
    }
}
