package com.habitsoft.kiyaa.forms;

import java.util.ArrayList;
import java.util.Iterator;

import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.FieldSetElement;
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
public class TwoRowForm extends ComplexPanel implements View {

	String labelClass = "ui-form-label";
	String labelStyle = null;
	String fieldClass = "ui-form-field";
	String fieldStyle = null;
	String defaultFieldClass = "set";
	ArrayList<Field> fields = new ArrayList<Field>();
	private FieldSetElement fieldset;
	private String rowClass = "fieldrow";
	DivElement row;
	
	class Field {
		View view;
		Value test;
		Element element;
		boolean visible;
		
		public Field(View view, Value test, Element element, boolean visible) {
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
		public boolean isVisible() {
			return visible;
		}
		public void setVisible(boolean visible) {
			this.visible = visible;
		}
	}
	
	public TwoRowForm() {
		fieldset = FieldSetElement.as(DOM.createFieldSet());
		addRow();

		setElement(fieldset);
	}

	private void addRow() {
		row = DivElement.as(DOM.createDiv());
		setStyleName(row, rowClass);
		fieldset.appendChild(row);
	}
	
	public void addField(String label, View field) {
		addField(label, false, defaultFieldClass, field, null);
	}
	
	public void addField(String label, View field, Value test) {
		addField(label, false, defaultFieldClass, field, test);
	}
	
	public void addField(String label, String styleName, View field) {
		addField(label, false, styleName, field, null);
	}

	public void addField(String label, String styleName, View field, Value test) {
		addField(label, false, styleName, field, test);
	}

	public void addField(String label, boolean htmlLabel, String styleName, View field, Value test) {
		
		Element fieldDiv = DOM.createDiv();
		setStyleName(fieldDiv, styleName);
		Element labelElement = DOM.createLabel();
		DOM.setStyleAttribute(labelElement, "whiteSpace", "nowrap");
		fieldDiv.appendChild(labelElement);
		if(htmlLabel) DOM.setInnerHTML(labelElement, label); 
		else DOM.setInnerText(labelElement, label);
		setStylePrimaryName(labelElement, labelClass);
		if(labelStyle != null) DOM.setElementAttribute(labelElement, "style", labelStyle);
		fieldDiv.appendChild(DOM.createElement("br"));
		Widget fieldWidget = field.getViewWidget();
		fieldWidget.setStyleName(fieldClass);
		if(fieldStyle != null) DOM.setElementAttribute(fieldWidget.getElement(), "style", fieldStyle);
		add(fieldWidget, fieldDiv);
		row.appendChild(fieldDiv);
		fields.add(new Field(field, test, fieldDiv, true));
	}
	
	public void addBreak() {
		addRow();
	}
	public void clearFields() {
		for (Iterator<Field> i = fields.iterator(); i.hasNext();) {
			View view = i.next().getView();
			view.clearFields();
		}
	}

	public Widget getViewWidget() {
		return this;
	}

	public void load(AsyncCallback callback) {
		AsyncCallbackGroup group = new AsyncCallbackGroup();
		for (Iterator<Field> i = fields.iterator(); i.hasNext();) {
			i.next().load(group.member());
		}
		group.ready(callback);
	}

	public void save(AsyncCallback callback) {
		AsyncCallbackGroup group = new AsyncCallbackGroup();
		for (Iterator<Field> i = fields.iterator(); i.hasNext();) {
			i.next().save(group.member());
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

	public String getRowClass() {
		return rowClass;
	}

	public void setRowClass(String rowClass) {
		this.rowClass = rowClass;
	}

	public String getDefaultFieldClass() {
		return defaultFieldClass;
	}

	public void setDefaultFieldClass(String fieldDivClass) {
		this.defaultFieldClass = fieldDivClass;
	}

}
