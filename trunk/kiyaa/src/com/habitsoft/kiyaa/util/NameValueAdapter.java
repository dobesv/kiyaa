/**
 * 
 */
package com.habitsoft.kiyaa.util;

public interface NameValueAdapter<T> {
	public String getName(T model);
	public String getValue(T model);
}