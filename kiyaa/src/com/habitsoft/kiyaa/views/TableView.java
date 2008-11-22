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
import com.google.gwt.user.client.ui.SourcesTableEvents;
import com.google.gwt.user.client.ui.TableListener;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import com.habitsoft.kiyaa.metamodel.Action;
import com.habitsoft.kiyaa.metamodel.Value;
import com.habitsoft.kiyaa.util.AsyncCallbackFactory;
import com.habitsoft.kiyaa.util.AsyncCallbackGroup;
import com.habitsoft.kiyaa.util.AsyncCallbackProxy;
import com.habitsoft.kiyaa.util.AsyncCallbackWithTimeout;
import com.habitsoft.kiyaa.util.HoverStyleHandler;
import com.habitsoft.kiyaa.widgets.HTMLTableRowPanel;

public class TableView<T> extends BaseCollectionView<T> implements SourcesTableEvents, TableListener, Focusable {

	FlexTable table = new FlexTable();
	ArrayList<Column> columns = new ArrayList();
	ArrayList<HTMLTableRowPanel> rowPanels = new ArrayList();
	Element headings;
	View navigation;
	View emptyContent;
	View footer;
	private Element borderMiddle;
    private T contextMenuTarget;
    CustomPopup contextMenu;
    
    private final ClickListener contextMenuListener = new ClickListener() {
        public void onClick(Widget sender) {
            onContextMenu(((HTMLTableRowPanel)sender).getRow());
        }
    };
	
	class Column {
		int position;
		ViewFactory viewFactory;
		String styleName;
		Value test;
		boolean visible;
		private ArrayList<ModelView> views; // One per row
		
