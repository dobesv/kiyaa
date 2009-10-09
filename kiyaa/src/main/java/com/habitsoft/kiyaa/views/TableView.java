package com.habitsoft.kiyaa.views;

import java.util.ArrayList;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasFocus;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SourcesTableEvents;
import com.google.gwt.user.client.ui.TableListener;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import com.habitsoft.kiyaa.metamodel.Action;
import com.habitsoft.kiyaa.metamodel.Value;
import com.habitsoft.kiyaa.util.AsyncCallbackDirectProxy;
import com.habitsoft.kiyaa.util.AsyncCallbackFactory;
import com.habitsoft.kiyaa.util.AsyncCallbackGroup;
import com.habitsoft.kiyaa.util.AsyncCallbackProxy;
import com.habitsoft.kiyaa.util.HoverStyleHandler;
import com.habitsoft.kiyaa.widgets.HTMLTableRowPanel;

public class TableView<T> extends BaseCollectionView<T> implements SourcesTableEvents, TableListener, Focusable {

	final FlexTable table = new FlexTable();
	ArrayList<Series> series = new ArrayList();
	ArrayList<HTMLTableRowPanel> rowPanels = new ArrayList();
	Element headings;
	View navigation;
    View bottomNavigation;
	View emptyContent;
	View footer;
	private Element borderMiddle;
    private T contextMenuTarget;
    CustomPopup contextMenu;
    RowStyleHandler<T> rowStyleHandler;
    boolean horizontal;
    
    private final ClickListener contextMenuListener = new ClickListener() {
        public void onClick(Widget sender) {
            onContextMenu(((HTMLTableRowPanel)sender).getRow());
        }
    };
	
    public interface RowStyleHandler<T> {
        public String getStyle(int row, T model);
    }
    
    /**
     * Represents either a row or a column, depending on whether
     * horizontal == true.
     */
	class Series {
		int position;
		ViewFactory viewFactory;
		String styleName;
		Value test;
		boolean visible;
		private ArrayList<ModelView> views; // One per row
		
		public Series(ViewFactory viewFactory, String styleName, Value test, boolean visible, int position) {
			super();
			this.position = position;
			if(viewFactory == null) throw new NullPointerException("viewFactory");
			this.viewFactory = viewFactory;
			this.styleName = styleName;
			this.test = test;
			this.visible = visible;
			this.views = new ArrayList<ModelView>();
		}
		public ViewFactory getViewFactory() {
			return viewFactory;
		}
		public void setViewFactory(ViewFactory viewFactory) {
			this.viewFactory = viewFactory;
		}
		public String getStyleName() {
			return styleName;
		}
		public void setStyleName(String styleClass) {
			this.styleName = styleClass;
		}
		public Value getTest() {
			return test;
		}
		public void setTest(Value test) {
			this.test = test;
		}
		public boolean isVisible() {
			return visible;
		}
		public void setVisible(boolean visible) {
			this.visible = visible;
		}
		public int getPosition() {
			return position;
		}
		public void setPosition(int position) {
			this.position = position;
		}
		public void hide() {
			if(visible) {
			    if(horizontal) {
			        HTMLTableRowPanel panel = rowPanels.get(position);
			        panel.setVisible(false);
			    } else {
                    DOM.setStyleAttribute(DOM.getChild(headings, position), "display", "none");
                    for(HTMLTableRowPanel rowPanel : rowPanels) {
                        Element cellElement = DOM.getChild(rowPanel.getElement(), position);
                        DOM.setStyleAttribute(cellElement, "display", "none");
                    }
			    }
				visible = false;
			}
		}
		public void show() {
			if(!visible) {
			    if(horizontal) {
                    HTMLTableRowPanel panel = rowPanels.get(position);
                    panel.setVisible(true);
			    } else {
    				DOM.setStyleAttribute(DOM.getChild(headings, position), "display", "");
    				for(HTMLTableRowPanel rowPanel : rowPanels) {
    					Element cellElement = DOM.getChild(rowPanel.getElement(), position);
    					DOM.setStyleAttribute(cellElement, "display", "");
    				}
			    }
				visible = true;
			}
		}
		public void load(AsyncCallback<Void> callback) {
			if(test == null) {
				loadViews(callback);
			} else {
				test.getValue(new AsyncCallbackProxy<Boolean,Void>(callback) {
					@Override
					public void onSuccess(Boolean result) {
						if(result) {
							loadViews(takeCallback());
							show();
						} else {
							hide();
							returnSuccess(null);
						}
					}
				});
			}
		}
		private void loadViews(AsyncCallback callback) {
			AsyncCallbackGroup group = new AsyncCallbackGroup();
			for(ModelView view: views) {
				view.load(group.<Void>member());
			}
			group.ready(callback);
		}
		public void save(AsyncCallback callback) {
			if(visible) {
    			AsyncCallbackGroup group = new AsyncCallbackGroup();
    			for(ModelView view: views) {
    				view.save(group.<Void>member());
    			}
    			group.ready(callback);
			} else {
				callback.onSuccess(null);
			}
		}
		void setViews(ArrayList<ModelView> views) {
			this.views = views;
		}
		ArrayList<ModelView> getViews() {
			return views;
		}
		public ModelView addView() {
			ModelView view = (ModelView)viewFactory.createView();
			views.add(view);
			return view;
		}
		
