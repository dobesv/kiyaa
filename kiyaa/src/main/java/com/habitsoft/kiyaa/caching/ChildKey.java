/**
 * 
 */
package com.habitsoft.kiyaa.caching;


public class ChildKey<T extends Comparable<T>> implements Comparable<ChildKey<T>> {
    final Long parentId;
    final T childKey;
    public ChildKey(Long parentId, T childKey) {
    	if(parentId == null) throw new NullPointerException();
    	if(childKey == null) throw new NullPointerException();
        this.parentId = parentId;
        this.childKey = childKey;
    }
    public int compareTo(ChildKey<T> other) {
        int cmp = parentId.compareTo(other.parentId);
        if(cmp == 0) cmp = childKey==null?other.childKey==null?0:-1:childKey.compareTo(other.childKey);
        return cmp;
    }
}