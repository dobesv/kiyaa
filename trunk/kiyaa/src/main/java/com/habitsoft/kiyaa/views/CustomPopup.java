package com.habitsoft.kiyaa.views;

import java.util.ArrayList;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ChangeListenerCollection;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.PopupListener;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import com.habitsoft.kiyaa.metamodel.Action;
import com.habitsoft.kiyaa.metamodel.ActionSeries;
import com.habitsoft.kiyaa.metamodel.Value;
import com.habitsoft.kiyaa.util.AsyncCallbackDirectProxy;
import com.habitsoft.kiyaa.util.AsyncCallbackFactory;
import com.habitsoft.kiyaa.util.AsyncCallbackProxy;
import com.habitsoft.kiyaa.util.ModelFilter;
import com.habitsoft.kiyaa.util.ToStringNameValueAdapter;
import com.habitsoft.kiyaa.util.Name;

public class CustomPopup<T> implements PopupListener {

    
    protected final PopupPanel popup = new PopupPanel(true);
    protected final ScrollPanel container = new ScrollPanel();
    protected final FlowPanel flow = new FlowPanel();
    protected BaseCollectionView<T> table;
    private ArrayList<Action> actions = new ArrayList<Action>();
    private ArrayList<Action> visibleActions = new ArrayList<Action>();
    private ActionSeries actionTests;
    protected T selectedModel;
    private ChangeListenerCollection changeListeners = new ChangeListenerCollection();
    protected boolean optional = true;
    private T[] models;
    private Value<T[]> modelsValue;
    private ArrayList<ViewFactory<ModelView<T>>> columns = new ArrayList<ViewFactory<ModelView<T>>>();
    private View emptyContent;
    protected boolean selectable = true;
    protected boolean clickable = false;
    private Timer filterOperation = new Timer() {
    		@Override
    		public void run() {
    			applyFilter(true);
    		}
    	};
    protected boolean popupShowing = false;
    private boolean applyDefaultFilter = true;
    public final ModelFilter<T> NOOP_FILTER = new ModelFilter<T>() {
        public boolean includes(T model) {
            return true;
        }
    };
    private ModelFilter<T> defaultFilter = NOOP_FILTER;
    private Timer hidePopupOperation = new Timer() {
    		@Override
    		public void run() {
    		    //GWT.log("hidePopupOperation.run", null);
    			hidePopup();
    		}
    	};
    protected Anchor removeFilterActionLabel;

    
    /**
     * A subclass can override this to disable the popup in some
     * circumstances - for example, if there's nothing to show in the
     * popup!
     * 
     * @return true if the popup should be shown
     */
    protected boolean shouldShowPopup() {
    	return (models != null && models.length > 0) || !visibleActions.isEmpty();
    }

    /**
     * If shouldShowPopup(), and the popup is not showing, show the popup.
     * 
     * If not shouldShowPopup, hide the popup if it is showing.
     * 
     * It's acceptable to pass null for the callback, if there's no follow-up operation
     * pending which depends on the pop-up being shown.
     */
    protected void showPopup(AsyncCallback<Void> callback, final int left, final int top) {
    	try {
    	    if(shouldShowPopup()) {
        	    if(!popupShowing) {
                    popupShowing = true;
            		if(table == null) {
            			createTableView(new AsyncCallbackDirectProxy<Void>(callback) {
            				@Override
            				public void onSuccess(Void arg0) {
            					if(table == null) throw new NullPointerException();
            					showPopupImpl(left, top);
            					returnSuccess(null);
            				}
            			});
            		} else {
                        showPopupImpl(left, top);
                		if(callback != null)
                			callback.onSuccess(null);
            		}
        	    } else {
                    if(callback != null)
                        callback.onSuccess(null);
        	    }
            } else {
                hidePopup();
                if(callback != null)
                    callback.onSuccess(null);
            }
    	} catch(Throwable caught) {
    		if(callback != null)
    			callback.onFailure(caught);
    		else caught.printStackTrace();
    	}
    }

    private void showPopupImpl(final int left, final int top) {
        hidePopupOperation.cancel();
        popupShowing = true;
        popup.setPopupPosition(left, top);
        popup.show();
        ensureSelectedIndexIsVisible();
    }

