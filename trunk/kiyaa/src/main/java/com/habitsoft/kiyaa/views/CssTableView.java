package com.habitsoft.kiyaa.views;

import java.util.ArrayList;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.SourcesChangeEvents;
import com.google.gwt.user.client.ui.Widget;
import com.habitsoft.kiyaa.metamodel.ModelCollection;
import com.habitsoft.kiyaa.util.AsyncCallbackGroup;
import com.habitsoft.kiyaa.util.ModelFilter;
import com.habitsoft.kiyaa.widgets.Label;

/**
 * A "CSS" table view.  The assumption is that you are doing layout using the "grid" system
 * and thus you don't want to use <table> tags because the browser doesn't obey your commands
 * when you do so.
 * 
 * Each column must be given a CSS class which specifies its width.  In addition, the classes
 * .thead, .tbody, .tr, .th, and .td are added to the appropriate div tags within the layout,
 * so they can be styled using css. 
 * 
 */
public class CssTableView implements View, SourcesChangeEvents {
	ArrayList columnViewFactories = new ArrayList();
	ArrayList columnStyleNames = new ArrayList();
	FlowPanel headings = new FlowPanel();
	FlowPanel panel = new FlowPanel();
	ListView list = new ListView();
	
	class RowModelView implements ModelView {
		Object model;
		FlowPanel panel = new FlowPanel();
		final ModelView[] views;
		
		public RowModelView() {
			views = new ModelView[columnViewFactories.size()];
			for (int i = 0; i < views.length; i++) {
				views[i] = (ModelView)((ViewFactory)columnViewFactories.get(i)).createView();
				final Widget viewWidget = views[i].getViewWidget();
				viewWidget.setStyleName((String)columnStyleNames.get(i));
				viewWidget.addStyleName("td");
				
				// Extremely evil hack to get around firefox's stupid habit
				// of ignoring empty div's during layout.  DAMN YOU FIREFOX!
				/*
				Element nbsp = DOM.createSpan();
				DOM.setInnerHTML(nbsp, "&nbsp;");
				DOM.appendChild(viewWidget.getElement(), nbsp);
				*/
				panel.add(viewWidget);
			}
		}
		
		public Object getModel() {
			return model;
		}

		public void setModel(Object model, AsyncCallback callback) {
			this.model = model;
			AsyncCallbackGroup group = new AsyncCallbackGroup();
			for (int i = 0; i < views.length; i++) {
				ModelView view = views[i];
				view.setModel(model, group.member());
			}
			group.ready(callback);
		}

		public void clearFields() {
			for (int i = 0; i < views.length; i++) {
				View view = views[i];
				view.clearFields();
			}
		}

		public void load(AsyncCallback callback) {
			AsyncCallbackGroup group = new AsyncCallbackGroup();
			for (int i = 0; i < views.length; i++) {
				View view = views[i];
				view.load(group.<Void>member());
			}
			group.ready(callback);
		}

		public void save(AsyncCallback callback) {
			AsyncCallbackGroup group = new AsyncCallbackGroup();
			for (int i = 0; i < views.length; i++) {
				View view = views[i];
				view.save(group.<Void>member());
			}
			group.ready(callback);
		}

		public Widget getViewWidget() {
			return panel;
		}
		
	}

