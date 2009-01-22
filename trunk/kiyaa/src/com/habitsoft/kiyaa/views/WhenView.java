package com.habitsoft.kiyaa.views;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.habitsoft.kiyaa.util.AsyncCallbackProxy;

/**
 * This is used to implement lazy conditional subview inside templates,
 * but of course you can use it elsewhere.
 * 
 * Set up a view factory and when the value if test is true, the view
 * is created and shown.
 * 
 * This class wraps its contents in a SPAN, which is always around even
 * when the inner view is not created.
 * 
 * As a convenience, you can provide placeholder HTML to use when the
 * test is false, for example to put an &nbsp; to ensure the DIV
 * participates in the HTML layout.
 */
public class WhenView extends SimplePanel implements View, TakesElementName {

	ViewFactory viewFactory;
	boolean shouldShow;
	View view;
	String placeholderHtml;
	
	public WhenView() {
	    this(null, null);
	}
	
	public WhenView(String tagName, String tagNamespace) {
	    super(tagName!=null && !"when".equals(tagName) ? DOM.createElement(tagName) : DOM.createDiv());
        DOM.setStyleAttribute(getElement(), "display", "inline");
	}
	
	/**
	 * Used to construct view, when the test is set to true.
	 */
	public ViewFactory getViewFactory() {
		return viewFactory;
	}

	public void setViewFactory(ViewFactory viewFactory) {
		this.viewFactory = viewFactory;
	}
	
	/**
	 * If passed true, the view will be constructed on the next load()
	 * if it wasn't already.  If false, the view will be destroyed on
	 * the next load() if it has been constructed.
	 * 
	 * In addition, setVisible(truth) will be called to hide the WhenView
	 * if it is not supposed to be shown.
	 */
	public void setTest(boolean truth) {
		setVisible(shouldShow = truth);
	}
	
	/**
	 * Opposite of setTest(), may be handy when the boolean NOT
	 * operator isn't readily available.
	 */
	public void setTestNot(boolean truth) {
		setTest(!truth);
	}
	
	/**
	 * Pass the clearFields() request on to the view, if it was
	 * constructed.
	 */
	public void clearFields() {
		if(view != null) {
			view.clearFields();
		}
	}

	/**
	 * Return the SimplePanel we use as our widget
	 */
	public Widget getViewWidget() {
		return this;
	}

	/**
	 * Based on whether the test is currently true or false, create
	 * or destroy the view.
	 * 
	 * If the view exists, calls load() on it.
	 */
	public void load(AsyncCallback callback) {
		if(shouldShow) {
			if(view == null) {
				if(viewFactory == null) {
					callback.onSuccess(null);
					return;
				}
				view = viewFactory.createView();
				
			}
			view.load(new AsyncCallbackProxy(callback) {
				@Override
				public void onSuccess(Object result) {
					// View may have become null while we waited, for whatever reason
					if(view != null && getWidget() != view.getViewWidget())
						setWidget(view.getViewWidget());
					super.onSuccess(result);
				}
			});
		} else {
			if(view != null) {
				view.getViewWidget().removeFromParent();
				view = null;
			}
			if(placeholderHtml != null) {
				DOM.setInnerHTML(getElement(), placeholderHtml);
			}
			callback.onSuccess(null);
		}
	}

	/**
	 * Proxy the save() call to the view, if it's current existing.
	 */
	public void save(AsyncCallback callback) {
		if(view != null) {
			view.save(callback);
		} else {
			callback.onSuccess(null);
		}
	}
	
	public String getPlaceholderHtml() {
		return placeholderHtml;
	}
    /**
     * When the view is not being shown, you can provide some placeholder
     * HTML to display.  This sets the innerHtml of this DIV when the
     * test is false.
     */
	public void setPlaceholderHtml(String placeholderHtml) {
		this.placeholderHtml = placeholderHtml;
	}
	
	/**
	 * Some browsers eliminate DIVs from the layout process if they
	 * don't have anything inside them.  Setting this to true will
	 * set the placeHolderHtml to "&nbsp;".
	 */
	public void setPlaceholderNbsp(boolean useNbsp) {
		if(useNbsp) {
			placeholderHtml = "&nbsp;";
		}
	}
}
