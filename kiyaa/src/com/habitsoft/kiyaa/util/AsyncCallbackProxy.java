package com.habitsoft.kiyaa.util;

import com.google.gwt.user.client.rpc.AsyncCallback;

public class AsyncCallbackProxy<T> implements AsyncCallback<T> {
	protected final AsyncCallback callback;

	public AsyncCallbackProxy(AsyncCallback delegate) {
		super();
		//if(!(this instanceof AsyncCallbackWithTimeout) && !(delegate instanceof AsyncCallbackWithTimeout))
		//	delegate = new AsyncCallbackWithTimeout<T>(delegate);
		this.callback = delegate;
	}

	public void onFailure(Throwable caught) {
		if(callback != null)
			callback.onFailure(caught);
	}

	public void onSuccess(T result) {
		if(callback != null)
			callback.onSuccess(result);
	}
	

}
