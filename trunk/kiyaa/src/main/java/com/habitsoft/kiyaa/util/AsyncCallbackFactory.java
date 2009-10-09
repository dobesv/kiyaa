package com.habitsoft.kiyaa.util;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

public abstract class AsyncCallbackFactory {
	/**
	 * Default implement will alert() if the operation fails.  Subclasses should override this
	 * to do something more cross-browser friendly (IE blocks alerts these days).
	 */
	public abstract <T> AsyncCallback<T> newInstance();	
	public <T> AsyncCallback<T> newInstance(final Object marker) { return newInstance(); }
	
	static AsyncCallbackFactory defaultFactory = new AsyncCallbackFactory() {
	    /**
	     * Default implement will alert() if the operation fails.  Subclasses should override this
	     * to do something more cross-browser friendly (IE blocks alerts these days).
	     */
	    @Override
        public <T> AsyncCallback<T> newInstance() {
	        return newInstance(null);
	    }
	    
	    @Override
        public <T> AsyncCallback<T> newInstance(final Object marker) {
	        return new AsyncCallback<T>() {
	            public void onSuccess(T arg0) {
	            }
	            public void onFailure(Throwable caught) {
	                if(marker != null && (caught instanceof Error || caught instanceof RuntimeException)) try {
	                    Log.error("At "+marker.toString(), caught);
	                } catch(Exception e) { }
	                if(caught.getMessage() != null)
	                    Window.alert(caught.getMessage());
	                else
	                    Window.alert(caught.toString());
	            }
	        };
	    }
	};

	public static AsyncCallbackFactory getDefaultFactory() {
		return defaultFactory;
	}

	public static void setDefaultFactory(AsyncCallbackFactory defaultFactory) {
		AsyncCallbackFactory.defaultFactory = defaultFactory;
	}
	
	public static <T> AsyncCallback<T> defaultNewInstance() {
		return defaultFactory.<T>newInstance();
	}
    public static <T> AsyncCallback<T> defaultNewInstance(Object marker) {
        return defaultFactory.<T>newInstance(marker);
    }
}
