package com.habitsoft.kiyaa.views;


import java.util.HashMap;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.HasFocus;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SourcesChangeEvents;
import com.google.gwt.user.client.ui.SourcesFocusEvents;
import com.google.gwt.user.client.ui.SourcesPopupEvents;
import com.google.gwt.user.client.ui.TextBoxBase;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.TextBoxBase.TextAlignConstant;
import com.habitsoft.kiyaa.util.FocusGroup;
import com.habitsoft.kiyaa.util.ModelFilter;
import com.habitsoft.kiyaa.util.NameValueAdapter;
import com.habitsoft.kiyaa.util.ToStringNameValueAdapter;
import com.habitsoft.kiyaa.widgets.TextBox;

/**
 * 
 * 
 */
public class CustomComboBox<T> extends CustomPopup<T> implements View, SourcesChangeEvents, SourcesPopupEvents, SourcesFocusEvents, HasFocus, Focusable {
	boolean stickySearchText;
	
	private final class MyFocusListener implements FocusListener {
		public void onFocus(Widget sender) {
			textboxHasFocus = true;
			if(showOnFocus && !popupShowing) {
			    //GWT.log("Textbox focussed, showing popup", null);
				showPopup(null);
			}
		}

		public void onLostFocus(Widget sender) {
			// If the popup is showing, maybe they clicked on something in the popup (like the scroll bar) and that's why we lost focus.
			// However, if they've tabbed away we want to hide the popup.
			if(!popupShowing) 
			    onTabEnterOrLostFocus(false);
			
			searching = false;
			textboxHasFocus = false;
		}
	}
	
	@Override
	public void onPopupClosed(PopupPanel sender, boolean autoClosed) {
        super.onPopupClosed(sender, autoClosed);
	    if(autoClosed && !textboxHasFocus) {
	        onTabEnterOrLostFocus(false);
	    }
	}
	
    final class MyKeyboardListener extends KeyboardListenerAdapter {
		@Override
		public void onKeyDown(Widget sender, char keyCode, int modifiers) {
			if(keyCode == KeyCodes.KEY_ESCAPE) {
				hidePopup();
				applySearchTextOperation.cancel();
			} else if(keyCode == KeyCodes.KEY_DOWN) {
				showPopup(new AsyncCallback<Void>() {
					public void onFailure(Throwable caught) {
						GWT.log("showPopup() failed in KEY_DOWN handler", caught);
					}
					public void onSuccess(Void arg0) {
						final int newIndex = table.getSelectedIndex()+1;
						if(newIndex < table.getRowCount()) {
							showSelectedIndex(newIndex);
						} else if(table.getRowCount() > 0) {
							showSelectedIndex(0);
						}
					}
				});
			} else if(keyCode == KeyCodes.KEY_UP) {
				showPopup(new AsyncCallback<Void>() {
					public void onFailure(Throwable caught) {
						GWT.log("showPopup() failed in KEY_UP handler", caught);
					}
					public void onSuccess(Void arg0) {
						final int newIndex = table.getSelectedIndex()-1;
						if(newIndex >= 0) {
							showSelectedIndex(newIndex);
						} else if(table.getRowCount() > 0) {
							showSelectedIndex(table.getRowCount()-1);
						}
					}
				});
			} else if(keyCode == KeyCodes.KEY_TAB || keyCode == KeyCodes.KEY_ENTER) {
				onTabEnterOrLostFocus(true);
	            if(focusNextOnEnter && focusGroup != null && keyCode == KeyCodes.KEY_ENTER) {
	                if(modifiers != 0)
	                    focusGroup.focusNextButton();
	                else
	                    focusGroup.focusNext();
	            }
			} else if(keyCode == KeyCodes.KEY_RIGHT || keyCode == KeyCodes.KEY_LEFT) {
			} else {
		        applySearchTextOperation.schedule(250);
			}
		}
		
		@Override
		public void onKeyUp(Widget sender, char keyCode, int modifiers) {
		}

