package com.habitsoft.kiyaa.views;

import java.util.ArrayList;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.IncrementalCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ChangeListenerCollection;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.ClickListenerCollection;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.SourcesChangeEvents;
import com.google.gwt.user.client.ui.SourcesClickEvents;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import com.habitsoft.kiyaa.metamodel.ModelCollection;
import com.habitsoft.kiyaa.util.AsyncCallbackGroup;
import com.habitsoft.kiyaa.util.AsyncCallbackGroupMember;
import com.habitsoft.kiyaa.util.AsyncCallbackProxy;
import com.habitsoft.kiyaa.util.HoverStyleHandler;
import com.habitsoft.kiyaa.util.ModelFilter;
import com.habitsoft.kiyaa.widgets.ScrollAutoLoader.Loader;

public abstract class BaseCollectionView<T> extends FlowPanel implements View, Loader, SourcesChangeEvents, SourcesClickEvents {

	protected T selectedModel;
	protected int selectedRow = -1;
	protected ArrayList<T> items = new ArrayList<T>();
	protected ArrayList<T> unfilteredItems = null;
	protected int[] itemIndexesAfterFiltering;
	protected ModelCollection collection;
	protected Object loadedCollectionId;
	//protected ScrollAutoLoader scrollAutoLoader;
	protected int increment = 0;
	protected int maxHeight = 0;
	protected int startOffset = 0;
	protected int totalItems = -1;
	protected boolean selectable = false;
	protected boolean clickable = false;
	T[] models;
	ModelFilter filter;
	ClickListenerCollection clickListeners;
	ChangeListenerCollection changeListeners;
	protected HoverStyleHandler.Group hoverGroup = new HoverStyleHandler.Group();
    private IncrementalCommand loadModelsCommand;
    private boolean modelsChanged;
	
	public BaseCollectionView() {
		super();
	}

	public void addChangeListener(ChangeListener listener) {
		if(changeListeners == null) changeListeners = new ChangeListenerCollection();
		changeListeners.add(listener);
	}

	public void removeChangeListener(ChangeListener listener) {
		if(changeListeners == null) return;
		changeListeners.remove(listener);
	}

	/**
	 * Show an item at the given index.  The index is an offset into
	 * the array of visible items at which the object is inserted.
	 * 
	 * @param i Index of the item to show, amongst visible items
	 * @param object Model object which should be shown
	 * @param callback 
	 */
	protected abstract void showItem(int i, T model, AsyncCallback callback);
	
	/**
	 * Remove an item from the view.  Items following the removed one should
	 * be shifted up to make room.
	 * @param i
	 */
	protected abstract void hideItem(int i);
	
	/**
	 * Subclass must provide a widget which is the "primary" widget for this
	 * collection view.
	 */
	protected abstract Widget getWidget();
	
	/**
	 * Get the number of rows displayed currently
	 */
	public int getRowCount() {
		return items.size();
	}
	
	public void addModel(T object, AsyncCallback callback) {
		addItem(items.size()+startOffset, object, callback);
	}
	
	protected void addItem(int i, T object, AsyncCallback callback) {
		items.add(i-startOffset, object);
		showItem(i-startOffset, object, callback);
		//GWT.log("Showing new item "+object, new Exception());
	}
	
	protected void removeItem(int i) {
	    try {
    		items.remove(i-startOffset);
    		hideItem(i-startOffset);
	    } catch(IndexOutOfBoundsException e) {
	        // already gone I suppose
	    }
	}
	
	protected void replaceItem(int i, T object, AsyncCallback callback) {
		
		final int row = i-startOffset;
		Object existing = items.get(row);
		if(existing == object) {
			callback.onSuccess(null);
			return; // Already there
		}
		items.set(row, object);
		setItem(row, object, callback);
	}
	protected abstract void setItem(int i, T object, AsyncCallback callback);
	
	public ModelCollection getCollection() {
		return collection;
	}