		public void checkVisible(AsyncCallbackGroup group) {
			if(test != null) {
    			test.getValue(new AsyncCallbackDirectProxy<Boolean>(group.<Boolean>member()) {
    				@Override
    				public void onSuccess(Boolean result) {
    					if(result) {
    						show();
    					} else {
    						hide();
    					}
    					super.onSuccess(null);
    				}
    			});
			}
		}
		public void load(int row, AsyncCallbackGroup group) {
			if(visible) {
				ModelView view = views.get(row);
				view.load(group.<Void>member());
			}
		}
		public void save(int row, AsyncCallbackGroup group) {
			if(visible) {
				ModelView view = views.get(row);
				view.save(group.<Void>member());
			}
		}
		boolean isLast() {
		    return getPosition() == series.size()-1;
		}
		
		void addItem(int index, Object model, AsyncCallback callback) {
			ModelView view = addView();			
			view.setModel(model, callback);
			int row = horizontal?position:index;
			int col = horizontal?index+1:position;
			table.setWidget(row, col, view.getViewWidget());
			// This is useless until we get the headings to have first and last ... in fact we only care about first and last on the headings!
			//if(getPosition() == 0) styleName = styleName==null?"first":styleName+" first";
			//if(isLast()) styleName = styleName==null?"last":styleName+" last";
			if(styleName != null)
				table.getCellFormatter().setStyleName(row, col, styleName);
			if(!visible) {
				Element cellElement = table.getCellFormatter().getElement(row, col);
				DOM.setStyleAttribute(cellElement, "display", "none");
			}
		}
		
	}
	public void addTableListener(TableListener listener) {
		table.addTableListener(listener);
	}
	public void removeTableListener(TableListener listener) {
		table.removeTableListener(listener);
	}
	public TableView() {
		Element borderTop = DOM.createDiv();
		setStyleName(borderTop, "table-border-top");
		DOM.appendChild(getElement(), borderTop);
		borderMiddle = DOM.createDiv();
		setStyleName(borderMiddle, "table-border-middle");
		DOM.appendChild(getElement(), borderMiddle);
		Element borderBottom = DOM.createDiv();
		setStyleName(borderBottom, "table-border-bottom");
		DOM.appendChild(getElement(), borderBottom);
		
		add(table, borderMiddle);
		Element thead = DOM.createTHead();
		DOM.insertChild(table.getElement(), thead, 0);
		headings = DOM.createTR();
		DOM.appendChild(thead, headings);
		setStylePrimaryName("ui-table");
		table.setStylePrimaryName("ui-table");
		table.addTableListener(this);
		setCellSpacing(0);
		setCellPadding(0);
	}
	
	@Override
	protected void onAttach() {
	    // Start with a fresh hover state, if we'd been hovering before
	    hoverGroup.clear();
	    for (HTMLTableRowPanel row : rowPanels) {
            row.onAttach();
        }
	    super.onAttach();
	}
	public void setBorderWidth(int width) {
		table.setBorderWidth(width);
	}

	public void setCellPadding(int padding) {
		table.setCellPadding(padding);
	}

