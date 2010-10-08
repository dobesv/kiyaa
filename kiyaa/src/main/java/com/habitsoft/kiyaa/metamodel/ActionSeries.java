package com.habitsoft.kiyaa.metamodel;

import java.util.ArrayList;
import java.util.Collection;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.habitsoft.kiyaa.util.AsyncCallbackDirectProxy;

/**
 * This Action subclass runs a series of other actions.  If any of them fails,
 * it returns failure without running the ones that follow.
 */
public class ActionSeries extends Action {

	public ArrayList<Action> actions = new ArrayList<Action>();
	
	public ActionSeries() {
	}

	public ActionSeries(Action... actions) {
	    for(Action a : actions) {
	        this.actions.add(a);
	    }
	}
	
	public void add(Action action) {
		if(action == null) throw new NullPointerException("Don't add null actions to an ActionSeries!");
		actions.add(action);
	}
	
	/**
	 * Class to run each action in order.  If any action fails, returns
	 * failure to the original callback.
	 * 
	 * Note that if actions are added to the list while this is running,
	 * it WILL pick them up and also run them.
	 */
	class Executor extends AsyncCallbackDirectProxy<Void> {
		int pos=0;
		
		public Executor(AsyncCallback<Void> delegate) {
			super(delegate);
		}

		@Override
		public void onSuccess(Void result) {
			performNext();
		}
		
		public void performNext() {
			if(pos < actions.size()) {
				Action nextAction = actions.get(pos);
				pos++;
				nextAction.performDeferred(this);
			} else {
				returnSuccess(null);
			}
		}
		
	}
	@Override
	public void perform(AsyncCallback<Void> callback) {
		if(actions.size() == 0) {
			callback.onSuccess(null);
			return;
		}
		new Executor(callback).performNext();
	}

	public boolean addAll(Collection<? extends Action> c) {
		return actions.addAll(c);
	}

	public void clear() {
		actions.clear();
	}

	public boolean contains(Object o) {
		return actions.contains(o);
	}

	public Action get(int index) {
		return actions.get(index);
	}

	public boolean isEmpty() {
		return actions.isEmpty();
	}

	public int size() {
		return actions.size();
	}
	

}
