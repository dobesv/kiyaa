package com.habitsoft.kiyaa.views;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.habitsoft.kiyaa.util.AsyncCallbackProxy;
import com.habitsoft.kiyaa.widgets.Feedback;

/**
 * Utility view that takes a view as a parameter and shows it.  This
 * is useful in a template where the view would be set programmatically;
 * you can make the view an attribute on the GeneratedHTMLView base class
 * and assign it into the view attribute of this class.
 */
public class DynamicView implements View {

	View view;
	SimplePanel wrapper = new SimplePanel();
	boolean viewChanged = false;
	String loadingHtml = null;
	
	public View getView() {
		return view;
	}

	public void setView(View view) {
		if(view != this.view) {
			this.view = view;
			viewChanged = true;
			if(view != null && loadingHtml != null) {
				wrapper.setWidget(new HTML(loadingHtml));
			} else {
				wrapper.clear();
			}
		}
	}

	public void clearFields() {
		if(view != null)
			view.clearFields();
	}

	public Widget getViewWidget() {
		return wrapper;
	}

	public void load(AsyncCallback<Void> callback) {
		if(view != null) {
			view.load(new AsyncCallbackProxy<Void>(callback) {
				@Override
				public void onSuccess(Void result) {
					show();
					super.onSuccess(result);
				}

                private void show() {
                    if(viewChanged) {
						wrapper.setWidget(view.getViewWidget());
						viewChanged = false;
					}
                }
				
				@Override
				public void onFailure(Throwable caught) {
                    Log.error("Error loading view "+view, caught);
                    displayError(caught);
					super.onSuccess(null);
				}
			});
		} else {
			callback.onSuccess(null);
		}
	}

	public void save(AsyncCallback<Void> callback) {
		if(view != null) {
			view.save(new AsyncCallbackProxy<Void>(callback) {
			    @Override
			    public void onFailure(Throwable caught) {
			        Log.error("Error saving view "+view, caught);
			        super.onSuccess(null);
			    }
			});
		} else {
			callback.onSuccess(null);
		}
	}

	public String getLoadingHtml() {
		return loadingHtml;
	}

	public void setLoadingHtml(String loadingHtml) {
		this.loadingHtml = loadingHtml;
		if(wrapper.getWidget() == null && view != null) {
			wrapper.setWidget(new HTML(loadingHtml));
		}
	}
	
	public void setLoadingImage(String src) {
		setLoadingHtml("<img src=\""+src+"\"/>");
	}

    private void displayError(Throwable caught) {
        Feedback fb = new Feedback();
        fb.showError(caught);
        GWT.getUncaughtExceptionHandler().onUncaughtException(caught);
        wrapper.setWidget(fb);
    }
}
