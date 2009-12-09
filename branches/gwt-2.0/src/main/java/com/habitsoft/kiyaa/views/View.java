package com.habitsoft.kiyaa.views;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;

/**
 * A user interface component that can load and save its state on demand.
 */
public interface View {
	/**
	 * Reset form fields to the last saved values in the model
	 * @param completionCallback TODO
	 */
	public void load(AsyncCallback<Void> callback);
	
	/**
	 * Get the visual representation of the "form".  Usually returns "this".
	 */
	public Widget getViewWidget();

	/**
	 * Set all field to defaults (NOT the values in the model object)
	 */
	public void clearFields();

	/**
	 * Tell the form to save the current inputs to its model and the
	 * database.
	 */
	public void save(AsyncCallback<Void> callback);

}
