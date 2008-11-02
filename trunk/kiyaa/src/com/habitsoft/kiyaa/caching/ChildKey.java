/**
 * 
 */
package com.habitsoft.kiyaa.caching;


public class ChildKey<T extends Comparable<T>> implements Comparable<ChildKey<T>> {
    final Long parentId;
    final T childKey;
    public ChildKey(Long parentId, T childKey) {
        this.parentId = parentId;
        this.childKey = childKey;
    }
    public int compareTo(ChildKey<T> other) {
        int cmp = parentId.compareTo(other.parentId);
        if(cmp == 0) cmp = childKey.compareTo(other.childKey);
        return cmp;
    }
}