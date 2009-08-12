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
	    TypedChildrenByOffsetLimitKey x = (TypedChildrenByOffsetLimitKey)other;
		int cmp = type==null?x.type==null?0:-1:x.type==null?1:type.compareTo(x.type);
		if(cmp == 0) return super.compareTo(other);
		return cmp;
	}
}