    /**
     * Show the popup at the current mouse position.  Assumes a mouse event
     * is currently being processed.
     */
    protected void showPopup(AsyncCallback<Void> callback) {
        Event event = Event.getCurrentEvent();
        showPopup(callback, event.getClientX()+Window.getScrollLeft(), event.getClientY()+Window.getScrollTop());
    }
    
    protected ViewFactory<ModelView<T>> getDefaultViewFactory() {
        return new NameViewFactory<T>(ToStringNameValueAdapter.<T>getInstance());
    }
    
    protected void createTableView(AsyncCallback<Void> callback) {
    	if(columns.isEmpty()) {
    		ListView<T> list = new ListView<T>();
    		list.setViewFactory(getDefaultViewFactory());
    		list.setEmptyContent(emptyContent);
    		//table.addColumn(new NameViewFactory<T>(nameValueAdapter));
    		table = list;
    	} else if(columns.size() == 1) {
    		ListView<T> list = new ListView<T>();
    		list.setViewFactory(columns.get(0));
    		list.setEmptyContent(emptyContent);
    		table = list;
    	} else {
    		TableView<T> t = new TableView<T>();
    		for(ViewFactory column: columns) {
    			t.addColumn(column);
    		}
    		t.setEmptyContent(emptyContent);
    		table = t;
    	}
    	// Defer adding the table to the DOM until after it's loaded
    	callback = new AsyncCallbackDirectProxy<Void>(callback) {
    		@Override
    		public void onSuccess(Void result) {
    			if(table != null) // Amazingly, the table could be set to null before we get here
    				flow.insert(table.getViewWidget(), 0);
    			super.onSuccess(result);
    		}
    	};
    	table.addClickListener(new ClickListener() {
    		public void onClick(Widget sender) {
    		    //GWT.log("Table clicked, enqueueHidePopup(10)", null);
    			enqueueHidePopup(10);
    		}
    	});
    	table.setSelectable(selectable);
    	table.setClickable(clickable);
    	if(models != null) {
    		updateModelsInTable();
    	}
    	//GWT.log("loading table in custom popup.createTableView()", null);
    	table.load(callback);
    }

    public void hidePopup() {
    	if(popupShowing) {
    		//GWT.log("Hide popup "+this, new Exception());
    		table.getHoverGroup().setActive(null);
    		popup.hide();
    		popupShowing = false;
    	}
    }

    protected void enqueueHidePopup(int delay) {
    	//GWT.log("Enqueue hide popup "+this+" after "+delay, new Exception());
    	hidePopupOperation.schedule(delay);
    }

    protected void cancelPendingHidePopup() {
        hidePopupOperation.cancel();
    }
    
    public void clearFields() {
    	selectedModel = null;
    	if(table != null) {
    		table.getViewWidget().removeFromParent();
    		table = null;
    	}
    }

    public void addPopupListener(PopupListener listener) {
    	popup.addPopupListener(listener);
    }

    public void removePopupListener(PopupListener listener) {
    	popup.removePopupListener(listener);
    }

    public native boolean isIE() /*-{
        return /msie/i.test(navigator.userAgent) && !/opera/i.test(navigator.userAgent);
    }-*/;
    
    public void setPopupHeight(String height) {        
        // Also setup a scroll bar, assuming that the height is being limited
        DOM.setStyleAttribute(container.getElement(), "overflowX", "hidden");
        DOM.setStyleAttribute(container.getElement(), "overflowY", "scroll");
        if(isIE()) // HACK for IE's broken way of doing layout with scroll bars
            DOM.setStyleAttribute(container.getElement(), "paddingRight", "20px");
    	popup.setHeight(height);
    }

    public void setPopupWidth(String width) {
    	popup.setWidth(width);
    }

    public void setPopupClass(String styleClass) {
    	popup.setStylePrimaryName(styleClass);
    }

    public T[] getModels() {
    	return models;
    }

    protected void modelsChanged(T[] models) {
        
    }
    public void setModels(final T[] models) {
    	if(this.models == models) {
    		return;
    	}
    	this.models = models;
    	if(table != null) {
    		updateModelsInTable();
    	}
        modelsChanged(models);
    }

    private void updateModelsInTable() {
    	applyFilter(false);
        table.setModels(models);
    }