	/**
	 * Change the collection being displayed by this list.  The callback
	 * is invoked when loading is complete.
	 */
	public void setCollection(ModelCollection collection, AsyncCallback callback) {
		if(collection != this.collection) {
	        this.models = null;
	        this.collection = collection;
	        startOffset = 0;
		}
		callback.onSuccess(null);
	}

	public T getSelectedModel() {
		return selectedModel;
	}

	public void setSelectedModel(Object selectedItem) {
		if(selectedItem == selectedModel)
			return;
		if(selectedItem == null) {
			setSelectedIndex(-1);
			this.selectedModel = null;
		} else {
			setSelectedIndex(items.indexOf(selectedItem));
		}
	}
	
	public boolean isSelectable() {
		return selectable;
	}

	public void setSelectable(boolean selectable) {
		this.selectable = selectable;
		if(selectable) {
			getWidget().addStyleDependentName("selectable");
		} else {
			selectedModel = null;
			getWidget().removeStyleDependentName("selectable");
		}
	}

    public boolean isClickable() {
        return clickable;
    }

    public void setClickable(boolean clickable) {
        this.clickable = clickable;
        if(selectable) {
            getWidget().addStyleDependentName("clickable");
        } else {
            getWidget().removeStyleDependentName("clickable");
        }
    }
	
	public void sendChangeEvent() {
		if(changeListeners == null) return;
		changeListeners.fireChange(this);
	}

	public int getIncrement() {
		return increment;
	}
	public void setIncrement(int increment) {
		this.increment = increment;
		//setupScrollAutoLoader();
	}

	/**
	 * Subclasses that support incremental loading can use the "scroll-to-load"
	 * incremental loading method.
	 */
	/*
	public void setupScrollAutoLoader() {
		if(scrollAutoLoader == null) {
			if(increment <= 0 && maxHeight == 0) return;
			scrollAutoLoader = new ScrollAutoLoader(getScrollElement(), this, increment, maxHeight, 1000);
			if(isAttached())
				scrollAutoLoader.fill(null);
		} else if(increment <= 0 && maxHeight == 0) {
		} else {
			scrollAutoLoader.setLimit(increment);
			scrollAutoLoader.setMaxHeight(maxHeight);
		}
	}
*/
	
	protected abstract Element getScrollElement();
	
	public void load(final int offset, final int limit, AsyncCallback callback) {
		if(collection == null) {
			callback.onSuccess(null);
			return;
		}
		
		AsyncCallbackGroup group = new AsyncCallbackGroup("Load offset="+offset+" limit="+limit);
		collection.getLength(new AsyncCallbackGroupMember<Integer>(group) {
			@Override
			public void onSuccess(Integer length) {
				totalItems = (length == null?-1:length);
				super.onSuccess(null);
			}
			
			@Override
			public void onFailure(Throwable caught) {
				totalItems = -1;
				super.onFailure(caught);
			}
		});

		collection.load(offset, limit, new AsyncCallbackGroupMember<T[]>(group, getClass().getName()+".collection.load()") {
			@Override
			public void onSuccess(T[] models) {				
				//GWT.log("Loading list ... got "+models.length+" results attached = "+isAttached(), null);
				showLoadedModels(models, offset, limit, group.member(getClass().getName()+".showLoadedModels("+models+")"));
				super.onSuccess(null);
			}
		});
		group.ready(callback);
	}

	public void load(final AsyncCallback<Void> callback) {
	    if(collection != null) {
	        Object collectionId = collection.getId();
            boolean collectionChanged = collectionId != null && !collectionId.equals(loadedCollectionId);
            if(collectionChanged) {
                //GWT.log("Collection changed.  old id = "+loadedCollectionId+" new id "+collectionId+" collection "+collection, null);
                this.loadedCollectionId = collectionId;
                this.models = null;
                startOffset = 0;
            } else {
                //GWT.log("Collection unchanged.  old id = "+loadedCollectionId+" new id "+collectionId+" collection "+collection, null);
            }
	    }
        
	    if(modelsChanged) {
	        modelsChanged = false;
            loadModels(callback);
	    } else if(collection == null) {
	        refreshModels(callback);
	    } else {
	        load(startOffset, increment, callback);
	    }
	}
	
