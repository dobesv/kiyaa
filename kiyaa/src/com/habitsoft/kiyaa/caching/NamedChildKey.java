/**
 * 
 */
package com.habitsoft.kiyaa.caching;


public class NamedChildKey implements Comparable<NamedChildKey> {
	final String childName;
	final Long parentId;
	public NamedChildKey(Long parent, String childName) {
		super();
		this.parentId = parent;
		this.childName = childName==null?"":childName;
	}

	public int compareTo(NamedChildKey other) {
		int cmp = parentId.compareTo(other.parentId);
		if(cmp == 0) cmp = childName.compareToIgnoreCase(other.childName);
		return cmp;
	}
	
}