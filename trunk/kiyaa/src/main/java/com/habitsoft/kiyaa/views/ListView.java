package com.habitsoft.kiyaa.views;

import java.util.ArrayList;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import com.habitsoft.kiyaa.util.AsyncCallbackGroup;
import com.habitsoft.kiyaa.util.HoverStyleHandler;

/**
 * Creator MUST call setViewFactory() before adding the view to the UI.
 */
public class ListView<T> extends BaseCollectionView<T> {

	FlowPanel flow = new FlowPanel();
	ViewFactory viewFactory;
	ArrayList<ModelView<T>> views = new ArrayList<ModelView<T>>();
	String rowClass=null;
	String rowStyle=null;
	View emptyContent;
	
	public ListView() {
		add(flow);
		flow.setStylePrimaryName("ui-list");
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void showItem(final int i, T object, AsyncCallbackGroup group) {
		ModelView<T> view = (ModelView<T>)viewFactory.createView();
		view.setModel(object, group.<Void>member());
		views.add(view);
		Widget widget = view.getViewWidget();
		if(selectable || clickable) {
    		FocusPanel wrapper = new FocusPanel(view.getViewWidget());
    		wrapper.addClickListener(new ClickListener() {
    			public void onClick(Widget sender) {
    				onRowClicked(i);
    			}
    		});
    		wrapper.addMouseListener(new HoverStyleHandler(wrapper, hoverGroup));
    		widget = wrapper;
		}
		widget.setStylePrimaryName("ui-list-item");
		if(rowStyle != null) DOM.setElementAttribute(widget.getElement(), "style", rowStyle);
		if(rowClass != null) {
			widget.addStyleName(rowClass);
		}
		if((i & 1) == 1) { // humans are base-1 creatures, so odd is really even.  How odd!
			widget.addStyleName("even");
			widget.removeStyleName("odd");
		} else {
			widget.addStyleName("odd");
			widget.removeStyleName("even");
		}
		if(i >= flow.getWidgetCount())
			flow.add(widget);
		else {
			flow.insert(widget, i);
			for(int j=i+1; j < flow.getWidgetCount(); j++) {
				if((j & 1) == 1) { // humans are base-1 creatures, so odd is really even.  How odd!
					flow.getWidget(j).addStyleName("even");
					flow.getWidget(j).removeStyleName("odd");
				} else {
					flow.getWidget(j).addStyleName("odd");
					flow.getWidget(j).removeStyleName("even");
				}
			}
		}
		if(views.size() == 1)
			checkEmpty();
	}

	@Override
	protected void setItem(int i, T model, AsyncCallbackGroup group) {
		if(i >= views.size()) {
			addItem(i, model, group);
			return;
		}
		ModelView<T> view = views.get(i);
		view.setModel(model, group.<Void>member());
	}
	@Override
	protected void hideItem(final int i) {
		flow.remove(i);
		views.remove(i);
		if(views.isEmpty())
			checkEmpty();
	}
	
	public ViewFactory getViewFactory() {
		return viewFactory;
	}

	public void setViewFactory(ViewFactory viewFactory) {
		this.viewFactory = viewFactory;
	}

	@Override
	protected void showSelected(int row, boolean selected) {
		try {
			Widget w = flow.getWidget(row);
			if(selected) w.addStyleDependentName("selected");
			else w.removeStyleDependentName("selected");
		} catch (IndexOutOfBoundsException iobe) {
			// I guess that's not a valid widget index, oh well!
			GWT.log("selected index invalid: "+row+" selected = "+selected, iobe);
		}
	}
	
	@Override
	public void setSelectable(boolean selectable) {
		boolean changed = (selectable || this.clickable) != (this.selectable || this.clickable);
		super.setSelectable(selectable);
		if(changed)
		    makeRowsClickable();
	}

	@Override
	public void setClickable(boolean clickable) {
        boolean changed = (this.selectable || clickable) != (this.selectable || this.clickable);
	    super.setClickable(clickable);
	    if(changed)
	        makeRowsClickable();
	}
    private void makeRowsClickable() {
        boolean clickable = (this.selectable || this.clickable);
		for(int i=0; i < flow.getWidgetCount(); i++) {
			Widget w = flow.getWidget(i);
			Widget replacement;
			if(selectable) {
	    		FocusPanel wrapper = new FocusPanel(w);
	    		final int row = i;
	    		wrapper.addClickListener(new ClickListener() {
	    			public void onClick(Widget sender) {
	    				onRowClicked(row);
	    			}
	    		});
	    		wrapper.addMouseListener(new HoverStyleHandler(w, hoverGroup));
	    		replacement = wrapper;
			} else {
				replacement = ((FocusPanel)w).getWidget();
			}
    		flow.insert(replacement, i);
    		flow.remove(w);
		}
    }
    
	@Override
	public void clearRows() {
		flow.clear();
		views.clear();
		checkEmpty();
	}

	@Override
	public void clearFields() {
		super.clearFields();
        for (View view : views) {
			view.clearFields();
		}
	}

	public void save(AsyncCallback<Void> callback) {
		AsyncCallbackGroup group = new AsyncCallbackGroup();
        for (View view : views) {
			view.save(group.<Void>member());
		}
		group.ready(callback);
	}
	
	@Override
	protected void startLoadingModels(AsyncCallbackGroup group) {
	}
	
	@Override
	protected void finishLoadingModels(AsyncCallbackGroup group) {
        if(emptyContent != null)
            emptyContent.load(group.<Void>member());
	}
	
	@Override
	protected void loadItem(int i, AsyncCallbackGroup group) {
	    views.get(i).load(group.<Void>member());
	}
	
	@Override
	protected UIObject getRowUIObject(int row) {
	    if(row < views.size())
	        return ((View)views.get(row)).getViewWidget();
	    else
	        return null;
	}

	public String getRowClass() {
		return rowClass;
	}

	public void setRowClass(String rowClass) {
		this.rowClass = rowClass;
	}

	public String getRowStyle() {
		return rowStyle;
	}

	public void setRowStyle(String rowStyle) {
		this.rowStyle = rowStyle;
	}

	@Override
	protected Element getScrollElement() {
		return getElement();
	}

	@Override
	protected Widget getWidget() {
		return flow;
	}

	public View getEmptyContent() {
		return emptyContent;
	}

	public void setEmptyContent(View emptyContent) {
		if(this.emptyContent != null) {
			this.emptyContent.getViewWidget().removeFromParent();
		}
		this.emptyContent = emptyContent;
		if(emptyContent != null) {
			emptyContent.getViewWidget().addStyleName("empty-table-content");
			flow.add(emptyContent.getViewWidget());
			checkEmpty();
		}
	}

	private void checkEmpty() {
		boolean empty = items.isEmpty();
		if(emptyContent != null) {
			emptyContent.getViewWidget().setVisible(empty);
		}
		if(empty) addStyleName("empty");
		else removeStyleName("empty");
	}

}