		/**
		 * When user uses the arrow keys to select an item, this method is used
		 * to highlight the currently selected item.
		 */
		protected void showSelectedIndex(int newIndex) {
		    if(selectable) {
    			searching = false;
    			table.setSelectedIndex(newIndex);
    			if(newIndex >= 0) {
    				final UIObject rowWidget = table.getRowUIObject(newIndex);
    				if(rowWidget != null)
    				    container.ensureVisible(rowWidget);
    			}
		    }
		}

	}
    NameValueAdapter<T> alternateNameValueAdapter = null;
	
	private Timer applySearchTextOperation = new Timer() {
		@Override
		public void run() {
			applySearchText(true);
		}
	};

	NameValueAdapter<T> nameValueAdapter = ToStringNameValueAdapter.getInstance();
    protected HashMap<String,String> nameValueMap = new HashMap<String, String>();
	
	boolean showOnFocus = true;
	final TextBoxBase textbox;
	boolean textboxHasFocus = false;
	boolean searching = false;
	private HashMap<String,Integer> valueIndexMap = new HashMap<String, Integer>();
	private FocusGroup focusGroup;
	boolean focusNextOnEnter=true;
    protected String currentValue;
	protected boolean searchable=true;
	
	public CustomComboBox() {
		this(new TextBox());
	}
	
	public CustomComboBox(TextBoxBase textbox) {
		this.textbox = textbox;
		
		textbox.setStylePrimaryName("ui-combobox");
		textbox.addKeyboardListener(new MyKeyboardListener());
		textbox.addFocusListener(new MyFocusListener());
		textbox.addClickListener(new ClickListener() {
			public void onClick(Widget sender) {
				showPopup(null);
			}
		});
		textbox.addChangeListener(new ChangeListener() {
			public void onChange(Widget sender) {
				sendChangeEvent();
			}
		});
		// Disable autocomplete on our custom combobox, since autocomplete interferes with our use of the cursor keys!
		DOM.setElementProperty(textbox.getElement(), "autocomplete", "off");
	}
	
	public void addKeyboardListener(KeyboardListener listener) {
		textbox.addKeyboardListener(listener);
	}
	public void addStyleDependentName(String styleSuffix) {
		textbox.addStyleDependentName(styleSuffix);
	}
	
	public void addStyleName(String style) {
		textbox.addStyleName(style);
	}

	private void applySearchText(boolean fromTyping) {
		// Select any exact match for the search string, if there is one; otherwise select nothing
		final String text = getText();

		if (text == null) {
			return;
		}
		
		final String value = text==null?null:nameValueMap.get(text.toLowerCase());
		if(!searchable && value == null)
		    return; // Only take exact matches if searchable is off
		// GWT.log("Selecting value "+value+" based on text "+text.toLowerCase(), null);
		selectValue(value, null, false);
		
		if(fromTyping) {
			if(searchable) {
			    searching = true;
			    applyFilter(true);
			}
	        
			showPopup(new AsyncCallback<Void>() {
				public void onFailure(Throwable caught) {
					GWT.log("showPopup() failed in typing handler", caught);
				}
				public void onSuccess(Void arg0) {
					searching = searchable;
				}
			});
		}
	}

	@Override
    public void clearFields() {
        currentValue = null;
        searching = false;
        super.clearFields();
        textbox.setText("");
    }

