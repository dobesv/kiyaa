package com.habitsoft.kiyaa.util;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface CanValidate {
	/**
	 * If all parts of this widget/form/whatever are valid, return true and
	 * clear error messages.
	 * Otherwise, display appropriate error messages and return false.
	 * @param callback Invoked with a Boolean validation result if validation proceeds without errors; Boolean.FALSE if there were validation errors 
	 * 
	 * @return whether the current user inputs are valid
	 */
	public void validate(AsyncCallback<Boolean> callback);
}
