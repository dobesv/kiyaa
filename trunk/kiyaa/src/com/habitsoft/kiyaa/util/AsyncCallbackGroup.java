package com.habitsoft.kiyaa.util;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Waits for all of a group of async callbacks to complete and then
 * calls a provided callback with the success/failure status.
 *
 * This is very useful when performing a bunch of independent async 
 * operations that you don't need the specific value from, just the
 * final success/failure result. 
 */
public class AsyncCallbackGroup {
	
	int pending = 0;
	Throwable error;
	boolean ready = false;
	AsyncCallbackGroupMember sharedMember;
	AsyncCallback callback;
	Object callbackParam;
	
	public AsyncCallbackGroup() {
		
	}
	
	public void addPending() {
		pending++;
	}
	
	public void addFailure(Throwable caught) {
		error = caught;
		removePending();
	}

	public void addSuccess(Object result) {
		removePending();
	}

	private void removePending() {
		pending--;
		if(ready && pending == 0) {
			done();
		}
	}

	private void done() {
		onComplete();
	}
	
	public void ready(AsyncCallback callback, Object callbackParam) {
		this.callback = callback;
		this.callbackParam = callbackParam;
		ready();
	}
	
	public void ready(AsyncCallback callback) {
		this.callback = callback;
		ready();
	}
	public void ready() {
		ready = true;
		if(pending == 0) {
			DeferredCommand.addCommand(new Command() {
				public void execute() {
					done();
				}
			});
		}
	}

	public AsyncCallback member() {
		if(sharedMember == null) {
			sharedMember = new AsyncCallbackGroupMember(this);
			return sharedMember;
		} else {
			addPending();
			return sharedMember;
		}
		
	}
	public void onComplete() {
		if(callback != null) {
			if(error != null) {
				callback.onFailure((Throwable)error);
			} else {
				callback.onSuccess(callbackParam);
			}
			callback = null;
		}
	}
	
}
