package com.habitsoft.kiyaa.util;

public interface ModelFilter<T> {
	/**
	 * Return true if the given model matches the filter represented by this filter.
	 */
	public boolean includes(T model) ;
}