	public void setCellSpacing(int spacing) {
		table.setCellSpacing(spacing);
	}

	public void addColumn(ViewFactory viewFactory, String heading, String styleName) {
		addColumn(viewFactory, heading, styleName, null);
	}
	public void addColumn(ViewFactory viewFactory, String heading, Value test) {
		addColumn(viewFactory, heading, null, test);
	}
	public void addColumn(ViewFactory viewFactory, String heading, String styleName, Value test) {
		series.add(new Series(viewFactory, styleName, test, true, series.size()));
		if(heading != null)
			addHeadingText(heading, styleName);
	}
	public void addColumn(ViewFactory viewFactory, String heading) {
		addColumn(viewFactory);
		addHeadingText(heading, null);
	}
	public void addColumn(ViewFactory viewFactory) {
		addColumn(viewFactory, null, null, null);
	}
	public int getColumnCount() {
		if(table.getRowCount() == 0) return 0;
		return table.getCellCount(0);
	}
    public void addRow(ViewFactory viewFactory, String label) {
        addRow(viewFactory, null, label, null);
    }
    public void addRow(ViewFactory viewFactory, String styleName, String label) {
        addRow(viewFactory, styleName, label, null);
    }
    public void addRow(ViewFactory viewFactory, Value test) {
        addRow(viewFactory, null, test);
    }
    public void addRow(ViewFactory viewFactory, String styleName, Value test) {
        addRow(viewFactory, styleName, null, test);
    }
    public void addRow(ViewFactory viewFactory, String styleName, String label, Value test) {
        horizontal=true;
        final int row = series.size();
        final HTMLTableRowPanel rowPanel = addRowPanel(row, null, styleName==null?"ui-table-row":"ui-table-row "+styleName);
        //if(styleName != null) rowPanel.addStyleName(styleName);
        if(label != null) {
            final Label labelWidget = new Label(label);
            labelWidget.setStyleName("ui-row-label");
            rowPanel.add(labelWidget);
        }
        series.add(new Series(viewFactory, styleName, test, true, row));
    }
    public void addRow(ViewFactory viewFactory) {
        addRow(viewFactory, null, null, null);
    }
    
	/*
	public void addHeading(Widget widget) {
		int col = getColumnCount();
		table.setWidget(0, col, widget);
		String styleClass = (String) columnStyleClasses.get(col);
		if(styleClass != null)
			table.getCellFormatter().setStylePrimaryName(0, col, styleClass);
		else
			table.getCellFormatter().setStylePrimaryName(0, col, "ui-table-heading");
	}
	*/
	public void addHeadingText(String text) {
		addHeadingText(text, null);
	}
	/*
	public void addHeading(Widget widget) {
		int col = getColumnCount();
		table.setWidget(0, col, widget);
		String styleClass = (String) columnStyleClasses.get(col);
		if(styleClass != null)
			table.getCellFormatter().setStylePrimaryName(0, col, styleClass);
		else
			table.getCellFormatter().setStylePrimaryName(0, col, "ui-table-heading");
	}
	*/
	public void addHeadingText(String text, String styleName) {
		Element th = DOM.createTH();
		DOM.setInnerText(th, text);
		if(styleName != null)
			setStyleName(th, styleName);
		DOM.appendChild(headings, th);
	}
	public void addHeadingHtml(String html) {
		addHeadingHtml(html, null);
	}
	public void addHeadingHtml(String html, String styleName) {
		Element th = DOM.createTH();
		DOM.setInnerHTML(th, html);
		if(styleName != null)
			setStyleName(th, styleName);
		DOM.appendChild(headings, th);
	}
	@Override
	public void clearRows() {
	    if(horizontal) {
	        for(HTMLTableRowPanel rowPanel : rowPanels) {
	            int count = DOM.getChildCount(rowPanel.getElement());
                table.removeCells(rowPanel.getRow(), 1, count-1);
	        }
	    } else {
    		while(table.getRowCount() > 0) {
    			table.removeRow(table.getRowCount()-1);
    		}
    		rowPanels.clear();
        }
		for(Series column:series) {
			column.getViews().clear();
		}
		checkEmpty(null);
		hoverGroup.clear();
	}
	
