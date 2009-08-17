/**
 * 
 */
package com.habitsoft.kiyaa.util;

/**
 * Object used to adapt objects for use in a listbox, since listbox
 * doesn't allow you store objects as their value, only strings.
 */
public interface NameValueAdapter<T> {
    /**
     * Return the label to use for the list item.  If the model is null,
     * this should return a non-null string, such as "".
     */
	public String getName(T model);
	
	/**
	 * Return the value to use for the list item.  If the model is null,
	 * this should return a non-null string, such as "null".
	 * 
	 * Note that null listbox values work with some browsers but not others,
	 * so just don't do it and you'll avoid some weirdness.
	 */
	public String getValue(T model);
}