package com.habitsoft.kiyaa.util;

public class AsyncCallbackGroupResultCachingMember<T> extends AsyncCallbackGroupMember<T>{
	private T result;

	public AsyncCallbackGroupResultCachingMember(AsyncCallbackGroup group) {
		super(group);
	}
	
	@Override
	public void onSuccess(T param) {
		result = param;
		super.onSuccess(param);
	}
	
	public T getResult() {
		return result;
	}
	public void setResult(T result) {
		this.result = result;
	}
	
}
