package com.habitsoft.kiyaa.util;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class AsyncCallbackProxy<T> implements AsyncCallback<T> {
	protected final AsyncCallback callback;
	protected final Object marker;
	
	public AsyncCallbackProxy(AsyncCallback delegate, Object marker) {
		super();
		//if(!(this instanceof AsyncCallbackWithTimeout) && !(delegate instanceof AsyncCallbackWithTimeout))
		//	delegate = new AsyncCallbackWithTimeout<T>(delegate);
		this.callback = delegate;
		this.marker = marker;
	}
	
	public AsyncCallbackProxy(AsyncCallback delegate) {
	    this(delegate, null);
	}

	public void onFailure(Throwable caught) {
	    if(marker != null && (caught instanceof Error) || (caught instanceof RuntimeException)) try {
	        Log.error("At "+marker.toString(), caught);
	    } catch(Exception e) { }
		if(callback != null)
			callback.onFailure(caught);
	}

	public void onSuccess(T result) {
		if(callback != null)
			callback.onSuccess(result);
	}

    public Object getMarker() {
        return marker;
    }
	

}
