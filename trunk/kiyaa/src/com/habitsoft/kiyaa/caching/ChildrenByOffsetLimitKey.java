/**
 * 
 */
package com.habitsoft.kiyaa.caching;


public class ChildrenByOffsetLimitKey<T> implements Comparable<ChildrenByOffsetLimitKey<T>> {
	final int limit;
	final int offset;
	final T parentId;
	
	public ChildrenByOffsetLimitKey(T parentId, int offset, int limit) {
		this.parentId = parentId;
		this.offset = offset;
		this.limit = limit;
	}
	
	public int compareTo(ChildrenByOffsetLimitKey<T> other) {
		int cmp = ((Comparable)parentId).compareTo(other.parentId);
		if(cmp == 0) cmp = offset - other.offset;
		if(cmp == 0) cmp = limit - other.limit;
		return cmp;
	}
}