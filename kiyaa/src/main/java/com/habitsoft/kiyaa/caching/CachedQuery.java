/**
 * 
 */
package com.habitsoft.kiyaa.caching;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.habitsoft.kiyaa.util.AsyncCallbackShared;

/**
 * Represents a cached (or cacheable) individual query.  The query is
 * identified by a composite key of the parameters of the query; the
 * query's "type" is typically identified by which CacheMap the query
 * is in, or which GlobalCachedQuery this is an instance of.
 * 
 * Expiry times should take into consideration:
 * 
 *  - How likely is this to be re-used?  More likely = more time
 *  - How soon is this likely to be re-used?  If longer than a reasonable
 *    amount, shorten the expiry to save memory, otherwise lengthen it
 *    to increase the chances of a cache hit.
 *  - How often will this be changed by someone else?  Try to set something
 *    that will show changes by others in a reasonable time frame. 
 */
public class CachedQuery<V> {
    public static final int VERY_LONG_EXPIRY = 24*60*60*1000;
    public static final int LONG_EXPIRY = 60*60*1000;
    public static final int MEDIUM_EXPIRY = 5*60*1000;
    public static final int SHORT_EXPIRY = 60*1000;
    public static final int VERY_SHORT_EXPIRY = 5*1000;
    
	long expiry=0;
	Throwable failure;
	AsyncCallbackShared<V> fetchInProgress;
	final long refreshInterval;
	V result;
	boolean inFetch;
	
	public CachedQuery(long refreshInterval) {
		this.refreshInterval = refreshInterval;
	}
	
	/**
	 * Try to fetch the given query from the cache.
	 * 
	 * Returns null if a cached result was returned to the
	 * given callback, otherwise a callback is returned.
	 */
	public AsyncCallback<V> fetch(final AsyncCallback<V> callback) {
		if(fetchInProgress != null) {
			if(callback != null)
				fetchInProgress.addCallback(callback);
			return null;
		} else if(System.currentTimeMillis() > expiry) {
			return saver(callback);
		} else if(failure != null) {		    
			if(callback != null) {
				if(inFetch) {
				    final Throwable failureToReturn = failure;
					DeferredCommand.addCommand(new Command() {
						public void execute() {
							callback.onFailure(failureToReturn);
						}
					});
				} else {
					inFetch=true;
					callback.onFailure(failure);
					inFetch=false;
				}
			}
			return null;
		} else {
			//expiry = System.currentTimeMillis() + refreshInterval;
			
			// Try to avoid cyclic callbacks - sometimes we'll call someone back and they'll
			// call someone else back that tries to fetch this same thing from the cache.  This
			// can result is very deep call stacks.
		    final V resultToReturn = result;
			if(callback != null) {
				if(inFetch) {
					DeferredCommand.addCommand(new Command() {
						public void execute() {
							callback.onSuccess(resultToReturn);
						}
					});
				} else {
					inFetch = true;
					callback.onSuccess(result);
					inFetch = false;
				}
			}
			return null;
		}
	}

	/**
	 * Ensure that the next call will fetch from the cache
	 */
	public void flush() {
		failure = null;
		fetchInProgress = null;
		expiry = 0;
		result = null;
	}
	
	/**
	 * Return true if the cached query has expired.
	 */
	public boolean isExpired() {
		return fetchInProgress == null && System.currentTimeMillis() > expiry;
	}
	
	/**
	 * When calling a method which saves or creates a new object,
	 * use "saver" to store the async result into this cache entry.
	 */
	public AsyncCallback saver(AsyncCallback<V> callback) {
		fetchInProgress = new AsyncCallbackShared<V>(callback) {
			@Override
			public void onFailure(Throwable caught) {
				store(null);
				failure = caught;
				expiry = System.currentTimeMillis() + 100;
				super.onFailure(caught);
			}
			@Override
			public void onSuccess(V result) {
				store(result);
				super.onSuccess(result);
			}
		};
		return fetchInProgress;
	}

	/**
	 * When an object is fetched as part of another operation,
	 * we don't need to do any lazy fetching.  So, this stores
	 * a prefetched result.
	 * 
	 * This typically happens when fetching arrays of objects, as
	 * well as when fetching objects that contain other objects.
	 */
	public void store(V obj) {
		expiry = System.currentTimeMillis() + refreshInterval;
		this.result = obj;
		this.failure = null;
		fetchInProgress = null;
	}

	/**
	 * Return whatever object we last fetched, if any.
	 * 
	 * Used to access/update the results of a previous fetch so
	 * we can process actions that update just a single field
	 * of the object.
	 */
	public V tryFetch() {
		return result;
	}
	
}