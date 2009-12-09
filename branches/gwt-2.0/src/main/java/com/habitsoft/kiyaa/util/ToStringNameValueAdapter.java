package com.habitsoft.kiyaa.util;


public class ToStringNameValueAdapter implements NameValueAdapter {

	public String getName(Object model) {
		if(model == null) return "";
		return String.valueOf(model);
	}

	public String getValue(Object model) {
		return String.valueOf(model);
	}

	static NameValueAdapter instance = new ToStringNameValueAdapter();
	public static NameValueAdapter getInstance() {
		return instance;
	}
}
