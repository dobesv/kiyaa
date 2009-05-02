package com.habitsoft.kiyaa.views;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.habitsoft.kiyaa.util.AsyncCallbackFactory;
import com.habitsoft.kiyaa.util.AsyncCallbackShared;
import com.habitsoft.kiyaa.util.AsyncCallbackWithTimeout;

/**
 * View which returns immediately from load() without waiting for its
 * subview(s) to load.
 */
public class NoWaitView implements View {

	View view;
	AsyncCallbackShared loadInProgress;
	
	public void clearFields() {
		view.clearFields();
	}

	public Widget getViewWidget() {
		return view.getViewWidget();
	}

	public void load(AsyncCallback callback) {
		callback.onSuccess(null);
		loadInProgress = new AsyncCallbackShared(AsyncCallbackFactory.defaultNewInstance()) {
			@Override
			public void onSuccess(Object result) {
				loadInProgress = null;
				super.onSuccess(result);
			}
			@Override
			public void onFailure(Throwable caught) {
				loadInProgress = null;
				super.onFailure(caught);
			}
		};
		view.load(new AsyncCallbackWithTimeout(loadInProgress, view));
	}

	public void save(final AsyncCallback callback) {
		if(loadInProgress != null) {
			loadInProgress.addCallback(new AsyncCallback() {
			
				public void onSuccess(Object result) {
					view.save(callback);
				}
			
				public void onFailure(Throwable caught) {
					callback.onFailure(caught);
				}
			});
		} else {
			view.save(callback);
		}
	}

	public View getView() {
		return view;
	}

	public void setView(View view) {
		this.view = view;
	}

}
