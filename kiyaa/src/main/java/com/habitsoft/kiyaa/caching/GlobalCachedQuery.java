/**
 * 
 */
package com.habitsoft.kiyaa.caching;

import java.util.ArrayList;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class GlobalCachedQuery<V> extends CachedQuery<V> {
    private static final ArrayList<GlobalCachedQuery<?>> globalQueries = new ArrayList<GlobalCachedQuery<?>>();
    private static final Timer timer = new Timer() {
        @Override
        public void run() {
            flushAllExpired();
        };
    };
    
	public GlobalCachedQuery(long refreshInterval) {
		super(refreshInterval);
		if(globalQueries.isEmpty())
		    timer.scheduleRepeating(VERY_SHORT_EXPIRY);
		globalQueries.add(this);
	}
	
	public <T> AsyncCallback<T> flusher(AsyncCallback<T> callback) {
		if(callback instanceof CachedQueryFlushCallbackProxy<?>) {
			((CachedQueryFlushCallbackProxy<?>)callback).addGlobalQuery(this);
			return callback;
		} else {
			return new CachedQueryFlushCallbackProxy<T>(callback, this);
		}
		
	}

    public static void flushAll() {
        for(GlobalCachedQuery<?> query : globalQueries) {
            query.flush();
        }
    }

    public static void flushAllExpired() {
        for(GlobalCachedQuery<?> query : globalQueries) {
            if(query.isExpired())
                query.flush();
        }
    }

    public static ArrayList<GlobalCachedQuery<?>> getGlobalQueries() {
        return globalQueries;
    }
}