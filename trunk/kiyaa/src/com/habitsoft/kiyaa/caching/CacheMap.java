/**
 * 
 */
package com.habitsoft.kiyaa.caching;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import com.google.gwt.user.client.rpc.AsyncCallback;

public class CacheMap<K,V> extends TreeMap<K,CachedQuery<V>> {
	private static final long serialVersionUID = 1L;
    private static ArrayList<CacheMap> allCaches = new ArrayList();
	
	final long refreshInterval;
	
	public CacheMap() {
		this(CachedQuery.MEDIUM_EXPIRY);
	}
	
	public CacheMap(long refreshInterval) {
		allCaches.add(this);
		this.refreshInterval = refreshInterval;
	}

	/**
	 * Try to fetch the given query from the cache.
	 * 
	 * Returns null if a cached result was returned to the
	 * given callback, otherwise a callback is returned.
	 */
	public AsyncCallback fetch(K key, AsyncCallback callback) {
		return getOrCreateQuery(key).fetch(callback);
	}

	public AsyncCallback flusher(AsyncCallback callback) {
		if(callback instanceof CachedQueryFlushCallbackProxy) {
			((CachedQueryFlushCallbackProxy)callback).addCache(this);
			return callback;
		} else {
			return new CachedQueryFlushCallbackProxy(callback, this);
		}
	}
	
	public AsyncCallback flusher(K key, AsyncCallback callback) {
		if(callback instanceof CachedQueryFlushCallbackProxy) {
			((CachedQueryFlushCallbackProxy)callback).addKey(this, key);
			return callback;
		} else {
			return new CachedQueryFlushCallbackProxy(callback, this, key);
		}
	}

	public void flushExpired() {
		for (Iterator i = entrySet().iterator(); i.hasNext();) {
			Map.Entry entry = (Map.Entry)i.next(); 
			CachedQuery query = (CachedQuery) entry.getValue();
			if(query.isExpired()) {
				i.remove();
			}
		}
	}
	
	protected CachedQuery getOrCreateQuery(K key) {
		CachedQuery query = (CachedQuery)get(key);
		if(query == null) {
			//GWT.log("Cache miss on "+key, null);
			query = new CachedQuery(refreshInterval);
			put(key, query);
		} else if(query.isExpired()) {
			//GWT.log("Expired cache entry on "+key, null);
		}
		return query;
	}

	public void store(K key, V obj) {
		getOrCreateQuery(key).store(obj);
	}
	
	public Object tryFetch(K key) {
		CachedQuery query = (CachedQuery)get(key);
		if(query == null) {
			return null;
		}
		return query.tryFetch();
	}

    public static void flushAllCaches() {
        for(CacheMap map : allCaches) {
            map.clear();
        }
    }

    public static void flushAllExpired() {
        for(CacheMap map : allCaches) {
            map.flushExpired();
        }
    }
}