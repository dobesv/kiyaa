package com.habitsoft.kiyaa.metamodel;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * Collection-like class meant for use in in the display system.
 * 
 * This interface takes into account that the collection is often,
 * but not always, backed by a web service which must be accessed
 * asynchronously, and that the collection is sometimes much larger
 * than what you really want to display.  Many common collection
 * operations are omitted because they are potentially expensive.
 *
 */
public interface ModelCollection<T> {
	/**
	 * Access part of the collection; accesses to the collection are
	 * assumed to be paged.  After all, who would be crazy enough to view the
	 * entire collection at once?
	 * 
	 * There a possibility when paging through the data that objects
	 * will be inserted into the middle of the "offset" space, in
	 * which case an item could be returned twice or never.  The caller
	 * should not assume that this won't happen.
	 * 
	 * The callback is passed an array of model objects. (Object[])
	 */
	void load(int offset, int limit, AsyncCallback<T []> callback);
	
	/**
	 * Fetch the total number of items in the collection; returns null if
	 * the total is not known or if the collection has unbounded size.
	 * 
	 * This is used to determine whether the current page is the last page.
	 */
	void getLength(AsyncCallback<Integer> callback);

	/**
	 * Return a long value indicating the underlying ID used to load this
	 * collection.  This is used by collection views to decide whether to
	 * reload fully, or just update incrementally.
	 */
    Object getId();
}
