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
public class VerticalForm extends ComplexPanel implements View {

	String labelClass = "ui-form-label";
	String labelStyle = null;
	String fieldClass = "ui-form-field";
	String fieldStyle = null;
	ArrayList<Column> columns = new ArrayList<Column>();
	Element tbody;
	Element labelRow;
	Element valueRow;
	private Element form;
	
	class Column {
		View view;
		Value test;
		Element label;
		Element value;
		boolean visible;
		
		public Column(View view, Value test, Element label, Element value, boolean visible) {
			super();
			this.view = view;
			this.test = test;
			this.label = label;
			this.value = value;
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
				for(Element element : new Element[]{label, value}) {
					DOM.setStyleAttribute(element, "display", "none");
				}
				visible = false;
			}
		}
		public void show() {
			if(!visible) {
				for(Element element : new Element[]{label, value}) {
					DOM.setStyleAttribute(element, "display", "");
				}
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
		public boolean isVisible() {
			return visible;
		}
		public void setVisible(boolean visible) {
			this.visible = visible;
		}
	}
	
	public VerticalForm() {
		form = DOM.createDiv();
		setStyleName(form, "form");
		addRow();

		setElement(form);
		setStylePrimaryName("form");
		addStyleName("two-row-form");
	}

	private void addRow() {
		Element table = DOM.createTable();
		
		tbody = DOM.createTBody();
		DOM.appendChild(table, tbody);
		
		labelRow = DOM.createTR();
		DOM.appendChild(tbody, labelRow);
		valueRow = DOM.createTR();
		DOM.appendChild(tbody, valueRow);
		
		DOM.appendChild(form, table);
	}
	
	public void addField(String label, View field) {
		addField(label, null, field, null, true);
	}
	
	public void addField(String label, View field, Value test) {
		addField(label, null, field, test, true);
	}
	
	public void addField(String label, String styleName, View field) {
		addField(label, styleName, field, null, true);
	}

	public void addField(String label, String styleName, View field, Value test) {
		addField(label, styleName, field, test, true);
	}

	public void addField(String label, String styleName, View field, Value test, boolean addBreak) {
		
		Element labelTH = DOM.createTH();
		if(styleName != null) setStylePrimaryName(labelTH, styleName);
		DOM.appendChild(labelRow, labelTH);
		Element labelElement = DOM.createLabel();
		DOM.setStyleAttribute(labelElement, "whiteSpace", "nowrap");
		DOM.appendChild(labelTH, labelElement);
		DOM.setInnerText(labelElement, label);
		setStylePrimaryName(labelElement, labelClass);
		if(labelStyle != null) DOM.setElementAttribute(labelElement, "style", labelStyle);
		
		Element widgetTD = DOM.createTD();
		DOM.appendChild(valueRow, widgetTD);
		Widget fieldWidget = field.getViewWidget();
		fieldWidget.setStyleName(fieldClass);
		if(fieldStyle != null) DOM.setElementAttribute(fieldWidget.getElement(), "style", fieldStyle);
		add(fieldWidget, widgetTD);
		
		columns.add(new Column(field, test, labelTH, widgetTD, true));
		if(addBreak) addBreak();
	}
	
	public void addBreak() {
		addRow();
	}
	public void clearFields() {
		for (Column col : columns) {
			View view = col.getView();
			view.clearFields();
		}
	}

	public Widget getViewWidget() {
		return this;
	}

	public void load(AsyncCallback callback) {
		AsyncCallbackGroup group = new AsyncCallbackGroup();
        for (Column col : columns) {
			col.load(group.member());
		}
		group.ready(callback);
	}

	public void save(AsyncCallback callback) {
		AsyncCallbackGroup group = new AsyncCallbackGroup();
		for (Column col : columns) {
            col.save(group.member());
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

}