	private void refreshModels(AsyncCallback<Void> callback) {
	    AsyncCallbackGroup group = new AsyncCallbackGroup();
	    startLoadingModels(group);
	    for(int row=0; row < items.size(); row++) {
	        loadItem(row, group);
	    }
	    finishLoadingModels(group);
	    group.ready(callback);
    }

    protected void startLoadingModels(AsyncCallbackGroup group) {
	    
	}

    private void loadModels(final AsyncCallback<Void> callback) {
        final int modelCount = models==null?0:models.length;
        totalItems = modelCount;
        
        while(items.size() > modelCount) {
            removeItem(items.size()-1);
        }
        
        if(filter != null) {
            unfilteredItems = new ArrayList();
            itemIndexesAfterFiltering = new int[modelCount];
        }
        startOffset = 0;
        loadModelsCommand = new IncrementalCommand() {
            final AsyncCallbackGroup group = new AsyncCallbackGroup();
            int i = 0;
            int selectedIndex=-1;
            int row=0;
            
            @Override
            public boolean execute() {
                if(loadModelsCommand != this) { // Did another load come along and replace us?
                    callback.onSuccess(null);
                    return false;
                }
                if(i == 0) {
                    startLoadingModels(group);
                }
                
                for (int done=0; i < modelCount && done < 5; i++, done++) {
                    T model = models[i];
                    if(model == null) {
                        callback.onFailure(new NullPointerException("Model "+i+" of "+modelCount+" was null in "+models));
                        return false;
                    }
                    if(filter != null) {
                        unfilteredItems.add(model);
                        
                        if(!filter.includes(model)) {
                            itemIndexesAfterFiltering[i] = -1; 
                            continue;
                        } else {
                            itemIndexesAfterFiltering[i] = row; 
                        }
                    }
                    if(row == items.size()) {
                        addItem(row, model, group.member());
                    } else {
                        replaceItem(row, model, group.member());
                    }
                    if(selectedModel != null && selectedModel.equals(model)) {
                        selectedIndex = row;
                    }
                    loadItem(row, group);
                    row++;
                }
   
                if(i >= modelCount) {
                    // Clean up any trailing rows after filters were applied, 10 at a time
                    for(int done=0; items.size() > row && done < 5; done++) {
                        removeItem(items.size()-1);
                    }
                    if(items.size() > row)
                        return true;
      
                    if(selectedIndex >= 0) {
                        setSelectedIndex(selectedIndex);
                    } else {
                        selectedRow = -1;
                    }
      //              if(scrollAutoLoader != null) {
      //                  scrollAutoLoader.setEnd(models.length);
      //              }
                    loadModelsCommand = null;
                    
                    finishLoadingModels(group);
                    load(startOffset, increment, callback);
                    return false;
                } else {
                    return true;
                }
            }
        };
        if(loadModelsCommand.execute()) {
            DeferredCommand.addCommand(loadModelsCommand);
        }
    }
	
	private void showLoadedModels(T[] models, final int offset, final int limit, AsyncCallback callback) {
		try {
			if(selectedRow >= offset && selectedRow < (offset+limit)) {
				selectRow(-1);
			}
			AsyncCallbackGroup group = new AsyncCallbackGroup();
	        startLoadingModels(group);
			for (int i = 0; i < models.length; i++) {
				if(models[i] == null)
					throw new NullPointerException("Model "+i+" of "+models.length+" in "+models+" was null");
				int idx = offset+i-startOffset;
				if(idx == items.size()) {
					addItem(offset+i, models[i], group.member());
				} else if(models[i] != items.get(i)){
					replaceItem(offset+i, models[i], group.member());
				}
				loadItem(i, group);
			}
			
			// If we get less than we asked for, there's no more to get
			final boolean done = increment <= 0 || models.length<limit;
			int endOffset = offset + models.length;
			if(models.length > 0) // If we got models back then we can push totalItems ahead to include them
			    totalItems = Math.max(totalItems, endOffset);
			if(done) {
				for(int i=offset + items.size()-1; i >= endOffset; i--){
					removeItem(i);
				}
			}
			
			finishLoadingModels(group);
			group.ready(callback, new Boolean(!done));
		} catch (Throwable caught) {
			callback.onFailure(caught);
		}
	}

