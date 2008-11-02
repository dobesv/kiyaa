/**
 * 
 */
package com.habitsoft.kiyaa.caching;


public class ChildrenByOffsetLimitKey implements Comparable<ChildrenByOffsetLimitKey> {
	final int limit;
	final int offset;
	final Long parentId;
	
	public ChildrenByOffsetLimitKey(Long parentId, int offset, int limit) {
		this.parentId = parentId;
		this.offset = offset;
		this.limit = limit;
	}
	
	public int compareTo(ChildrenByOffsetLimitKey other) {
		int cmp = parentId.compareTo(other.parentId);
		if(cmp == 0) cmp = offset - other.offset;
		if(cmp == 0) cmp = limit - other.limit;
		return cmp;
	}
}