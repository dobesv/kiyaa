package com.habitsoft.kiyaa.metamodel;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface Value<T> {

	public void setValue(T newValue, AsyncCallback<Void> callback);
	
	public void getValue(AsyncCallback<T> callback);
}
