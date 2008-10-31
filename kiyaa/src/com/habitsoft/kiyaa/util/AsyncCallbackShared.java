package com.habitsoft.kiyaa.util;

import java.util.ArrayList;

import com.google.gwt.user.client.rpc.AsyncCallback;

public class AsyncCallbackShared<T> implements AsyncCallback<T> {

	ArrayList<AsyncCallback<T>> callbacks = new ArrayList<AsyncCallback<T>>();
	boolean done = false;
	T result = null;
	Throwable caught = null;
	
	public AsyncCallbackShared() {
	}
	
	public AsyncCallbackShared(AsyncCallback<T> firstCallback) {
		callbacks.add(firstCallback);
	}
	
	public void addCallback(AsyncCallback<T> callback) {
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
		for (AsyncCallback<T> callback : callbacks) {
			fireCallback(callback);
		}
	}
	public void onFailure(Throwable caught) {
		this.caught = caught;
		this.done = true;
		this.fireCallbacks();
	}
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

}
