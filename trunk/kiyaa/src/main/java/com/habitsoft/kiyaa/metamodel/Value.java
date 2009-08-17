package com.habitsoft.kiyaa.metamodel;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface Value {

	public void setValue(Object newValue, AsyncCallback callback);
	
	public void getValue(AsyncCallback callback);
}