	public static AsyncCallback hideDuringUpdate(AsyncCallback callback, final Widget widget) {
		DOM.setStyleAttribute(widget.getElement(), "visibility", "hidden");
		callback = new AsyncCallbackProxy(callback) {
			@Override
			public void onSuccess(Object result) {
				DOM.setStyleAttribute(widget.getElement(), "visibility", "visible");
				super.onSuccess(result);
			}
			
			@Override
			public void onFailure(Throwable caught) {
				DOM.setStyleAttribute(widget.getElement(), "visibility", "visible");
				super.onFailure(caught);
			}
		};
		return callback;
	}
	
	public void setModels(final T[] models) {
		if(models == this.models) {
			return;
		}
		//GWT.log("New models: "+models+" old models "+this.models, null);
		this.models = models;
		modelsChanged = true;
	}

	public Object[] getModels() {
		if(unfilteredItems != null) return unfilteredItems.toArray();
		return items.toArray();
	}
	
	/**
	 * Show the given row as selected.  Typically this is done by
	 * calling addStyleDependentName(row, "selected") or
	 * removeStyleDependentName(row, "selected") based on the value
	 * of selected.
	 */
	protected abstract void showSelected(int row, boolean selected);
	
	protected abstract UIObject getRowUIObject(int row);
	
	/**
	 * Called by the subclass when a row is clicked
	 */
	protected void onRowClicked(int row) {
		if(selectRow(row) && selectable)
			sendChangeEvent();
		sendClickEvent(this);
	}

	/**
	 * Do what's needed to mark/show the given row as selected.
	 * 
	 * @param row The row that has been selected
	 * @return false if that row was already selected
	 */
	private boolean selectRow(int row) {
	    selectedModel = row == -1 || row >= items.size() ? null : items.get(row);
	    if(selectable) {
    		if(selectedRow != -1) {
    			if(selectedRow == row)
    				return false; // Clicked the same row
    			showSelected(selectedRow, false);
    		}
    		selectedRow = row;
    		if(selectedRow != -1)
    			showSelected(selectedRow, true);
	    }
		return true;
	}
	
	@Override
	public void clear() {
		unfilteredItems = null;
		items.clear();
//		if(scrollAutoLoader != null)
//			scrollAutoLoader.reset();
		selectedModel = null;
		startOffset = 0;
		clearRows();
	}
	
	public void clearFields() {
		if(selectable)
			selectRow(-1);
	}
	
	/**
	 * Remove all rows from the display.
	 */
	protected abstract void clearRows();
	
//	public void load(AsyncCallback callback) {
//		if(scrollAutoLoader != null) {
//			scrollAutoLoader.waitUntilReady(callback);
//		} else {
//			callback.onSuccess(null);
//		}
//	}
	
	public Widget getViewWidget() {
		return this;
	}
	
	public int getSelectedIndex() {
		return selectedRow + startOffset;
	}
	
	public void setSelectedIndex(int newIndex) {
		selectRow(newIndex-startOffset);
	}

