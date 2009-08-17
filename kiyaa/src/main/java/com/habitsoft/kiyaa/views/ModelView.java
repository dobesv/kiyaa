package com.habitsoft.kiyaa.views;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Extension of View for the somewhat common case of a View
 * that has getModel() and setModel().
 */
public interface ModelView<T> extends View {

	public T getModel();
	
	public void setModel(T model, AsyncCallback callback);
}
