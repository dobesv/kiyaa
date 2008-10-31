package com.habitsoft.kiyaa.util;

import com.google.gwt.user.client.rpc.AsyncCallback;

public class AsyncCallbackGroupMember<T> implements AsyncCallback<T> {

	protected final AsyncCallbackGroup group;
	
	public AsyncCallbackGroupMember(AsyncCallbackGroup group) {
		super();
		this.group = group;
		group.addPending();
	}

	public void onFailure(Throwable caught) {
		//com.google.gwt.core.client.GWT.log("Group member failure "+group, location);
		group.addFailure(caught);
	}

	public void onSuccess(T param) {
		//com.google.gwt.core.client.GWT.log("Group member success "+group, location);
		group.addSuccess(param);
	}

}
