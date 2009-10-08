package com.habitsoft.kiyaa.util;

import com.google.gwt.user.client.rpc.AsyncCallback;

public class AsyncCallbackProxy<T> extends AsyncCallbackFilter<T,T> {
	
	public AsyncCallbackProxy(AsyncCallback<T> delegate, Object marker) {
		super(delegate, marker);
	}
	
	public AsyncCallbackProxy(AsyncCallback<T> delegate) {
	    super(delegate);
	}
	
	public void onSuccess(T result) {
        returnSuccess(result);
	}
}
