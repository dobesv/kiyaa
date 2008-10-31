package com.habitsoft.kiyaa.views;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.habitsoft.kiyaa.metamodel.Action;
import com.habitsoft.kiyaa.util.AsyncCallbackProxy;

/**
 * Action which can load/save a view before/after being performed.
 */
public class ViewAction extends Action {

	View view;
	boolean saveBefore;
	boolean loadAfter;
	Action action;
	
	public ViewAction(Action action, View view, boolean saveBefore, boolean loadAfter) {
		this.view = view;
		this.saveBefore = saveBefore;
		this.loadAfter = loadAfter;
		this.action = action;
	}
	public ViewAction(Action action, View view) {
		this(action, view, true, true);
	}

	@Override
	public void perform(AsyncCallback<Void> callback) {
		performOnView(action, view, saveBefore, loadAfter, callback);
	}

	/**
	 * Tell the view to save, then perform the action, then load.
	 */
	public static void performOnView(final Action action, final View view, boolean saveBefore, boolean loadAfter, AsyncCallback<Void> callback) {
		if(loadAfter) {
			callback = new AsyncCallbackProxy(callback) {
				@Override
				public void onSuccess(Object result) {
					view.load(callback);
				}
			};
		}
		if(action != null)
			callback = action.performOnSuccess(callback);
		if(saveBefore) {
    		view.save(callback);
		} else {
			callback.onSuccess(null);
		}
	}
	
	/**
	 * Wrapper for perform on view that always saves and loads the view, since that is the most common case.
	 */
	public static void performOnView(Action action, View view, AsyncCallback<Void> callback) {
		performOnView(action, view, true, true, callback);
	}
	
	protected View getView() {
		return view;
	}

	protected void setView(View view) {
		this.view = view;
	}

	protected boolean isSaveBefore() {
		return saveBefore;
	}

	protected void setSaveBefore(boolean saveBefore) {
		this.saveBefore = saveBefore;
	}

	protected boolean isLoadAfter() {
		return loadAfter;
	}

	protected void setLoadAfter(boolean loadAfter) {
		this.loadAfter = loadAfter;
	}
	
}
