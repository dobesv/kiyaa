package com.habitsoft.kiyaa.util;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Wrap an async callback with a timeout, so that if the function we're
 * expecting to complete doesn't call us back in a reasonable time we
 * can fire a failure callback.  This is meant as a debugging tool rather
 * than something to use for timeouts in a live situation - it helps find
 * cases where you've forgotten to call the callback.
 * 
 * @author dobes
 *
 */
public class AsyncCallbackWithTimeout<T> extends AsyncCallbackDirectProxy<T> {
	boolean complete;
	Timer timer;
	Error timeout;
	public AsyncCallbackWithTimeout(AsyncCallback<T> callback, int timeoutMillis, Object marker) {
		super(callback, marker);
		timer = new Timer() {
			@Override
			public void run() {
				onTimeout();
			}
		};
		timer.schedule(timeoutMillis);
		timeout = new TimeOutException();
	}
	public AsyncCallbackWithTimeout(AsyncCallback<T> callback, Object marker) {
		this(callback, 120000, marker);
	}
	@Override
	public void onFailure(Throwable caught) {
		if(!complete) { // If we already timed out, don't pass any events through
			complete = true;
			timer.cancel();
			super.onFailure(caught);
		} else {
			GWT.log("Discarded error because the callback has timed out, or the callback was already invoked: "+caught, caught);
			GWT.log("Callback created here", timeout);
		}
	}

	@Override
	public void onSuccess(T result) {
		if(!complete) {
			complete = true;
			timer.cancel();
			super.onSuccess(result);
		} else {
			GWT.log("Discarded result because the callback has timed out: "+result, timeout);
			GWT.log("Backtrace for callback", new Error());
		}
	}

	static class TimeOutException extends Error {
		private static final long serialVersionUID = 1L;
		TimeOutException() {
			super("An operation timed out; this may be caused by a slow or unreliable network connection, a server outage, or a bug.");
		}		
	}
	public void onTimeout() {
		if(!complete) {
			complete = true;
			super.onFailure(timeout);
		}
	}
}