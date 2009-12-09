/**
 * 
 */
package com.habitsoft.kiyaa.caching;


public class ChildrenByTypeKey<E extends Enum> implements Comparable<ChildrenByTypeKey> {
	final Long parentId;
	final E type;
	public ChildrenByTypeKey(Long parentId, E type) {
		super();
		this.parentId = parentId;
		this.type = type;
	}
	public int compareTo(ChildrenByTypeKey other) {
		int cmp = parentId.compareTo(other.parentId);
		if(cmp == 0) cmp = type==null?other.type==null?0:-1:other.type==null?1:type.compareTo(other.type);
		return cmp;
	}
}