    public void addChangeListener(ChangeListener listener) {
    	changeListeners.add(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
    	changeListeners.remove(listener);
    }

    @Name("action")
    public void addAction(@Name("label") final String label, @Name("action") final Action action) {
    	addAction(label, action, null);
    }

    @Name("action")
    public void addAction(@Name("label") String label, @Name("action") Action action, @Name("test") Value<Boolean> test) {
    	addAction(label, action, test, true);
    }

    /**
     * Add an action to the menu.
     * 
     * @param label
     * @param action
     * @param test If non-null, the action will only show when the value is true
     */
    public Anchor addAction(@Name("label") final String label, @Name("action") final Action action, @Name("test") final Value<Boolean> test, @Name("hideOnClick") final boolean hideOnClick) {
    	final Anchor widget = new Anchor();
    	widget.setText(label);
    	widget.setStyleName("action");
    	flow.add(widget);
    	actions.add(action);
        //GWT.log("Action added ..."+label, null);
    	widget.addClickListener(new ClickListener() {
    		public void onClick(Widget sender) {
                //GWT.log("Action clicked ..."+label, null);
    			if(hideOnClick)
    				hidePopup();
    			action.perform(AsyncCallbackFactory.<Void>defaultNewInstance());
    		}
    	});
    	if(test != null) {
    		if(actionTests == null) actionTests = new ActionSeries();
    		actionTests.add(new Action() {
    			@Override
    			public void perform(AsyncCallback<Void> callback) {
    				test.getValue(new AsyncCallbackProxy<Boolean,Void>(callback) {
    					@Override
    					public void onSuccess(Boolean result) {
    					    if(widget.isVisible() != result)
    					        widget.setVisible(result); 
					        if(result && !visibleActions.contains(action))
					            visibleActions.add(action);
					        else if(!result && visibleActions.contains(action))
					            visibleActions.remove(action);
    						returnSuccess(null);
    					}
    				});
    			}
    		});
    	} else visibleActions.add(action);
    	return widget;
    }

    protected Anchor restoreFilterActionLabel;

    public void onPopupClosed(PopupPanel sender, boolean autoClosed) {
        popupShowing = false;
    }

    public CustomPopup() {
        popup.addPopupListener(this);
        popup.setStylePrimaryName("ui-dropdown");
        popup.setWidget(container);
        container.setWidget(flow);
    }

    @Name("removeFilterAction")
    public void addRemoveFilterAction(@Name("removeLabel") String removeLabel, @Name("restoreLabel") String restoreLabel) {
    	if(removeFilterActionLabel != null) {
    		removeFilterActionLabel.setText(removeLabel);
    	} else {
    		removeFilterActionLabel = addAction(removeLabel, new Action() {
    			@Override
    			public void perform(AsyncCallback<Void> callback) {
    				applyDefaultFilter = false;
    				if(restoreFilterActionLabel != null)
    				    restoreFilterActionLabel.setVisible(true);
    				removeFilterActionLabel.setVisible(false);
    				applyFilter(true);
    				callback.onSuccess(null);
    			}
    		}, new Value<Boolean>() {
    			public void getValue(AsyncCallback<Boolean> callback) {
    				callback.onSuccess(applyDefaultFilter);
    			}
    			public void setValue(Boolean newValue, AsyncCallback<Void> callback) {
    				applyDefaultFilter = ((Boolean)newValue).booleanValue();
                    applyFilter(true);
    				callback.onSuccess(null);
    			}
    		}, false);
    	}
    	if(restoreFilterActionLabel != null) {
    		restoreFilterActionLabel.setText(restoreLabel);
    	} else {
    		restoreFilterActionLabel = addAction(restoreLabel, new Action() {
    			@Override
    			public void perform(AsyncCallback<Void> callback) {
    				applyDefaultFilter = true;
                    if(removeFilterActionLabel != null)
                        removeFilterActionLabel.setVisible(true);
                    restoreFilterActionLabel.setVisible(false);
    				applyFilter(true);
    				callback.onSuccess(null);
    			}
    		}, new Value<Boolean>() {
    			public void getValue(AsyncCallback<Boolean> callback) {
    				callback.onSuccess(!applyDefaultFilter);
    			}
    			public void setValue(Boolean newValue, AsyncCallback<Void> callback) {
    				applyDefaultFilter = !((Boolean)newValue).booleanValue();
    				applyFilter(true);
    				callback.onSuccess(null);
    			}
    		}, false);
    	}
    }

    @Name("column")
    public void addColumn(ViewFactory<ModelView<T>> viewFactory) {
        if(viewFactory == null) throw new NullPointerException("viewFactory");
    	columns.add(viewFactory);
    	if(table != null) {
    		if(table instanceof TableView<?>)
    			((TableView<T>)table).addColumn(viewFactory);
    		else
    			createTableView(AsyncCallbackFactory.<Void>defaultNewInstance());
    	}
    }

    public T getSelectedModel() {
    	return selectedModel;
    }

    protected void ensureSelectedIndexIsVisible() {
    	if(table.getSelectedIndex() >= 0  && table.getRowCount() > 0) {
    		final UIObject rowUIObject = table.getRowUIObject(table.getSelectedIndex());
    		if(rowUIObject != null)
    		    container.ensureVisible(rowUIObject);
            // TODO Ensure that the part of the container which has the ui object is also visible.  #584
    	}
    }

    protected void sendChangeEvent() {
    	changeListeners.fireChange(null);
    }

    protected void enqueueApplyFilter() {
    	// Calling schedule will re-schedule the timer; any pending event is cancelled first,
    	// then the timer will run after 250ms.  Basically we want to only run the timer after
    	// the user stops typing for a little bit.
    	filterOperation.schedule(250);
    }

    public ModelFilter<T> getDefaultFilter() {
    	return defaultFilter;
    }

    public void setDefaultFilter(ModelFilter<T> defaultFilter) {
    	this.defaultFilter = defaultFilter;
    }

    public void load(AsyncCallback<Void> callback) {
    	if(actionTests != null)
    		callback = actionTests.performOnSuccess(callback);
    	
    	if(modelsValue != null) {
    	    modelsValue.getValue(new AsyncCallbackProxy<T[],Void>(callback) {
    	        @Override
                public void onSuccess(T[] result) {
    	            setModels(result);
    	            //if(table != null) GWT.log("loading table in custom popup.load() (async models)", null);
    	            loadTable(takeCallback());
    	        }
    	    });
    	} else {
    	    loadTable(callback);
    	}
    }

    private void loadTable(AsyncCallback<Void> callback) {
        if(table != null) {
    		table.load(callback);
    	} else {
    		createTableView(callback);
    	}
    }

    public void save(AsyncCallback<Void> callback) {
    	if(table != null) {
    		table.save(callback);    		
    	} else {
    		callback.onSuccess(null);
    	}
    }

    public boolean isOptional() {
    	return optional;
    }

    public void setOptional(boolean optional) {
    	this.optional = optional;
    }

    public View getEmptyContent() {
    	return emptyContent;
    }

    public void setEmptyContent(View emptyContent) {
    	this.emptyContent = emptyContent;
    }

    protected boolean isFiltered() {
        return false;
    }
    
    protected ModelFilter<T> getFilter() {
        return null;
    }

    protected void applyFilter(final boolean loadTable) {
        // Don't apply the filter if we haven't created the table
        if(table == null)
            return;
        //GWT.log("Filtering...", null);
        AsyncCallback<Void> callback = 
            new AsyncCallback<Void>() {
            public void onSuccess(Void result) {
                if(loadTable) {
                    //GWT.log("Loading table in applyFilter()", null);
                    loadTable(AsyncCallbackFactory.<Void>defaultNewInstance());
                }
            }
            public void onFailure(Throwable caught) {
                GWT.log("Filter operation failed", caught);
            }
        };
        if(isFiltered()) {
            table.setFilter(getFilter(), callback);
        } else {
            table.setFilter(applyDefaultFilter?defaultFilter:NOOP_FILTER, callback);
        }
    }

    public boolean isPopupShowing() {
        return popupShowing;
    }

    public void setPopupShowing(boolean popupShowing) {
        this.popupShowing = popupShowing;
    }

    public void setModels(Value<T[]> models) {
        modelsValue = models;
    }

    public boolean isSelectable() {
        return selectable;
    }

    public void setSelectable(boolean selectable) {
        this.selectable = selectable;
        if(table != null)
            table.setSelectable(selectable);
    }

    public boolean isClickable() {
        return clickable;
    }

    public void setClickable(boolean clickable) {
        this.clickable = clickable;
        if(table != null)
            table.setClickable(clickable);
    }

    
}