package com.habitsoft.kiyaa.util;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class ProfilingCallback<T> extends AsyncCallbackDirectProxy<T> {
    final long start = System.currentTimeMillis();
    final String method;
    
    public ProfilingCallback(AsyncCallback<T> delegate, String method) {
        super(delegate);
        this.method = method;
    }

    protected long elapsed() {
        return System.currentTimeMillis() - start;
    }
    /**
     * When we get a failure, query the controller to see whether we should
     * retry, and when.
     */
    @Override
    public final void onFailure(final Throwable caught) {
        Log.debug(method+","+caught.getClass().getName()+","+elapsed());
        super.onFailure(caught);
    }
    
    @Override
    public void onSuccess(T result) {
        Log.debug(method+","+(result==null?"null":result.getClass().getName())+","+elapsed());
        super.onSuccess(result);
    }
}
