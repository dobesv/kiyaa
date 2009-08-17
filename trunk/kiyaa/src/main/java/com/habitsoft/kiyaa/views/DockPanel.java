package com.habitsoft.kiyaa.views;

import java.util.ArrayList;
import java.util.Iterator;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.habitsoft.kiyaa.util.AsyncCallbackGroup;

public class DockPanel extends com.google.gwt.user.client.ui.DockPanel implements View {

	ArrayList views = new ArrayList();
	
	public void addNorth(View view) {
		views.add(view);
		add(view.getViewWidget(), NORTH);
	}
	public void addWest(View view) {
		views.add(view);
		add(view.getViewWidget(), WEST);
	}
	public void addEast(View view) {
		views.add(view);
		add(view.getViewWidget(), EAST);
	}
	public void addSouth(View view) {
		views.add(view);
		add(view.getViewWidget(), SOUTH);
	}
	public void addCenter(View view) {
		views.add(view);
		add(view.getViewWidget(), CENTER);
	}
	public void clearFields() {
		for (Iterator i = views.iterator(); i.hasNext();) {
			View view = (View) i.next();
			view.clearFields();
		}
	}
	public Widget getViewWidget() {
		return this;
	}
	public void load(AsyncCallback callback) {
		AsyncCallbackGroup group = new AsyncCallbackGroup();
		for (Iterator i = views.iterator(); i.hasNext();) {
			View view = (View) i.next();
			view.load(group.member());
		}
		group.ready(callback);
	}
	public void save(AsyncCallback callback) {
		AsyncCallbackGroup group = new AsyncCallbackGroup();
		for (Iterator i = views.iterator(); i.hasNext();) {
			View view = (View) i.next();
			view.save(group.member());
		}
		group.ready(callback);
	}
	
}
