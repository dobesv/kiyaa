/**
 * 
 */
package com.habitsoft.kiyaa.caching;


public class TypedChildrenKey<E extends Enum> implements Comparable<TypedChildrenKey>{
	private Long parentId;
	private E type;
	public TypedChildrenKey(Long parentId, E type) {
		this.parentId = parentId;
		this.type = type;
	}
	public int compareTo(TypedChildrenKey o) {
		int cmp = parentId.compareTo(o.parentId);
		if(cmp == 0) cmp = type.compareTo(o.type);
		return cmp;
	}
}