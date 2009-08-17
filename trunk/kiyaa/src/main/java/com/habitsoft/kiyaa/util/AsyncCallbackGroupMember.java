package com.habitsoft.kiyaa.util;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class AsyncCallbackGroupMember<T> implements AsyncCallback<T> {

	protected final AsyncCallbackGroup group;
	protected final Object marker;
	
	public AsyncCallbackGroupMember(AsyncCallbackGroup group, Object marker) {
		super();
		this.group = group;
		this.marker = marker;
		group.addPending();
	}
	
	public AsyncCallbackGroupMember(AsyncCallbackGroup group) {
	    this(group, null);
    }

	public void onFailure(Throwable caught) {
        if(marker != null && (caught instanceof Error || caught instanceof RuntimeException)) try {
            Log.error("At "+marker.toString(), caught);
        } catch(Exception e) { }
		//com.google.gwt.core.client.GWT.log("Group member failure "+group, location);
		group.addFailure(caught);
	}

	public void onSuccess(T param) {
		//com.google.gwt.core.client.GWT.log("Group member success "+group, location);
		group.addSuccess(param);
	}

}