		public Column(ViewFactory viewFactory, String styleName, Value test, boolean visible, int position) {
			super();
			this.position = position;
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
				DOM.setStyleAttribute(DOM.getChild(headings, position), "display", "none");
				for(HTMLTableRowPanel rowPanel : rowPanels) {
					Element cellElement = DOM.getChild(rowPanel.getElement(), position);
					DOM.setStyleAttribute(cellElement, "display", "none");
				}
				visible = false;
			}
		}
		public void show() {
			if(!visible) {
				DOM.setStyleAttribute(DOM.getChild(headings, position), "display", "");
				for(HTMLTableRowPanel rowPanel : rowPanels) {
					Element cellElement = DOM.getChild(rowPanel.getElement(), position);
					DOM.setStyleAttribute(cellElement, "display", "");
				}
				visible = true;
			}
		}
		public void load(AsyncCallback callback) {
			if(test == null) {
				loadViews(callback);
			} else {
				test.getValue(new AsyncCallbackProxy<Boolean>(callback) {
					@Override
					public void onSuccess(Boolean result) {
						if(result) {
							loadViews(callback);
							show();
						} else {
							hide();
							super.onSuccess(null);
						}
					}
				});
			}
		}
		private void loadViews(AsyncCallback callback) {
			AsyncCallbackGroup group = new AsyncCallbackGroup();
			for(ModelView view: views) {
				view.load(group.member());
			}
			group.ready(callback);
		}
		public void save(AsyncCallback callback) {
			if(visible) {
    			AsyncCallbackGroup group = new AsyncCallbackGroup();
    			for(ModelView view: views) {
    				view.save(group.member());
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
    			test.getValue(new AsyncCallbackProxy<Boolean>(group.member()) {
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
				view.load(group.member());
			}
		}
		public void save(int row, AsyncCallbackGroup group) {
			if(visible) {
				ModelView view = views.get(row);
				view.save(group.member());
			}
		}
		boolean isLast() {
		    return getPosition() == columns.size()-1;
		}
		
		void addItem(int row, Object model, AsyncCallback callback) {
			ModelView view = addView();			
			view.setModel(model, callback);
			table.setWidget(row, getPosition(), view.getViewWidget());
			// This is useless until we get the headings to have first and last ... in fact we only care about first and last on the headings!
			//if(getPosition() == 0) styleName = styleName==null?"first":styleName+" first";
			//if(isLast()) styleName = styleName==null?"last":styleName+" last";
			if(styleName != null)
				table.getCellFormatter().setStyleName(row, getPosition(), styleName);
			if(!visible) {
				Element cellElement = table.getCellFormatter().getElement(row, getPosition());
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
		columns.add(new Column(viewFactory, styleName, test, true, columns.size()));
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
		while(table.getRowCount() > 0) {
			table.removeRow(table.getRowCount()-1);
		}
		rowPanels.clear();
		for(Column column:columns) {
			column.getViews().clear();
		}
		checkEmpty(null);
		hoverGroup.clear();
	}
	
	@Override
	protected void showItem(int row, Object object, AsyncCallback callback) {
		HTMLTableRowPanel rowPanel = new HTMLTableRowPanel(table, row, "ui-table-row", selectable || clickable, hoverGroup);
		rowPanel.addContextMenuListener(contextMenuListener);
        rowPanels.add(row, rowPanel);

		AsyncCallbackGroup group = new AsyncCallbackGroup();
		for (Column column : columns) {
			column.addItem(row, object, group.member());
		}
				
		if(object == selectedModel) {
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
		group.ready(callback);
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
		for(Column column: columns) {
			column.getViews().remove(i);
		}
		rowPanels.remove(i);
		
		// Fix the row numbers for proceeding row panels
		for(int j=i; j < rowPanels.size(); j++) {
			HTMLTableRowPanel tableRowPanel = (HTMLTableRowPanel)rowPanels.get(j);
			tableRowPanel.setRow(j);
		}
		table.removeRow(i);
		
		if(rowPanels.size() == 0)
			checkEmpty(null);
	}
	@Override
	protected void setItem(int row, Object model, AsyncCallback callback) {
		callback = new AsyncCallbackWithTimeout(callback); // debug
		if(row == rowPanels.size()) {
			showItem(row, model, callback);
			return;
		}
		AsyncCallbackGroup group = new AsyncCallbackGroup();
		for(Column column: columns) {
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
		for(Column column:columns) {
			for (View view:column.getViews()) {
				view.clearFields();
			}
		}
	}

	public void save(AsyncCallback callback) {
		AsyncCallbackGroup group = new AsyncCallbackGroup();
		for(Column column:columns) {
			column.save(group.member());
		}
		if(navigation != null && navigation.getViewWidget().isVisible())
		    navigation.save(group.member());
		if(emptyContent != null && emptyContent.getViewWidget().isVisible())
		    emptyContent.save(group.member());
		group.ready(callback);
	}
	
	@Override
	public void load(AsyncCallback callback) {
		super.load(new AsyncCallbackProxy(callback) {
			@Override
			public void onSuccess(Object result) {
				AsyncCallbackGroup group = new AsyncCallbackGroup();
				
				final int rowCount = items.size();
				for(Column column:columns) {
					column.checkVisible(group);
				}
				for(int i=0; i < rowCount; i++) {
		    		for(Column column:columns) {
		    			column.load(i, group);
		    		}
				}
				/*
				for(Column column:columns) {
					column.load(group.member());
				}
				*/
				checkEmpty(group);
				if(footer != null)
					footer.load(group.member());
				group.ready(callback);
			}
		});
	}
	@Override
	protected UIObject getRowUIObject(int row) {
		return (UIObject)rowPanels.get(row);
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
		for(Column c : columns) {
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
	public void setTable(FlexTable table) {
		this.table = table;
	}
	public ArrayList<Column> getColumns() {
		return columns;
	}
	public void setColumns(ArrayList<Column> columns) {
		this.columns = columns;
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
		if(navigation != null) {
			if(!empty && group != null)
				navigation.load(group.member());
			navigation.getViewWidget().setVisible(!empty);
		}
		if(emptyContent != null) {
			emptyContent.getViewWidget().setVisible(empty);
			if(empty && group != null)
			    emptyContent.load(group.member());
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
}
