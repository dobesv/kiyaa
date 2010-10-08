package com.habitsoft.kiyaa.util;

import java.util.ArrayList;

import com.google.gwt.user.client.rpc.AsyncCallback;

public class AsyncCallbackShared<T> implements AsyncCallback<T>, AsyncCallbackExtensions {

	ArrayList<AsyncCallback<T>> callbacks = new ArrayList<AsyncCallback<T>>();
	boolean done = false;
	T result = null;
	Throwable caught = null;
	
	public AsyncCallbackShared() {
	}
	
	public AsyncCallbackShared(AsyncCallback<T> firstCallback) {
		callbacks.add(firstCallback);
	}

	/**
	 * Construct the shared callback in an initially "done" state.  The
	 * callback has to be reset in order to be used again.
	 * 
	 * Useful for cases where you want to use isDone() as a flag for
	 * whether an operation is in progress or if the shared callback
	 * is currently "idle".
	 * 
	 * @param result Result to return to any callback added
	 */
	public AsyncCallbackShared(T result) {
		super();
		this.done = true;
		this.result = result;
	}

	/**
	 * If the callback has already been called (isDone()) then
	 * returns the success / failure it received before.
	 * 
	 * Otherwise the callback is stored and will be fired
	 * when this callback is invoked with success / failure
	 */
	public void addCallback(AsyncCallback<T> callback) {
		if(callback == null)
			return;
		if(done) {
			fireCallback(callback);
		} else {
			callbacks.add(callback);
		}
	}
	
	protected void fireCallback(AsyncCallback<T> callback) {
		if(caught != null) {
			callback.onFailure(caught);
		} else {
			callback.onSuccess(result);
		}
	}
	
	protected void fireCallbacks() {
		for (AsyncCallback<T> callback : callbacks.toArray(new AsyncCallback[callbacks.size()])) {
			fireCallback(callback);
		}
	}
	public void onFailure(Throwable caught) {
		this.caught = caught;
		this.done = true;
		this.fireCallbacks();
	}
	
	/**
	 * Clear the "done" flag and any cached result from a prior call.
	 * 
	 * When done == false the callback could be considered to be "in use",
	 * use reset() before starting a new operation to indicate that an
	 * operation is in progress.
	 */
	public void reset() {
	    callbacks.clear();
        result = null;
	    caught = null;
	    done = false;
	}

	public void onSuccess(T result) {
		this.result = result;
		this.done = true;
		this.fireCallbacks();
	}

	public ArrayList<AsyncCallback<T>> getCallbacks() {
		return callbacks;
	}

	/**
	 * True if this callback was invoked with success or failure.
	 * 
	 * Also true if the constructor was invoked with a result parameter.
	 */
	public boolean isDone() {
		return done;
	}

	public T getResult() {
		return result;
	}

	public Throwable getCaught() {
		return caught;
	}

	@Override
	public void resetTimeout(Integer expectedTimeNeeded) {
		if(!done) {
			for(AsyncCallback<T> callback : callbacks) {
				if(callback instanceof AsyncCallbackExtensions)
					((AsyncCallbackExtensions) callback).resetTimeout(expectedTimeNeeded);
			}
		}
	}
}
