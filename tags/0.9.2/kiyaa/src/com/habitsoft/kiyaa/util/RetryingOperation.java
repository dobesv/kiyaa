package com.habitsoft.kiyaa.util;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;

public abstract class RetryingOperation<T> extends AsyncCallbackProxy<T> {
    final RetryController controller;
    int retriesSoFar=0;
    
    protected RetryingOperation(AsyncCallback delegate, RetryController controller) {
        super(delegate);
        this.controller = controller;
    }

    /**
     * When we get a failure, query the controller to see whether we should
     * retry, and when.
     */
    @Override
    public final void onFailure(final Throwable caught) {
        if(controller.shouldRetry(caught, retriesSoFar)) {
            final int delay = controller.getRetryDelay(caught, retriesSoFar);
            retriesSoFar++;
            if(delay > 0) {
                //GWT.log("Retryable error caught; retrying operation after "+delay+" ms", caught);
                new Timer() {
                    @Override
                    public void run() {
                        perform();
                    }
                }.schedule(delay);
            } else {
                //GWT.log("Retryable error caught; retrying operation immediately", caught);
                perform();
            }
        } else {
            super.onFailure(caught);
        }
    }
    
    /**
     * Subclass should implement this method and use this object as
     * the "callback" parameter for its asyncronous operation.
     */
    public abstract void perform();
}