	private void applyFilter(ModelFilter modelFilter, AsyncCallback callback) {
		ArrayList<T> allItems = unfilteredItems;
		if(allItems == null) {
			allItems = new ArrayList<T>();
			allItems.addAll(items);
		}
		unfilteredItems = null;
		if(itemIndexesAfterFiltering==null || itemIndexesAfterFiltering.length != allItems.size())
			itemIndexesAfterFiltering = new int[allItems.size()];
		// Start with a blank slate
		items.clear();
		clearRows();
		selectedRow=-1;
		
		AsyncCallbackGroup group = new AsyncCallbackGroup();
		int j=0;
		for(int i=0; i < allItems.size(); i++) {
			T item = allItems.get(i);
			if(modelFilter.includes(item)) {
				itemIndexesAfterFiltering[i] = j;
				addItem(j+startOffset, item, group.member());
				j++;
			} else {
				itemIndexesAfterFiltering[i] = -1;
			}
		}
		unfilteredItems = allItems;
		if(selectedModel != null) {
			setSelectedIndex(items.indexOf(selectedModel));
		}
		
		group.ready(callback);
	}

	/*
	public int getMaxHeight() {
		return maxHeight;
	}

	public void setMaxHeight(int maxHeight) {
		this.maxHeight = maxHeight;
		//setupScrollAutoLoader();
	}
	
	public void setHeight(String height) {
		super.setHeight(height);
		scrollAutoLoader.setHeight(height);
	}
	*/
	
	public void gotoNextPage() {
		startOffset += increment;
		// If we exceed the number of items, return to the last non-empty page of results
		if(startOffset >= totalItems && totalItems != -1)
			startOffset = totalItems - (totalItems % increment);
	}
	
	public void gotoPrevPage() {
		startOffset -= increment;
		if(startOffset < 0)
			startOffset = 0;
	}
	
	public void gotoFirstPage() {
	    startOffset = 0;
	}
	public void gotoLastPage() {
	    int lastPage = getMaxPageNumber();
	    if(lastPage > 0)
	        startOffset = (lastPage-1) * increment;
	}

	public boolean isFirstPage() {
		return startOffset == 0;
	}
	
	public boolean isLastPage() {
		return (totalItems != -1) && (startOffset + increment >= totalItems);
	}
	
	public int getPageNumber() {
	    return 1 + startOffset/increment;
	}
	
	public int getMaxPageNumber() {
	    if(totalItems == -1)
	        return -1;
	    else
	        return (totalItems+increment-1)/increment;
	}

	public ModelFilter getFilter() {
		return filter;
	}

	public void setFilter(ModelFilter<T> filter, AsyncCallback callback) {
		if(filter != this.filter) {
			this.filter = filter;
			applyFilter(filter, callback);
		}
	}

	public int[] getItemIndexesAfterFiltering() {
		return itemIndexesAfterFiltering;
	}

	public void setItemIndexesAfterFiltering(int[] itemIndexesAfterFiltering) {
		this.itemIndexesAfterFiltering = itemIndexesAfterFiltering;
	}

	/**
	 * Total number of items in the collection, if known.  -1 otherwise.
	 */
	public int getTotalItems() {
		return totalItems;
	}

	public void setTotalItems(int totalItems) {
		this.totalItems = totalItems;
	}

	public boolean isEmpty() {
		return getTotalItems() == 0;
	}
	public void addClickListener(ClickListener listener) {
		if(clickListeners == null) clickListeners = new ClickListenerCollection();
		clickListeners.add(listener);
	}

	public void sendClickEvent(Widget sender) {
		if(clickListeners == null) return;
		clickListeners.fireClick(sender);
	}

	public void removeClickListener(ClickListener listener) {
		if(clickListeners == null) return;
		clickListeners.remove(listener);
	}

	public HoverStyleHandler.Group getHoverGroup() {
		return hoverGroup;
	}

	public void setHoverGroup(HoverStyleHandler.Group hoverGroup) {
		this.hoverGroup = hoverGroup;
	}

    public int getStartOffset() {
        return startOffset;
    }

    public void setStartOffset(int startOffset) {
        this.startOffset = startOffset;
    }

    protected void finishLoadingModels(AsyncCallbackGroup group) {
    }

    protected void loadItem(int i, AsyncCallbackGroup group) {
    }


}