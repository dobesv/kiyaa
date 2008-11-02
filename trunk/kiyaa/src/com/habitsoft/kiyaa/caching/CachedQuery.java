/**
 * 
 */
package com.habitsoft.kiyaa.caching;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.habitsoft.kiyaa.util.AsyncCallbackShared;

public class CachedQuery<V> {
    /**
     * Expiry times should take into consideration:
     * 
     *  - How likely is this to be re-used?  More likely = more time
     *  - How soon is this likely to be re-used?  If longer than a reasonable
     *    amount, shorten the expiry to save memory, otherwise lengthen it
     *    to increase the chances of a cache hit.
     *  - How often will this be changed by someone else?  Try to set something
     *    that will show changes by others in a reasonable time frame. 
     */
    public static final long VERY_LONG_EXPIRY = 24*60*60*1000;
    public static final long LONG_EXPIRY = 60*60*1000;
    public static final long MEDIUM_EXPIRY = 5*60*1000;
    public static final long SHORT_EXPIRY = 60*1000;
    public static final long VERY_SHORT_EXPIRY = 5*1000;
    
	long expiry=0;
	Throwable failure;
	AsyncCallbackShared fetchInProgress;
	final long refreshInterval;
	V result;
	
	
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
				DeferredCommand.addCommand(new Command() {
					public void execute() {
						callback.onFailure(failure);
					}
				});
			}
			return null;
		} else {
			//expiry = System.currentTimeMillis() + refreshInterval;
			
			// Run the onSuccess in a new call stack to more closely emulate the behavior of a server
			// call.  Otherwise we get weird problems when the callback calls back to us in a cycle
			// before we've cleaned up our state.
			if(callback != null) {
				DeferredCommand.addCommand(new Command() {
					public void execute() {
						callback.onSuccess(result);
					}
				});
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
	public AsyncCallback saver(AsyncCallback callback) {
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