	public CssTableView() {
		panel.setStylePrimaryName("table");
		headings.setStyleName("thead");
		list.setStyleName("tbody");
		list.setRowClass("tr");
		panel.add(headings);
		panel.add(list);
		list.viewFactory = new ViewFactory() {
			public View createView() {
				return new RowModelView();
			}
		}; 
	}
	public void addHeading(Widget widget) {
		widget.addStyleName("th");
		headings.add(widget);
	}
	public void addHeadingText(String heading, String styleName) {
		final Label label = new Label(heading);
		label.setStyleName(styleName);
		addHeading(label);
	}
	public void addHeadingHtml(String heading, String styleName) {
		final Label html = new Label();
		html.setHTML(heading);
		html.setStyleName(styleName);
		addHeading(html);
	}
	public void addColumn(ViewFactory viewFactory, String heading, String styleName) {
		addHeadingText(heading, styleName);
		addColumn(viewFactory, styleName);
	}
	public void addColumn(ViewFactory viewFactory, String styleName) {
		columnViewFactories.add(viewFactory);
		columnStyleNames.add(styleName);
	}
	public int getColumnCount() {
		return columnViewFactories.size();
	}
	public void addChangeListener(ChangeListener listener) {
		list.addChangeListener(listener);
	}
	public void addStyleDependentName(String styleSuffix) {
		list.addStyleDependentName(styleSuffix);
	}
	public void addStyleName(String style) {
		list.addStyleName(style);
	}
	public void clearFields() {
		list.clearFields();
	}
	public void setFilter(ModelFilter modelFilter, AsyncCallback callback) {
		list.setFilter(modelFilter, callback);
	}
	public int getAbsoluteLeft() {
		return list.getAbsoluteLeft();
	}
	public int getAbsoluteTop() {
		return list.getAbsoluteTop();
	}
	public ModelCollection getCollection() {
		return list.getCollection();
	}
	public int getIncrement() {
		return list.getIncrement();
	}
	public Object[] getModels() {
		return list.getModels();
	}
	public int getSelectedIndex() {
		return list.getSelectedIndex();
	}
	public Object getSelectedModel() {
		return list.getSelectedModel();
	}
	public String getStyleName() {
		return list.getStyleName();
	}
	public String getStylePrimaryName() {
		return list.getStylePrimaryName();
	}
	public Widget getViewWidget() {
		return panel;
	}
	public boolean isSelectable() {
		return list.isSelectable();
	}
	public boolean isVisible() {
		return list.isVisible();
	}
	public void load(AsyncCallback callback) {
		list.load(callback);
	}
	public void removeChangeListener(ChangeListener listener) {
		list.removeChangeListener(listener);
	}
	public void removeStyleDependentName(String styleSuffix) {
		list.removeStyleDependentName(styleSuffix);
	}
	public void removeStyleName(String style) {
		list.removeStyleName(style);
	}
	public void save(AsyncCallback callback) {
		list.save(callback);
	}
	public void setCollection(ModelCollection collection, AsyncCallback callback) {
		list.setCollection(collection, callback);
	}
	public void setHeight(String height) {
		list.setHeight(height);
	}
	public void setIncrement(int increment) {
		list.setIncrement(increment);
	}
	public void setModels(Object[] models) {
		list.setModels(models);
	}
	public void setPixelSize(int width, int height) {
		list.setPixelSize(width, height);
	}
	public void setSelectable(boolean selectable) {
		list.setSelectable(selectable);
	}
	public void setSelectedIndex(int newIndex) {
		list.setSelectedIndex(newIndex);
	}
	public void setSelectedModel(Object selectedItem) {
		list.setSelectedModel(selectedItem);
	}
	public void setSize(String width, String height) {
		list.setSize(width, height);
	}
	public void setStyleName(String style) {
		list.setStyleName(style);
	}
	public void setStylePrimaryName(String style) {
		list.setStylePrimaryName(style);
	}
	public void setTitle(String title) {
		list.setTitle(title);
	}
	public void setVisible(boolean visible) {
		list.setVisible(visible);
	}
	public void setWidth(String width) {
		list.setWidth(width);
	}
	public String getRowClass() {
		return list.getRowClass();
	}
	public String getRowStyle() {
		return list.getRowStyle();
	}
	public void setRowClass(String rowClass) {
		list.setRowClass(rowClass);
	}
	public void setRowStyle(String rowStyle) {
		list.setRowStyle(rowStyle);
	}
	public void setBodyClass(String bodyClass) {
		list.addStyleName(bodyClass);
	}
	public void setBodyStyle(String bodyStyle) {
		DOM.setElementAttribute(list.getElement(), "style", bodyStyle);
	}
//	public int getMaxHeight() {
//		return list.getMaxHeight();
//	}
//	public void setMaxHeight(int maxHeight) {
//		list.setMaxHeight(maxHeight);
//	}
}
