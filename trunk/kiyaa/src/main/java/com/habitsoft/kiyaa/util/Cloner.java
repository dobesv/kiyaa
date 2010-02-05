package com.habitsoft.kiyaa.util;

import java.io.Serializable;
import java.util.Set;

public interface Cloner<T extends Serializable> {

	/**
	 * Create a new instance of T and then
	 * use <code>clone(src, dest)</code> to copy the given
	 * object into it.  Then return that object.
	 */
	public T clone(T src);
	
	/**
	 * Copy all properties from src to dest.
	 * 
	 * <ul>
	 * <li>Native arrays are deep-copied.</li>
	 * <li>Objects with a <code>clone()</code> method will have that called</li>
	 * <li>Everything else is copied as-is using the setter on the target object.</li>
	 * </ul>
	 */
	public void clone(T src, T dest);
	
	/**
	 * Compare the properties of a and b and return a list
	 * of the names of the properties which differ.
	 * 
	 * <ul>
	 * <li>Objects are compared using equals()</li>
	 * <li>Native arrays are compared using Arrays.equals()</li>
	 * <li>Primitive types are compared using ==</li>
	 * </ul>
	 * 
	 * When two arrays differ, the elements are compared one at a time
	 * and any changed, added, or removed element indexes are reported in
	 * <arrayPropery>[<index>] format.
	 */
	public Set<String> diff(T a, T b);
}
