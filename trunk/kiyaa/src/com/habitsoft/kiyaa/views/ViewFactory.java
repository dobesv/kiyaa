package com.habitsoft.kiyaa.views;


public interface ViewFactory<T extends View> {

	public T createView();
	
}
