package com.habitsoft.kiyaa.util;

import com.google.gwt.user.client.rpc.AsyncCallback;

public class AsyncCallbackDirectProxy<T> extends AsyncCallbackProxy<T,T> {
	
	public AsyncCallbackDirectProxy(AsyncCallback<T> delegate, Object marker) {
		super(delegate, marker);
	}
	
	public AsyncCallbackDirectProxy(AsyncCallback<T> delegate) {
	    super(delegate);
	}
	
	public void onSuccess(T result) {
        returnSuccess(result);
	}
}
