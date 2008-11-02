/**
 * 
 */
package com.habitsoft.kiyaa.caching;

import java.util.ArrayList;

import com.google.gwt.user.client.rpc.AsyncCallback;

public class GlobalCachedQuery<V> extends CachedQuery<V> {
    private static ArrayList<GlobalCachedQuery> globalQueries = new ArrayList();
	public GlobalCachedQuery(long refreshInterval) {
		super(refreshInterval);
		globalQueries.add(this);
	}
	
	public AsyncCallback flusher(AsyncCallback callback) {
		if(callback instanceof CachedQueryFlushCallbackProxy) {
			((CachedQueryFlushCallbackProxy)callback).addGlobalQuery(this);
			return callback;
		} else {
			return new CachedQueryFlushCallbackProxy(callback, this);
		}
		
	}

    public static void flushAll() {
        for(GlobalCachedQuery query : globalQueries) {
            query.flush();
        }
    }

    public static void flushAllExpired() {
        for(GlobalCachedQuery query : globalQueries) {
            if(query.isExpired())
                query.flush();
        }
    }
}