	@Override
	protected void showItem(int row, T model, AsyncCallback callback) {
        AsyncCallbackGroup group = new AsyncCallbackGroup();
	    if(horizontal) {
            for (Series column : series) {
                column.addItem(row, model, group.member("Table col "+row+" row "+column.position));
            }
	    } else {
    		HTMLTableRowPanel rowPanel = addRowPanel(row, model, "ui-table-row");
    
    		for (Series column : series) {
    			column.addItem(row, model, group.member("Table col "+column.position+" row "+row));
    		}
    				
    		if(model == selectedModel) {
    			if(selectedRow != -1) {
    				showSelected(selectedRow, false);
    			}
    			selectedRow = row;
    			showSelected(selectedRow, true);
    		}
    		if(rowPanels.size() == 1)
    			checkEmpty(group);
    		if(isAttached())
    		    rowPanel.onAttach();
	    }
		group.ready(callback);
	}
    private HTMLTableRowPanel addRowPanel(int row, T model, String styleName) {
        HTMLTableRowPanel rowPanel = new HTMLTableRowPanel(table, row, styleName, selectable || clickable, hoverGroup);
        rowPanel.addContextMenuListener(contextMenuListener);
        rowPanels.add(row, rowPanel);
        maybeAssignStyle(row, rowPanel, model);
        // Passing clickable || selectable to HTMLTableRowPanel adds a hoverstylehandler for us ...
//        if(clickable || selectable)
//            rowPanel.addMouseListener(new HoverStyleHandler(rowPanel, hoverGroup));
        return rowPanel;
    }
    private void maybeAssignStyle(int row, HTMLTableRowPanel rowPanel, T model) {
        if(rowStyleHandler != null) {
		    String style = rowStyleHandler.getStyle(row, model);
		    if(style != null)
		        rowPanel.setStylePrimaryName(style);
		}
    }
	
	protected void onContextMenu(int row) {
	    if(contextMenu != null) {
    	    Event event = Event.getCurrentEvent();
            event.cancelBubble(true);
            event.preventDefault();
            contextMenuTarget = items.get(row);
            contextMenu.load(AsyncCallbackFactory.defaultNewInstance());
            contextMenu.showPopup(null);
	    }
    }
	
    public T getContextMenuTarget() {
        return contextMenuTarget;
    }
    public void setContextMenuTarget(T contextMenuTarget) {
        this.contextMenuTarget = contextMenuTarget;
    }
    @Override
	protected void hideItem(int i) {
		for(Series column: series) {
			column.getViews().remove(i);
		}
		if(horizontal) {
		    for(HTMLTableRowPanel rowPanel : rowPanels) {
		        final Element rowElement = rowPanel.getElement();
		        if(DOM.getChildCount(rowElement) > i+1)
		            rowElement.removeChild(DOM.getChild(rowElement, i+1));
		    }
		} else {
    		rowPanels.remove(i);
    		
    		// Fix the row numbers for proceeding row panels
    		for(int j=i; j < rowPanels.size(); j++) {
    			HTMLTableRowPanel tableRowPanel = (HTMLTableRowPanel)rowPanels.get(j);
    			tableRowPanel.setRow(j);
    			// TODO Should we assign the style?  Need to know the model object for these rows
//    	        if(rowStyleHandler != null) {
//    	            maybeAssignStyle(j, tableRowPanel,);
//    	        }
    		}
    		table.removeRow(i);
		}
		if(rowPanels.size() == 0)
			checkEmpty(null);
	}
	@Override
	protected void setItem(int row, T model, AsyncCallback callback) {
		//callback = new AsyncCallbackWithTimeout(callback); // debug
		if(row == rowPanels.size()) {
			showItem(row, model, callback);
			return;
		}
		if(rowStyleHandler != null) {
		    maybeAssignStyle(row, rowPanels.get(row), model);
		}
		AsyncCallbackGroup group = new AsyncCallbackGroup();
		for(Series column: series) {
			ArrayList<ModelView> views = column.getViews();
			ModelView view;
			try {
				view = views.get(row);
			} catch (ArrayIndexOutOfBoundsException	e) {
				callback.onFailure(new ArrayIndexOutOfBoundsException("Tried to give model "+model+" for row "+row+" on table "+this+" but column "+column.getPosition()+" doesn't have that many rows; it has "+column.getViews().size()));
				return;
			}
			view.setModel(model, group.member());
		}
		group.ready(callback);
	}
	