	@Override
    protected void createTableView(AsyncCallback<Void> callback) {
        super.createTableView(callback);
        table.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                if(!textboxHasFocus && !searching) {
                    //GWT.log("table changed, focussing to textbox and updating model from table.", null);
                    //textbox.setFocus(true);
                    useModelFromTable();
                }
            }
        });
    }

	public void focus() {
		setFocus(true);
	}

	public NameValueAdapter<T> getAlternateNameValueAdapter() {
		return alternateNameValueAdapter;
	}

	public String getCurrentLabel() {
        return textbox.getText();
    }

	@Override
    protected ViewFactory getDefaultViewFactory() {
        return new NameViewFactory<T>(nameValueAdapter);
    }

	public String getSearchText() {
		if(selectedModel != null)
			return null;
		String text = getText();
		if(text.length() > 0)
			return text;
		return null;
	}

	public String getStyleName() {
		return textbox.getStyleName();
	}

	public String getStylePrimaryName() {
		return textbox.getStylePrimaryName();
	}

	public int getTabIndex() {
		return textbox.getTabIndex();
	}

	public String getText() {
		return textbox.getText();
	}

	public TextBoxBase getTextbox() {
		return textbox;
	}

	public String getTitle() {
		return textbox.getTitle();
	}

	public Widget getViewWidget() {
        return textbox;
    }

	public boolean isShowOnFocus() {
		return showOnFocus;
	}

	@Override
    protected void modelsChanged(T[] models) {
	    if(selectable || clickable) {
            valueIndexMap.clear();
            nameValueMap.clear();
            for(int i=0; i < models.length; i++) {
                T model = models[i];
                String value = nameValueAdapter.getValue(model);
                valueIndexMap.put(value, i);
                nameValueMap.put(nameValueAdapter.getName(model).toLowerCase(), value);
                if(alternateNameValueAdapter != null) {
                    String altValue = alternateNameValueAdapter.getValue(model);
                    String altName = alternateNameValueAdapter.getName(model);
                    if(altName != null && altName.length() > 0) {
                        valueIndexMap.put(altValue, i);
                        nameValueMap.put(altName.toLowerCase(), altValue);
                    }
                }
            }
            // Try to initialize the value by matching a model's text
            String text = getText();
            if(currentValue == null) {
                if(text != null)
                    currentValue = nameValueMap.get(text.toLowerCase());
            }
            
            // Make sure we have the right model instance selected
            selectValue(currentValue, null, !isOptional() || text == null || text.length() == 0);
	    }
    }

	private void onTabEnterOrLostFocus(final boolean tabOrEnter) {
	    // Popup might become invisible before the timer expires (it's actually quite likely that it will)
        final boolean shouldUseModelFromTable = popupShowing && !searching && table != null && table.getSelectedModel() != null;
        
        
	    // Have to schedule a timer so that if we just lost focus due to the user clicking on something we won't
        // hide the popup before the click is processed.
        if(shouldUseModelFromTable) {
            useModelFromTable();
        } else {
            // If they were typing in a name, match it before we clear it
    		applySearchTextOperation.cancel();
        	applySearchText(false);
        	if(tabOrEnter)
        		sendChangeEvent();
        }
        if(selectedModel != null || stickySearchText == false) {
            setText(nameValueAdapter.getName(selectedModel));
        	searching = false;
        }
        
        hidePopup();
    }
	
	public void removeKeyboardListener(KeyboardListener listener) {
		textbox.removeKeyboardListener(listener);
	}
	
	public void removeStyleDependentName(String styleSuffix) {
		textbox.removeStyleDependentName(styleSuffix);
	}

	public void removeStyleName(String style) {
		textbox.removeStyleName(style);
	}

	public void setAccessKey(char key) {
		textbox.setAccessKey(key);
		
	}

	/**
	 * Note: set the name value adapters before setting the models,
	 * since during setModels() is when the name value adapter is used!
	 */
	public void setAlternateNameValueAdapter(NameValueAdapter<T> alternateNameValueAdapter) {
		this.alternateNameValueAdapter = alternateNameValueAdapter;
	}

	public void setEnabled(boolean enabled) {
		textbox.setEnabled(enabled);
	}
	public void setFocus(boolean focused) {
		textbox.setFocus(focused);
	}
	public void setHeight(String height) {
		textbox.setHeight(height);
	}
	public void setMaxLength(int length) {
		((TextBox)textbox).setMaxLength(length);
	}
	public void setName(String name) {
		textbox.setName(name);
	}
	public void setReadOnly(boolean readOnly) {
		textbox.setReadOnly(readOnly);
	}
	public void setSearchText(String text) {
		if(selectedModel == null) {
			setText(text);
		}
	}
	public void setShowOnFocus(boolean showOnFocus) {
		this.showOnFocus = showOnFocus;
	}
	public void setTabIndex(int index) {
		textbox.setTabIndex(index);
	}
	public void setText(String text) {
		// Select any exact match for the search string, if there is one; otherwise select nothing
		if(nameValueMap != null && (selectable || clickable))
			selectValue(nameValueMap.get(text==null?"":text.toLowerCase()), null, false);
		
		// Regardless of whether we selected a value, set the text to what they asked for
		textbox.setText(text);
	}
	public void setTextAlignment(TextAlignConstant align) {
		textbox.setTextAlignment(align);
	}
    public void setTitle(String title) {
		textbox.setTitle(title);
	}
    public void setVisible(boolean visible) {
		textbox.setVisible(visible);
	}
    public void setVisibleLength(int length) {
		((TextBox)textbox).setVisibleLength(length);
	}
	
    public void setWidth(String width) {
		textbox.setWidth(width);
	}
    
    @Override
    protected void showPopup(AsyncCallback<Void> callback) {
	    showPopup(callback, textbox.getAbsoluteLeft(), textbox.getAbsoluteTop()+textbox.getOffsetHeight());
	}
    
    void useModelFromTable() {
		setSelectedModel(table.getSelectedModel());
		sendChangeEvent();
	}

    protected boolean selectValue(String value, T model, boolean updateText) {
        if(!(selectable || clickable))
            return false;
        if(value == null && model != null)
            value = nameValueAdapter.getValue(model);
        int selectedIndex = indexOfValue(value);
        if(model == null) {
            T[] models = getModels();
            if(models != null) {
                if(selectedIndex == -1 && !optional && models.length > 0) {
                    selectedIndex = 0;
                }
                if(selectedIndex != -1) {
                    try {
                        model = models[selectedIndex];
                        if(value == null)
                            value = nameValueAdapter.getValue(model);
                    } catch(IndexOutOfBoundsException e) {
                        // oh well, we tried ...
                    }
                }
            }
        }
        
        // Don't allow a null model/value if we're not supposed to.
        if(selectedIndex == -1 && !optional) {
            if(value != null)
                currentValue = value;
            return false;
        }
        
        boolean result = (model != this.selectedModel);
        this.selectedModel = model;
        if(model != null)
            searching = false;
        if(updateText)
            textbox.setText(nameValueAdapter.getName(selectedModel));
        currentValue = value;
        if(table != null) {
            int[] itemIndexesAfterFiltering = table.getItemIndexesAfterFiltering();
            if(selectedIndex != -1 && itemIndexesAfterFiltering != null && selectedIndex < itemIndexesAfterFiltering.length) {
                selectedIndex = itemIndexesAfterFiltering[selectedIndex];
            }
            table.setSelectedIndex(selectedIndex);
            ensureSelectedIndexIsVisible();
        }
        return result;
    }

    private int indexOfValue(String value) {
        Integer selectedIndexObj = ((Integer)valueIndexMap.get(value));
        int selectedIndex = selectedIndexObj==null?-1:selectedIndexObj.intValue();
        return selectedIndex;
    }

    public void addFocusListener(FocusListener listener) {
        textbox.addFocusListener(listener);
    }

    public void removeFocusListener(FocusListener listener) {
        textbox.removeFocusListener(listener);
    }
    public void addClickListener(ClickListener listener) {
        textbox.addClickListener(listener);
    }

    public int getSelectedIndex() {
        return indexOfValue(currentValue);
    }

    public void setSelectedModel(T selectedItem) {
        // Check if it's already been set
        if(selectedItem == selectedModel || (selectedModel != null && selectedModel.equals(selectedItem)))
            return;
        selectValue(null, selectedItem, true);
        enqueueHidePopup(10);
    }

    public void setValue(String value) {
        if(currentValue == value || 
            (currentValue != null 
                && currentValue.equals(value)))
            return;
        selectValue(value, null, true);
        enqueueHidePopup(10);
    }
    public String getValue() {
        return currentValue;
    }

    @Override
    protected boolean isFiltered() {
        return searching && !"".equals(getText().trim());
    }
    
    @Override
    protected ModelFilter getFilter() {
        final String text = getText();
        // Escape any special regex characters in their search pattern
        final String[] words = text.toLowerCase().split("\\s+");
        return new ModelFilter<T>() {
        	boolean containsAllWords(String text) {
        		for(String word : words) {
        			if(text.contains(word))
        				return true;
        		}
        		return false;
        	}
            public boolean includes(T model) {
                String label = nameValueAdapter!=null?nameValueAdapter.getName(model):model.toString();
                boolean result = containsAllWords(label.toLowerCase());
                if(!result && alternateNameValueAdapter != null) {
                    label = alternateNameValueAdapter.getName(model);
                    result = label != null && containsAllWords(label.toLowerCase());
                }
                //GWT.log("Does "+text+" match "+label+"? "+result, null);
                return result;
            }
        };
    }

    public Timer getApplySearchTextOperation() {
        return applySearchTextOperation;
    }

    public void setApplySearchTextOperation(Timer applySearchTextOperation) {
        this.applySearchTextOperation = applySearchTextOperation;
    }

    public NameValueAdapter<T> getNameValueAdapter() {
        return nameValueAdapter;
    }

    /**
     * Note: set the name value adapters before setting the models,
     * since during setModels() is when the name value adapter is used!
     */
    public void setNameValueAdapter(NameValueAdapter<T> nameValueAdapter) {
        this.nameValueAdapter = nameValueAdapter;
    }

    public HashMap<String, String> getNameValueMap() {
        return nameValueMap;
    }

    public void setNameValueMap(HashMap<String, String> nameValueMap) {
        this.nameValueMap = nameValueMap;
    }

    public boolean isTextboxHasFocus() {
        return textboxHasFocus;
    }

    public void setTextboxHasFocus(boolean textboxHasFocus) {
        this.textboxHasFocus = textboxHasFocus;
    }

    public boolean isSearching() {
        return searching;
    }

    public void setSearching(boolean typing) {
        this.searching = typing;
    }

    public HashMap<String, Integer> getValueIndexMap() {
        return valueIndexMap;
    }

    public void setValueIndexMap(HashMap<String, Integer> valueIndexMap) {
        this.valueIndexMap = valueIndexMap;
    }

    public void setFocusGroup(FocusGroup group) {
        if(this.focusGroup != null)
            this.focusGroup.remove(textbox);
        this.focusGroup = group;
        if(group != null)
            group.add(textbox);
    }

    public boolean isFocusNextOnEnter() {
        return focusNextOnEnter;
    }

    public void setFocusNextOnEnter(boolean focusNextOnEnter) {
        this.focusNextOnEnter = focusNextOnEnter;
    }

    public boolean isSearchable() {
        return searchable;
    }

    public void setSearchable(boolean searchable) {
        this.searchable = searchable;
    }
    
    public String getInnerHelp() {
        if(textbox instanceof TextBox) {
            return ((TextBox)textbox).getInnerHelp();
        } else {
            return null;
        }
    }
    public void setInnerHelp(String helpText) {
        if(textbox instanceof TextBox) {
            ((TextBox)textbox).setInnerHelp(helpText);
        }
    }
    
    @Override
    public void save(AsyncCallback<Void> callback) {
		applySearchTextOperation.cancel();
		applySearchText(false);
    	super.save(callback);
    }

    /**
     * Set to true if you want to preserve search text
     * when the TextBox loses focus; otherwise, the text
     * will be set to the selected model's text as returned
     * by the nameValueAdapter, even if the selectedModel
     * is null.
     */
	public boolean isStickySearchText() {
		return stickySearchText;
	}

	public void setStickySearchText(boolean stickySearchText) {
		this.stickySearchText = stickySearchText;
	}
}
