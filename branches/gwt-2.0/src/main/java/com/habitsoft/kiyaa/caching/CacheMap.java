/**
 * 
 */
package com.habitsoft.kiyaa.caching;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class CacheMap<K,V> extends TreeMap<K,CachedQuery<V>> {
	private static final long serialVersionUID = 1L;
    private static final ArrayList<CacheMap<?,?>> allCaches = new ArrayList<CacheMap<?,?>>();
	private static final Timer timer = new Timer() {
	    @Override
	    public void run() {
	        flushAllExpired();
	    }  
	};
	final long refreshInterval;
	
	public CacheMap() {
		this(CachedQuery.MEDIUM_EXPIRY);
	}
	
	public CacheMap(long refreshInterval) {
	    if(allCaches.isEmpty())
	        timer.scheduleRepeating(CachedQuery.VERY_SHORT_EXPIRY);
		allCaches.add(this);
		this.refreshInterval = refreshInterval;
	}

	/**
	 * Try to fetch the given query from the cache.
	 * 
	 * Returns null if a cached result was returned to the
	 * given callback, otherwise a callback is returned.
	 */
	public AsyncCallback<V> fetch(K key, AsyncCallback<V> callback) {
		return getOrCreateQuery(key).fetch(callback);
	}

	public <T> AsyncCallback<T> flusher(AsyncCallback<T> callback) {
		if(callback instanceof CachedQueryFlushCallbackProxy<?>) {
			// Not sure how to get around this one ...
			((CachedQueryFlushCallbackProxy<T>)callback).addCache(this);
			return callback;
		} else {
			return new CachedQueryFlushCallbackProxy<T>(callback, this);
		}
	}
	
	public <T> AsyncCallback<T> flusher(K key, AsyncCallback<T> callback) {
		if(callback instanceof CachedQueryFlushCallbackProxy<?>) {
			((CachedQueryFlushCallbackProxy<T>)callback).addKey(this, key);
			return callback;
		} else {
			return new CachedQueryFlushCallbackProxy<T>(callback, this, key);
		}
	}

	public void flushExpired() {
		for (Iterator<Map.Entry<K,CachedQuery<V>>> i = entrySet().iterator(); i.hasNext();) {
			Map.Entry<K,CachedQuery<V>> entry = (Map.Entry<K,CachedQuery<V>>)i.next(); 
			CachedQuery<V> query = (CachedQuery<V>) entry.getValue();
			if(query.isExpired()) {
				i.remove();
			}
		}
	}
	
	protected CachedQuery<V> getOrCreateQuery(K key) {
		CachedQuery<V> query = (CachedQuery<V>)get(key);
		if(query == null) {
			//GWT.log("Cache miss on "+key, null);
			query = new CachedQuery<V>(refreshInterval);
			put(key, query);
		} else if(query.isExpired()) {
			//GWT.log("Expired cache entry on "+key, null);
		}
		return query;
	}

	public void store(K key, V obj) {
		CachedQuery<V> q = getOrCreateQuery(key);
		q.store(obj);
	}
	
	public Object tryFetch(K key) {
		CachedQuery<?> query = (CachedQuery<?>)get(key);
		if(query == null) {
			return null;
		}
		return query.tryFetch();
	}

    public static void flushAllCaches() {
        for(CacheMap<?,?> map : allCaches) {
            map.clear();
        }
    }

    public static void flushAllExpired() {
        for(CacheMap<?,?> map : allCaches) {
            map.flushExpired();
        }
    }

    public static ArrayList<CacheMap<?,?>> getAllCaches() {
        return allCaches;
    }
}