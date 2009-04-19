package com.habitsoft.kiyaa.util;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class AsyncCallbackProxy<T> implements AsyncCallback<T> {
	protected final AsyncCallback callback;
	protected final Object marker;
	protected boolean complete;
	
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
	    completed();
	    if(marker != null && (caught instanceof Error || caught instanceof RuntimeException)) try {
	        Log.error("At "+marker.toString(), caught);
	    } catch(Exception e) { }
		if(callback != null)
			callback.onFailure(caught);
	}

	private void completed() {
	    try {
	        if(complete) Log.error("AsyncCallbackProxy called twice; at "+marker, new Error());
        } catch(Exception e) { }
        complete = true;
    }

    public void onSuccess(T result) {
        completed();
		if(callback != null)
			callback.onSuccess(result);
	}

    public Object getMarker() {
        return marker;
    }
	

}
