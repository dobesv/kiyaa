package com.habitsoft.kiyaa.widgets;

import java.util.Iterator;

import com.google.gwt.user.client.ui.HTMLTable;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

public class HTMLTableColumnPanel extends Panel {

	protected final HTMLTable table;
	protected final int column;
	protected int row;
	
	public HTMLTableColumnPanel(HTMLTable table, int column) {
		this.table = table;
		this.column = column;
		this.row = 0;
	}

	@Override
	public void add(Widget widget) {
		table.setWidget(row, column, widget);
		row++;
	}
	
	public Iterator iterator() {
		return new Iterator() {
			int currentRow=0;
			Widget lastResult;
			public void remove() {
				if(lastResult != null)
					table.remove(lastResult);
			}
		
			public Object next() {
				lastResult = table.getWidget(currentRow, column);
				currentRow++;
				return lastResult;
			}
		
			public boolean hasNext() {
				return currentRow < row;
			}
		
		};
	}

	@Override
	public boolean remove(Widget w) {
		return table.remove(w);
	}

	@Override
	public void clear() {
		for (Iterator i = this.iterator(); i.hasNext();) {
			Widget w = (Widget) i.next();
			w.removeFromParent();
		}
		row = 0;
	}
}