	public void onCellClicked(SourcesTableEvents sender, int row, int cell) {
		if((selectable || clickable) && row >= 0) {
			onRowClicked(row);
		}
	}
	
	@Override
	public void setSelectable(boolean selectable) {
	    // TODO This code assumes that selectable is never set to true, then false, then true
		if((this.clickable || selectable) != (this.selectable || this.clickable) && selectable) {
			attachHoverStyleHandlersToRows();
		}
		super.setSelectable(selectable);
	}
    private void attachHoverStyleHandlersToRows() {
        for (HTMLTableRowPanel tableRowPanel : rowPanels) {
        	tableRowPanel.addMouseListener(new HoverStyleHandler(tableRowPanel, hoverGroup));
        }
    }

    @Override
    public void setClickable(boolean clickable) {
        // TODO This code assumes that clickable is never set to true, then false, then true
        if(((clickable || this.selectable) != (this.clickable || this.selectable)) && clickable) {
            attachHoverStyleHandlersToRows();
        }
        super.setClickable(clickable);
    }
	
	@Override
	protected void showSelected(int row, boolean selected) {
		if(row >= table.getRowCount() || row < 0)
			return;
		if(selected)
			rowPanels.get(row).addStyleDependentName("selected");
		else
			rowPanels.get(row).removeStyleDependentName("selected");
	}
	@Override
	public void clearFields() {
		super.clearFields();
		for(Series column:series) {
			for (View view:column.getViews()) {
				view.clearFields();
			}
		}
	}

	public void save(AsyncCallback callback) {
		AsyncCallbackGroup group = new AsyncCallbackGroup();
		for(Series column:series) {
			column.save(group.member());
		}
		if(navigation != null && navigation.getViewWidget().isVisible())
		    navigation.save(group.<Void>member());
        if(bottomNavigation != null && bottomNavigation.getViewWidget().isVisible())
            bottomNavigation.save(group.<Void>member());
		if(emptyContent != null && emptyContent.getViewWidget().isVisible())
		    emptyContent.save(group.<Void>member());
		group.ready(callback);
	}

	@Override
	protected UIObject getRowUIObject(int row) {
		try {
		    return (UIObject)rowPanels.get(row);
		} catch(IndexOutOfBoundsException e) {
		    return null;
		}
	}
	@Override
	protected Element getScrollElement() {
		Element e = table.getElement();
		for(Element child = DOM.getFirstChild(e); child != null; child = DOM.getNextSibling(child)) {
			if("TBODY".equalsIgnoreCase(DOM.getElementProperty(child, "nodeName"))) {
				return child;
			}
		}
		return table.getElement();
	}

	public void focus() {
		if(getRowCount() == 0)
			return;
		for(Series c : series) {
			ModelView firstView = c.getViews().get(0);
			if(firstView instanceof Focusable) {
				((Focusable)firstView).focus();
				return;
			}
			if(firstView.getViewWidget() instanceof HasFocus) {
				((HasFocus)firstView.getViewWidget()).setFocus(true);
				return;
			}
		}
	}
	@Override
	protected Widget getWidget() {
		return table;
	}
	public FlexTable getTable() {
		return table;
	}
	public ArrayList<Series> getSeries() {
		return series;
	}
	public void setSeries(ArrayList<Series> columns) {
		this.series = columns;
	}
	public ArrayList<HTMLTableRowPanel> getRowPanels() {
		return rowPanels;
	}
	public void setRowPanels(ArrayList<HTMLTableRowPanel> rowPanels) {
		this.rowPanels = rowPanels;
	}
	public Element getHeadings() {
		return headings;
	}
	public void setHeadings(Element headings) {
		this.headings = headings;
	}
	public View getNavigation() {
		return navigation;
	}
	public void setNavigation(View navigation) {
		if(this.navigation != null) {
			if(this.navigation == navigation)
				return;
			this.navigation.getViewWidget().removeFromParent();
		}
		this.navigation = navigation;
		insert(navigation.getViewWidget(), 0);
	}
    public View getbottomNavigation() {
        return bottomNavigation;
    }
    public void setbottomNavigation(View bottomNavigation) {
        if(this.bottomNavigation != null) {
            if(this.bottomNavigation == bottomNavigation)
                return;
            this.bottomNavigation.getViewWidget().removeFromParent();
        }
        this.bottomNavigation = bottomNavigation;
        add(bottomNavigation.getViewWidget());
    }
	
