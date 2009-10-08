package com.habitsoft.kiyaa.util;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;

public abstract class AsyncCallbackFilter<In,Out> implements AsyncCallback<In> {
	protected final AsyncCallback<Out> callback;
	protected final Object marker;
	protected boolean complete;
	
	public AsyncCallbackFilter(AsyncCallback<Out> delegate, Object marker) {
		super();
		//if(!(this instanceof AsyncCallbackWithTimeout) && !(delegate instanceof AsyncCallbackWithTimeout))
		//	delegate = new AsyncCallbackWithTimeout<T>(delegate);
		this.callback = delegate;
		this.marker = marker;
	}
	
	public AsyncCallbackFilter(AsyncCallback<Out> delegate) {
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

	/**
	 * Subclass would call sendSuccess() with the intended result
	 */
	public abstract void onSuccess(In result);

	protected void returnSuccess(Out result) {
		completed();
		if(callback != null)
			callback.onSuccess(result);
	}
	
    public Object getMarker() {
        return marker;
    }
	
	public AsyncCallback<Out> getDelegate() {
    	return callback;
    }
}
