/**
 * 
 */
package com.habitsoft.kiyaa.caching;


public class TypedChildrenByOffsetLimitKey<T, E extends Enum> extends ChildrenByOffsetLimitKey<T> {
	private E type;

	public TypedChildrenByOffsetLimitKey(T parentId, E type, int offset, int limit) {
		super(parentId, offset, limit);
		this.type = type;
	}
	
	@Override
	public int compareTo(ChildrenByOffsetLimitKey<T> other) {
		int cmp = type.compareTo(((TypedChildrenByOffsetLimitKey)other).type);
		if(cmp == 0) return super.compareTo(other);
		return cmp;
	}
}