	/**
	 * The footer is a view that appears inside the border, below the table
	 * data.  It's a good place to put buttons, perhaps.
	 */
	public View getFooter() {
		return footer;
	}
	public void setFooter(View footer) {
		if(this.footer != null) {
			if(this.footer == footer)
				return;
			this.footer.getViewWidget().removeFromParent();
		}
		this.footer = footer;
		add(footer.getViewWidget(), borderMiddle);
	}
	
	public View getEmptyContent() {
		return emptyContent;
	}
	public void setEmptyContent(View emptyContent) {
		if(this.emptyContent != null) {
			if(this.emptyContent == emptyContent)
				return;
			this.emptyContent.getViewWidget().removeFromParent();
		}
		this.emptyContent = emptyContent;
		if(emptyContent != null) {
			emptyContent.getViewWidget().addStyleName("empty-table-content");
			add(emptyContent.getViewWidget(), borderMiddle);
		}
	}
	private void checkEmpty(AsyncCallbackGroup group) {
		boolean empty = items.isEmpty() && startOffset == 0;
		//table.setVisible(!empty);
		if(navigation != null &&  group != null)
			navigation.load(group.<Void>member());
		if(bottomNavigation != null && group != null)
		    bottomNavigation.load(group.<Void>member());		    
		if(emptyContent != null) {
			emptyContent.getViewWidget().setVisible(empty);
			if(empty && group != null)
			    emptyContent.load(group.<Void>member());
		}
		if(empty) addStyleName("empty");
		else removeStyleName("empty");
	}
	
	/**
	 * TableView wraps the table in a DIV; to set the CSS class of the table,
	 * use setTableClass(); setStyleName() will only affect the DIV wrapper.
	 */
	public void setTableClass(String styleName) {
		table.setStyleName(styleName);
	}
	
	
    private void createContextMenu() {
        if(contextMenu == null) {
            contextMenu = new CustomPopup();
            contextMenu.setSelectable(false); // not selectable by default
        }
    }
    public Anchor addContextMenuAction(String label, Action action, Value test, boolean hideOnClick) {
        createContextMenu();
        return contextMenu.addAction(label, action, test, hideOnClick);
    }
    public void addContextMenuAction(String label, Action action, Value test) {
        createContextMenu();
        contextMenu.addAction(label, action, test);
    }
    public void addContextMenuAction(String label, Action action) {
        createContextMenu();
        contextMenu.addAction(label, action);
    }
    public void setContextMenuModels(Value models) {
        createContextMenu();
        contextMenu.setModels(models);
    }
    public void addContextMenuColumn(ViewFactory viewFactory) {
        createContextMenu();
        contextMenu.addColumn(viewFactory);
    }
	public void setContextMenuSelectable(boolean selectable) {
        createContextMenu();
        contextMenu.setSelectable(selectable);
	}
    public RowStyleHandler<T> getRowStyleHandler() {
        return rowStyleHandler;
    }
    public void setRowStyleHandler(RowStyleHandler<T> rowStyleHandler) {
        if(rowStyleHandler != this.rowStyleHandler) {
            this.rowStyleHandler = rowStyleHandler;
            for(HTMLTableRowPanel rowPanel : rowPanels) {
                maybeAssignStyle(rowPanel.getRow(), rowPanel, null);
            }
        }
    }
    public boolean isHorizontal() {
        return horizontal;
    }
    public void setHorizontal(boolean horizontal) {
        this.horizontal = horizontal;
    }
    @Override
    protected void startLoadingModels(AsyncCallbackGroup group) {
        for(Series column:series) {
            column.checkVisible(group);
        }
    }
    @Override
    protected void loadItem(int i, AsyncCallbackGroup group) {
        for(Series column:series) {
            column.load(i, group);
        }
    }
    @Override
    protected void finishLoadingModels(AsyncCallbackGroup group) {
        checkEmpty(group);
        if(footer != null)
            footer.load(group.<Void>member());
    }
}
