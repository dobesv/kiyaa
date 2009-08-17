package com.habitsoft.kiyaa.widgets;

import java.util.ArrayList;
import java.util.HashMap;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.Widget;
import com.habitsoft.kiyaa.metamodel.Action;
import com.habitsoft.kiyaa.metamodel.ModelCollection;
import com.habitsoft.kiyaa.util.FocusGroup;
import com.habitsoft.kiyaa.util.NameValueAdapter;

/**
 * <ui:listbox ... /> 
 * 
 * Currently the order in which the attributes are set is important; be sure
 * to set models last so that the name/value adapter and optional
 * attributes are taken into consideration.
 * 
 * Note that when using selectedModel(), the models in the listbox should
 * implement equals() or it might not show the correct selected value
 * on a refresh.
 * 
 */
public class ListBox extends com.google.gwt.user.client.ui.ListBox {

	protected String currentValue;
	protected ModelCollection collection;
	protected boolean optional;
	protected String nullLabel="";
	protected HashMap actions = new HashMap();
	protected ArrayList<String> actionLabels = new ArrayList();
	protected Object[] models;
	protected Object pendingSelectedModel;
	protected FocusGroup focusGroup;
	
	public ListBox() {
	}

	public ListBox(boolean isMultipleSelect) {
		super(isMultipleSelect);
	}

	public void setLabels(String[] labels) {
		for (int i = 0; i < labels.length; i++) {
			String label = labels[i];
			if(i >= getItemCount())
				this.addItem(label);
			else
				this.setItemText(i, label);
		}
	}
	
	public void setValues(String[] values) {
		for (int i = 0; i < values.length; i++) {
			String value = values[i];
			if(i >= getItemCount())
				this.addItem(value, value);
			else
				this.setValue(i, value);
			if(currentValue == value || (currentValue != null && value != null && value.equals(currentValue))) {
				setSelectedIndex(i);
			}
		}
	}
	
	public void setValue(String value) {
		if(currentValue != null && value != null && currentValue.equals(value))
			return;
		currentValue = value;
		for(int i=0; i < getItemCount(); i++) {
			if(getValue(i).equals(value)) {
				setSelectedIndex(i);
				break;
			}
		}
	}
	
	public String getValue() {
		final int selectedIndex = getSelectedIndex();
		currentValue = selectedIndex>=0?getValue(selectedIndex):null;
		return currentValue;
	}
	public void setValueAsId(Long value) {
		if(value == null) {
			setValue("");
		} else {
			setValue(value.toString());
		}
	}
	public Long getValueAsId() {
		String value = getValue();
		if(value == null || value.equals("")) {
			return null;
		} else {
			try {
				return new Long(value);
			} catch(NumberFormatException nfe) {
				return null;
			}
		}
	}
	public String getCurrentValue() {
		return currentValue;
	}

	public String getCurrentLabel() {
		try {
			return this.getItemText(getSelectedIndex());
		} catch(IndexOutOfBoundsException iobe) {
			return null;
		}
	}
	
	public void setCurrentValue(String currentValue) {
		this.currentValue = currentValue;
	}
	
	public Object getSelectedModel() {
		int index = getSelectedIndex() - (optional?1:0);
		if(index >= 0 && models != null && index < models.length)
			return models[index];
		return (optional || models==null || models.length == 0)?null:models[0];
	}
	public void setSelectedModel(Object model) {
		if(model != null) {
			if(models != null) {
        		for(int i=0; i < models.length; i++) {
        			if(models[i].equals(model)) {
        				setSelectedIndex(i + (optional?1:0));
        				return;
        			}
        		}
			}
			
			// If we didn't find it, wait for it ...
			pendingSelectedModel = model;
		} else if(optional) {
			setSelectedIndex(0);
		} else {
			setSelectedIndex(-1);
		}
	}
	public boolean isOptional() {
		return optional;
	}

	public void setOptional(boolean optional) {
		this.optional = optional;
	}

	NameValueAdapter nameValueAdapter;
	
	/**
	 * Set the models
	 * 
	 * You must set nameValueAdaptor before setting the models unless the
	 * toString() method of the models is good enough.
	 */
	public void setModels(Object[] models) {
		//GWT.log("setModels()..."+this.models+" -> "+models+" ==? "+(models == this.models), new Exception());
		if(models != this.models) {
    		this.models = models;
    		clear();
    		if(optional) {
    			addItem(nullLabel, "");
    		}
    		for (int i = 0; i < models.length; i++) {
    			Object model = models[i];
    			String name;
    			String value;
    			if(nameValueAdapter != null) {
    				name = nameValueAdapter.getName(model);
    				value = nameValueAdapter.getValue(model);
    			} else {
    				name = value = model.toString();
    			}
    			addItem(name, value);
    			if(currentValue != null && value.equals(currentValue)) {
    				setSelectedIndex(getItemCount()-1);
    			}
    		}
    		for (String label : actionLabels) {
    			this.addItem(label);
    		}
		}
		if(pendingSelectedModel != null) {
			setSelectedModel(pendingSelectedModel);
			pendingSelectedModel = null;
		}
	}

	public NameValueAdapter getNameValueAdapter() {
		return nameValueAdapter;
	}

	public void setNameValueAdapter(NameValueAdapter nameValueAdaptor) {
		this.nameValueAdapter = nameValueAdaptor;
	}

	public void addAction(String label, Action action) {
		if(actions.size() == 0) {
			this.addChangeListener(new ChangeListener() {
				public void onChange(Widget sender) {
					final String value = getValue();
					if(actions.containsKey(value)) {
						Action action = (Action)actions.get(value);
						action.perform(new AsyncCallback() {
							public void onSuccess(Object result) {
							}
							public void onFailure(Throwable caught) {
								GWT.log("Action "+value+" failed: "+caught, caught);
							}
						});
					}
				}
			});
		}
		actions.put(label, action);
		actionLabels.add(label);
		this.addItem(label);
	}

	public String getNullLabel() {
		return nullLabel;
	}

	public void setNullLabel(String nullLabel) {
		this.nullLabel = nullLabel;
	}
	
    public void setFocusGroup(FocusGroup group) {
        if(this.focusGroup != null)
            this.focusGroup.remove(this);
        this.focusGroup = group;
        if(group != null)
            group.add(this);
    }
}
