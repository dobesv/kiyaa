package com.habitsoft.kiyaa.widgets;

import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.SourcesChangeEvents;
import com.habitsoft.kiyaa.metamodel.Action;
import com.habitsoft.kiyaa.util.NameValueAdapter;

public class EditableLabelListBox extends EditableLabel implements SourcesChangeEvents {
	ListBox listbox = new ListBox();
	String showingValue;
	
	@Override
	protected FocusWidget createEditor() {
		return listbox;
	}
	public void addItem(String item, String value) {
		listbox.addItem(item, value);
	}

	public void addAction(String label, Action action) {
		listbox.addAction(label, action);
	}

	public void addItem(String item) {
		listbox.addItem(item);
	}

	public int getItemCount() {
		return listbox.getItemCount();
	}

	public String getItemText(int index) {
		return listbox.getItemText(index);
	}

	public NameValueAdapter getNameValueAdapter() {
		return listbox.getNameValueAdapter();
	}

	public String getValue() {
		return listbox.getValue();
	}

	public String getValue(int index) {
		return listbox.getValue(index);
	}

	public Long getValueAsId() {
		return listbox.getValueAsId();
	}

	public void insertItem(String item, int index) {
		listbox.insertItem(item, index);
	}

	public void insertItem(String item, String value, int index) {
		listbox.insertItem(item, value, index);
	}

	public boolean isItemSelected(int index) {
		return listbox.isItemSelected(index);
	}

	public boolean isMultipleSelect() {
		return listbox.isMultipleSelect();
	}

	public boolean isOptional() {
		return listbox.isOptional();
	}

	public void removeItem(int index) {
		listbox.removeItem(index);
	}

	public void setCurrentValue(String currentValue) {
		listbox.setCurrentValue(currentValue);
		updateFromEditor();
	}

	public void setItemSelected(int index, boolean selected) {
		listbox.setItemSelected(index, selected);
		updateFromEditor();
	}

	public void setItemText(int index, String text) {
		listbox.setItemText(index, text);
		updateFromEditor();
	}

	public void setLabels(String[] labels) {
		listbox.setLabels(labels);
		updateFromEditor();
	}

	public void setModels(Object[] models) {
		listbox.setModels(models);
		updateFromEditor();
	}

	public void setMultipleSelect(boolean multiple) {
		listbox.setMultipleSelect(multiple);
	}

	public void setNameValueAdapter(NameValueAdapter nameValueAdaptor) {
		listbox.setNameValueAdapter(nameValueAdaptor);
	}

	public void setOptional(boolean optional) {
		listbox.setOptional(optional);
	}

	public void setSelectedIndex(int index) {
		listbox.setSelectedIndex(index);
		updateFromEditor();
	}

	public void setValue(int index, String value) {
		listbox.setValue(index, value);
		updateFromEditor();
	}

	public void setValue(String value) {
		listbox.setValue(value);
		updateFromEditor();
	}

	@Override
	protected void updateFromEditor() {
		setText(listbox.getCurrentLabel());
		showingValue = listbox.getValue();
	}
	
	@Override
	protected void updateEditor() {
		listbox.setValue(showingValue);
	}
	
	public void setValueAsId(Long value) {
		listbox.setValueAsId(value);
		updateFromEditor();
	}

	public void setValues(String[] values) {
		listbox.setValues(values);
		updateFromEditor();
	}

	public void setVisibleItemCount(int visibleItems) {
		listbox.setVisibleItemCount(visibleItems);
	}

	public void addChangeListener(ChangeListener listener) {
		listbox.addChangeListener(listener);
	}

	public void removeChangeListener(ChangeListener listener) {
		listbox.removeChangeListener(listener);
	}
	
	
}
