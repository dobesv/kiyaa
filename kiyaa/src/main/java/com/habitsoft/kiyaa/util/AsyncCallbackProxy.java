package com.habitsoft.kiyaa.util;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;

public abstract class AsyncCallbackProxy<In,Out> implements AsyncCallback<In>, AsyncCallbackExtensions {
	private final AsyncCallback<Out> callback;
	private final Object marker;
	private boolean complete;
	
	public AsyncCallbackProxy(AsyncCallback<Out> delegate, Object marker) {
		super();
		//if(!(this instanceof AsyncCallbackWithTimeout) && !(delegate instanceof AsyncCallbackWithTimeout))
		//	delegate = new AsyncCallbackWithTimeout<T>(delegate);
		this.callback = delegate;
		this.marker = marker;
	}
	
	public AsyncCallbackProxy(AsyncCallback<Out> delegate) {
	    this(delegate, null);
	}

	/**
	 * Default failure handler passes the failure onto the delegate.
	 */
	public void onFailure(Throwable caught) {
	    if(marker != null && (caught instanceof Error || caught instanceof RuntimeException)) try {
	        Log.error("At "+marker.toString(), caught);
	    } catch(Exception e) { }
	    completed();
		if(callback != null)
			callback.onFailure(caught);
	}

	private void completed() {
	    try {
	        if(complete)
	        	Log.error("AsyncCallbackProxy called twice; marker = "+marker, new Error());
        } catch(Exception e) { }
        complete = true;
    }

	/**
	 * Subclass would implement this to process the parameter and
	 * call sendSuccess() with the intended result
	 */
	public abstract void onSuccess(In result);

	/**
	 * Mark this callback as "used"/"complete" and pass the parameter
	 * on to the delegate callback.
	 */
	protected void returnSuccess(Out result) {
		completed();
		if(callback != null)
			callback.onSuccess(result);
	}
	
    public Object getMarker() {
        return marker;
    }
	
    /**
     * Access the delegate callback without marking
     * the proxy as "complete".  Use this only for
     * special purposes.
     * 
     * @return
     */
	public AsyncCallback<Out> getDelegate() {
    	return callback;
    }
	
	/**
	 * Use this if you are passing the callback to to another method
	 * that will call onSuccess/onFailure for you.
	 * 
	 * For example:
	 * 
	 * @Override public void onSuccess(Void x) {
	 *    myView.load(takeCallback());
	 * }
	 * 
	 * Note: This marks this proxy as "used" or "completed" and cannot be
	 * called twice!
	 */
	public AsyncCallback<Out> takeCallback() {
		resetTimeout(null); // Let our delegate chain know that we're still "active" one way or another
		completed();
    	return callback;
    }
	
	@Override
	public void resetTimeout(Integer expectedTimeNeeded) {
		if(callback instanceof AsyncCallbackExtensions)
			((AsyncCallbackExtensions) callback).resetTimeout(null);
	}
	
	@Override
	public boolean isOkayToWaitForCurrentAction() {
		if(callback instanceof AsyncCallbackExtensions)
			return ((AsyncCallbackExtensions) callback).isOkayToWaitForCurrentAction();
		else
			return false;
	}
	
	/**
	 * Wrap a callback in a proxy that ignores the incoming value and returns void.  Useful
	 * if you are just interested in errors and not the actual result.
	 */
	public static <T> AsyncCallback<T> toVoid(AsyncCallback<Void> callback) {
		return new AsyncCallbackProxy<T, Void>(callback) {
			public void onSuccess(T result) { 
				returnSuccess(null); 
			}
		};
	}
}
