/**
 * 
 */
package com.habitsoft.kiyaa.caching;


public class TypedChildrenByOffsetLimitKey<E extends Enum> extends ChildrenByOffsetLimitKey {
	private E type;

	public TypedChildrenByOffsetLimitKey(Long parentId, E type, int offset, int limit) {
		super(parentId, offset, limit);
		this.type = type;
	}
	
	@Override
	public int compareTo(ChildrenByOffsetLimitKey other) {
		int cmp = type.compareTo(((TypedChildrenByOffsetLimitKey)other).type);
		if(cmp == 0) return super.compareTo(other);
		return cmp;
	}
}