package com.habitsoft.kiyaa.views;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.habitsoft.kiyaa.metamodel.Value;
import com.habitsoft.kiyaa.util.AsyncCallbackDirectProxy;
import com.habitsoft.kiyaa.util.AsyncCallbackProxy;

/**
 * Create a scoped variable with a given value.  Useful if you are
 * accessing a value loaded using an AsyncCallback and you just want
 * to load it once.
 * 
 * Example: <k:with value="${expr}" with-model="com.package.ModelType model"/>
 */
public class WithView implements View {

	ModelView<Object> view;
	Value value;
	
	public void clearFields() {
		if(view != null) {
			view.clearFields();
		}
	}

	public Widget getViewWidget() {
		if(view == null) return new Label();
		return view.getViewWidget();
	}

	public void load(AsyncCallback<Void> callback) {
		if(view != null) {
			if(value != null) {
				value.getValue(new AsyncCallbackProxy<Object,Void>(callback) {
					@Override
					public void onSuccess(Object result) {
						view.setModel(result, new AsyncCallbackDirectProxy<Void>(takeCallback()) {
							@Override
							public void onSuccess(Void result) {
								view.load(takeCallback());
							}
						});
					}
				});
			} else {
				view.load(callback);
			}
		} else {
			callback.onSuccess(null);
		}
	}

	public void save(AsyncCallback<Void> callback) {
		if(view != null) {
			view.save(callback);
		} else {
			callback.onSuccess(null);
		}
	}

	public View getView() {
		return view;
	}

	public Value getValue() {
		return value;
	}

	public void setValue(Value value) {
		this.value = value;
	}

	public void setView(ModelView<Object> view) {
		this.view = view;
	}

}
