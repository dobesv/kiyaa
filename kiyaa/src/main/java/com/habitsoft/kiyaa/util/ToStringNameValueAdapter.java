package com.habitsoft.kiyaa.util;


public class ToStringNameValueAdapter<T> implements NameValueAdapter<T> {

	public String getName(T model) {
		if(model == null) return "";
		return String.valueOf(model);
	}

	public String getValue(T model) {
		return String.valueOf(model);
	}

	static NameValueAdapter<Object> instance = new ToStringNameValueAdapter<Object>();
	@SuppressWarnings("unchecked")
	public static <T> NameValueAdapter<T> getInstance() {
		return (NameValueAdapter<T>) instance;
	}
}
