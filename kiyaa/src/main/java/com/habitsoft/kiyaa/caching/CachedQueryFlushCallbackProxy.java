package com.habitsoft.kiyaa.caching;

import java.util.ArrayList;
import java.util.HashSet;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.habitsoft.kiyaa.util.AsyncCallbackProxy;

/**
 * An AsyncCallbackProxy that flushes cached entries on success.  The 
 * "caches" are flushed entirely, whereas "keys" represents individual
 * keys to flush.  "globalQueries" are global values/lists which will be
 * flushed.
 * 
 * This is used to flush out stale data after updating a record.  If the
 * operation fails, the data is assumed to not be invalidated.
 * 
 */
public class CachedQueryFlushCallbackProxy<K,V> extends AsyncCallbackProxy<V> {
	class CacheKey {
		final CacheMap<K,V> cache;
		final Object key;
		private CacheKey(Object key, CacheMap<K,V> cache) {
			this.key = key;
			this.cache = cache;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			final CacheKey other = (CacheKey) obj;
			if (cache == null) {
				if (other.cache != null)
					return false;
			} else if (!cache.equals(other.cache))
				return false;
			if (key == null) {
				if (other.key != null)
					return false;
			} else if (!key.equals(other.key))
				return false;
			return true;
		}
		public CacheMap<K,V> getCache() {
			return cache;
		}
		public Object getKey() {
			return key;
		}
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((cache == null) ? 0 : cache.hashCode());
			result = prime * result + ((key == null) ? 0 : key.hashCode());
			return result;
		}
		
	}
	final ArrayList<CacheMap<K,V>> caches = new ArrayList<CacheMap<K,V>>();
	
	final ArrayList<GlobalCachedQuery<V>> globalQueries = new ArrayList<GlobalCachedQuery<V>>();
	final HashSet<CacheKey> keys = new HashSet<CacheKey>();
	public CachedQueryFlushCallbackProxy(AsyncCallback<V> callback, CacheMap<K,V> cache) {
		super(callback);
		caches.add(cache);
	}

	public CachedQueryFlushCallbackProxy(AsyncCallback<V> callback, CacheMap<K,V> map, K key) {
		super(callback);
		keys.add(new CacheKey(key, map));
	}

	public CachedQueryFlushCallbackProxy(AsyncCallback<V> callback, GlobalCachedQuery<V> query) {
		super(callback);
		globalQueries.add(query);
	}
	
	public void addCache(CacheMap<K,V> cache) {
		caches.add(cache);
	}
	
	public void addGlobalQuery(GlobalCachedQuery<V> query) {
		globalQueries.add(query);
	}
	
	public void addKey(CacheMap<K,V> map, Object key) {
		keys.add(new CacheKey(key, map));
	}
	
	@Override
	public void onSuccess(V result) {
		for(CacheMap<K,V> cache : caches) {
			cache.clear();
		}
		for(GlobalCachedQuery<V> query : globalQueries) {
			query.flush();
		}
		for(CacheKey entry : keys) {
			entry.getCache().remove(entry.getKey());
		}
		super.onSuccess(result);
	}
}