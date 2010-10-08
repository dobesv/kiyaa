/**
 * 
 */
package com.habitsoft.kiyaa.views;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.habitsoft.kiyaa.util.NameValueAdapter;
import com.habitsoft.kiyaa.widgets.Label;

final class NameViewFactory<T> implements ViewFactory<ModelView<T>> {
	public class ModelNameView implements ModelView<T> {
		T model;
		Label label = new Label();

		public void save(AsyncCallback<Void> callback) {
			callback.onSuccess(null);
		}

		public void load(AsyncCallback<Void> callback) {
			label.setText(nameValueAdapter.getName(model));
			callback.onSuccess(null);
		}

		public Widget getViewWidget() {
			return label;
		}

		public void clearFields() {
			label.setText("");
		}

		public void setModel(T model, AsyncCallback<Void> callback) {
			this.model = model;
			load(callback);
		}

		public T getModel() {
			return model;
		}
	}

	final NameValueAdapter<T> nameValueAdapter;
	
	public NameViewFactory(NameValueAdapter<T> nameValueAdapter) {
		super();
		this.nameValueAdapter = nameValueAdapter;
	}

	public ModelNameView createView() {
		return new ModelNameView();
	}
}