package com.habitsoft.kiyaa.forms;

import java.util.ArrayList;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.Widget;
import com.habitsoft.kiyaa.metamodel.Value;
import com.habitsoft.kiyaa.util.AsyncCallbackGroup;
import com.habitsoft.kiyaa.util.AsyncCallbackProxy;
import com.habitsoft.kiyaa.views.View;

/**
 * Allow easy creation of a form that goes
 * 
 * Label: Editor
 * 
 * Where all the labels are in one column and all the editors
 * are in the other.
 * 
 * It also allows insertion of section titles.
 * 
 */
public class TwoColumnForm extends ComplexPanel implements View {

	String labelClass = "ui-form-label";
	String labelStyle = null;
	String fieldClass = "ui-form-field";
	String fieldStyle = null;
	ArrayList<Row> rows = new ArrayList<Row>();
	Element tbody;
	private Element table;
	
	class Row {
		View view;
		Value test;
		Element element;
		boolean visible;
		
		public Row(View view, Value test, Element element, boolean visible) {
			super();
			this.view = view;
			this.test = test;
			this.element = element;
			this.visible = visible;
		}
		public View getView() {
			return view;
		}
		public void setView(View view) {
			this.view = view;
		}
		public Value getTest() {
			return test;
		}
		public void setTest(Value test) {
			this.test = test;
		}
		public void hide() {
			if(visible) {
				DOM.setStyleAttribute(element, "display", "none");
				visible = false;
			}
		}
		public void show() {
			if(!visible) {
				DOM.setStyleAttribute(element, "display", "");
				visible = true;
			}
		}
		public void load(AsyncCallback callback) {
			if(test != null) {
				test.getValue(new AsyncCallbackProxy<Boolean>(callback) {
					@Override
					public void onSuccess(Boolean result) {
						if(result) {
							show();
							view.load(callback);
						} else {
							hide();
							super.onSuccess(null);
						}
					}
				});
			} else {
				view.load(callback);
			}
		}
		public void save(AsyncCallback callback) {
			if(visible)
				view.save(callback);
			else
				callback.onSuccess(null);
		}
		public Element getElement() {
			return element;
		}
		public void setElement(Element element) {
			this.element = element;
		}
		public boolean isVisible() {
			return visible;
		}
		public void setVisible(boolean visible) {
			this.visible = visible;
		}
	}
	
	public TwoColumnForm() {
		Element form = DOM.createDiv();
		setStyleName(form, "form");
		table = DOM.createTable();
		DOM.appendChild(form, table);
		tbody = DOM.createTBody();
		DOM.appendChild(table, tbody);
		setElement(form);
		setStylePrimaryName("form");
		addStyleName("two-column-form");
		
	}
	
	public void addField(String label, View field) {
		addField(label, null, field, null);
	}
	
	public void addField(String label, View field, Value test) {
		addField(label, null, field, test);
	}
	
	public void addField(String label, String styleName, View field) {
		addField(label, styleName, field, null);
	}

	public void addField(String label, String styleName, View field, Value test) {
		
		Element row = DOM.createTR();
		if(styleName != null) setStylePrimaryName(row, styleName);
		Element labelTD = DOM.createTD();
		DOM.appendChild(row, labelTD);
		Element labelElement = DOM.createLabel();
		DOM.appendChild(labelTD, labelElement);
		DOM.setStyleAttribute(labelElement, "whiteSpace", "nowrap");
		DOM.setInnerText(labelElement, label);
		setStylePrimaryName(labelElement, labelClass);
		if(labelStyle != null) DOM.setElementAttribute(labelElement, "style", labelStyle);
		
		Element widgetTD = DOM.createTD();
		DOM.appendChild(row, widgetTD);
		Widget fieldWidget = field.getViewWidget();
		fieldWidget.setStyleName(fieldClass);
		if(fieldStyle != null) DOM.setElementAttribute(fieldWidget.getElement(), "style", fieldStyle);
		add(fieldWidget, widgetTD);
		
		DOM.appendChild(tbody, row);
		rows.add(new Row(field, test, row, true));
	}
	
	public void clearFields() {
	    for (Row row : rows) {
            row.getView().clearFields();
		}
	}

	public Widget getViewWidget() {
		return this;
	}

	public void load(AsyncCallback callback) {
		AsyncCallbackGroup group = new AsyncCallbackGroup();
		for (Row row : rows) {
			row.load(group.member());
		}
		group.ready(callback);
	}

	public void save(AsyncCallback callback) {
		AsyncCallbackGroup group = new AsyncCallbackGroup();
		for (Row row : rows) {
            row.save(group.member());
		}
		group.ready(callback);
	}

	public String getLabelClass() {
		return labelClass;
	}

	public void setLabelClass(String labelClass) {
		this.labelClass = labelClass;
	}

	public String getFieldClass() {
		return fieldClass;
	}

	public void setFieldClass(String fieldClass) {
		this.fieldClass = fieldClass;
	}

	public String getLabelStyle() {
		return labelStyle;
	}

	public void setLabelStyle(String labelStyle) {
		this.labelStyle = labelStyle;
	}

	public String getFieldStyle() {
		return fieldStyle;
	}

	public void setFieldStyle(String fieldStyle) {
		this.fieldStyle = fieldStyle;
	}

	public void setTableClass(String styleName) {
		setStyleName(table, styleName);
	}
}
